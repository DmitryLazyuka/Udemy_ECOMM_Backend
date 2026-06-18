package org.example.udemyproject.service;

import org.example.udemyproject.model.AppRole;
import org.example.udemyproject.model.Role;
import org.example.udemyproject.model.User;
import org.example.udemyproject.payload.AuthenticationResult;
import org.example.udemyproject.payload.UserDTO;
import org.example.udemyproject.payload.UserResponse;
import org.example.udemyproject.repository.RoleRepository;
import org.example.udemyproject.repository.UserRepository;
import org.example.udemyproject.security.JwtUtils;
import org.example.udemyproject.security.request.LoginRequest;
import org.example.udemyproject.security.request.SignupRequest;
import org.example.udemyproject.security.response.MessageResponse;
import org.example.udemyproject.security.response.UserInfoResponse;
import org.example.udemyproject.security.services.UserDetailsImpl;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final String ROLE_NOT_FOUND_MESSAGE = "Role is not found";

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtUtils jwtUtils,
                           UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           ModelMapper modelMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    @Override
    public AuthenticationResult login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        UserInfoResponse response =  new UserInfoResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                jwtUtils.generateTokenFromUsername(userDetails.getUsername()),
                roles);

        return new AuthenticationResult(response, jwtCookie);
    }

    @Override
    public ResponseEntity<MessageResponse> register(SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Username is already in use"));
        }
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Email is already taken"));
        }
        User user = new User(
                signupRequest.getUsername(),
                passwordEncoder.encode(signupRequest.getPassword()),
                signupRequest.getEmail());

        Set<String> strRoles = signupRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            roles.add(getRole(AppRole.ROLE_USER));
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin" -> roles.add(getRole(AppRole.ROLE_ADMIN));
                    case "seller" -> roles.add(getRole(AppRole.ROLE_SELLER));
                    default -> roles.add(getRole(AppRole.ROLE_USER));
                }
            });
        }
        user.setRoles(roles);
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully"));
    }

    @Override
    public UserInfoResponse getCurrentUserDetails(Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return new UserInfoResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                jwtUtils.generateTokenFromUsername(userDetails.getUsername()),
                roles);
    }

    @Override
    public ResponseCookie logoutUser() {
        return jwtUtils.getCleanJwtCookie();
    }

    @Override
    public UserResponse getAllSellers(Pageable pageDetails) {
        Page<User> allUsers = userRepository.findByRoleName(AppRole.ROLE_SELLER, pageDetails);
        List<UserDTO> userDTOs = allUsers.getContent()
                .stream()
                .map(p -> modelMapper.map(p, UserDTO.class))
                .toList();
        UserResponse response = new UserResponse();
        response.setContent(userDTOs);
        response.setPageNumber(allUsers.getNumber());
        response.setPageSize(allUsers.getSize());
        response.setTotalElements(allUsers.getTotalElements());
        response.setTotalPages(allUsers.getTotalPages());
        response.setLastPage(allUsers.isLast());

        return response;
    }

    private Role getRole(AppRole roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RuntimeException(ROLE_NOT_FOUND_MESSAGE));
    }
}
