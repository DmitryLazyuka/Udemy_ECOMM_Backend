package org.example.udemyproject.service;

import org.example.udemyproject.exceptions.ResourceNotFoundException;
import org.example.udemyproject.model.Address;
import org.example.udemyproject.model.User;
import org.example.udemyproject.payload.AddressDTO;
import org.example.udemyproject.repository.AddressRepository;
import org.example.udemyproject.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;

    public AddressServiceImpl(AddressRepository addressRepository, ModelMapper modelMapper, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.modelMapper = modelMapper;
        this.userRepository = userRepository;
    }

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {
        Address address = modelMapper.map(addressDTO, Address.class);

        List<Address> addressList = user.getAddresses();
        addressList.add(address);
        user.setAddresses(addressList);
        address.setUser(user);

        Address savedAddress = addressRepository.save(address);
        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getAddresses() {
        List<Address> addressList = addressRepository.findAll();
        return addressList.stream()
                .map(this::mapAddress)
                .toList();
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId",  addressId));
        return mapAddress(address);
    }

    @Override
    public List<AddressDTO> getUserAddresses(User user) {
        List<Address> addressList = user.getAddresses();
        return addressList.stream()
                .map(this::mapAddress)
                .toList();
    }

    @Override
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        Address addressFromDB = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId",  addressId));

        addressFromDB.setCity(addressDTO.getCity());
        addressFromDB.setCountry(addressDTO.getCountry());
        addressFromDB.setStreet(addressDTO.getStreet());
        addressFromDB.setState(addressDTO.getState());
        addressFromDB.setPincode(addressDTO.getPincode());
        addressFromDB.setBuildingName(addressDTO.getBuildingName());

        Address updatedAddress = addressRepository.save(addressFromDB);

        User user =  addressFromDB.getUser();
        user.getAddresses().removeIf(address -> address.getAddressId().equals(updatedAddress.getAddressId()));
        user.getAddresses().add(updatedAddress);
        userRepository.save(user);

        return mapAddress(updatedAddress);
    }

    @Override
    public String deleteAddress(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId",  addressId));
        User user =  address.getUser();
        user.getAddresses().removeIf(a -> a.getAddressId().equals(address.getAddressId()));
        userRepository.save(user);
        addressRepository.delete(address);

        return "Address has been deleted";
    }

    private AddressDTO mapAddress(Address address) {
        return modelMapper.map(address, AddressDTO.class);
    }
}
