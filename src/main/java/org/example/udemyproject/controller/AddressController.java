package org.example.udemyproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.udemyproject.model.User;
import org.example.udemyproject.payload.APIResponse;
import org.example.udemyproject.payload.AddressDTO;
import org.example.udemyproject.service.AddressService;
import org.example.udemyproject.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Addresses", description = "Address management endpoints")
@SecurityRequirement(name = "bearer")
public class AddressController {

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private AddressService addressService;

    @PostMapping("/addresses")
    @Operation(summary = "Create address", description = "Creates a new address for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address created successfully",
                    content = @Content(schema = @Schema(implementation = AddressDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid address payload",
                    content = @Content(schema = @Schema(implementation = APIResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<AddressDTO> createAddress(@Valid @RequestBody AddressDTO addressDTO) {
        User user = authUtil.loggedInUser();
        AddressDTO createdAddressDTO = addressService.createAddress(addressDTO, user);
        return new ResponseEntity<>(createdAddressDTO, HttpStatus.CREATED);
    }

    @GetMapping("/addresses")
    @Operation(summary = "List addresses", description = "Returns all available addresses.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses returned successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AddressDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<List<AddressDTO>> getAddresses(){
        List<AddressDTO> addressDTOS = addressService.getAddresses();
        return new ResponseEntity<>(addressDTOS, HttpStatus.OK);
    }

    @GetMapping("/addresses/{addressId}")
    @Operation(summary = "Get address by ID", description = "Returns a single address by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address returned successfully",
                    content = @Content(schema = @Schema(implementation = AddressDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Address not found",
                    content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<AddressDTO> getAddressById(@PathVariable Long addressId){
        AddressDTO addressDTO = addressService.getAddressById(addressId);
        return new ResponseEntity<>(addressDTO, HttpStatus.OK);
    }

    @GetMapping("/users/addresses")
    @Operation(summary = "List current user addresses", description = "Returns the addresses that belong to the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User addresses returned successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AddressDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<List<AddressDTO>> getUserAddresses(){
        User user = authUtil.loggedInUser();
        List<AddressDTO> addressDTOS = addressService.getUserAddresses(user);
        return new ResponseEntity<>(addressDTOS, HttpStatus.OK);
    }

    @PutMapping("/addresses/{addressId}")
    @Operation(summary = "Update address", description = "Updates an existing address by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address updated successfully",
                    content = @Content(schema = @Schema(implementation = AddressDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid address payload",
                    content = @Content(schema = @Schema(implementation = APIResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Address not found",
                    content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<AddressDTO> updateAddress(@Valid @RequestBody AddressDTO addressDTO,
                                                    @PathVariable Long addressId){

        AddressDTO updatedAddress = addressService.updateAddress(addressId, addressDTO);
        return new ResponseEntity<>(updatedAddress, HttpStatus.OK);
    }

    @DeleteMapping("/addresses/{addressId}")
    @Operation(summary = "Delete address", description = "Deletes an address by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Address not found",
                    content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<String> deleteAddress(@PathVariable Long addressId){
        String status = addressService.deleteAddress(addressId);
        return new ResponseEntity<>(status , HttpStatus.OK);
    }
}
