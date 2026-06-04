package org.example.udemyproject.service;

import jakarta.validation.Valid;
import org.example.udemyproject.model.User;
import org.example.udemyproject.payload.AddressDTO;

import java.util.List;

public interface AddressService {
    AddressDTO createAddress(AddressDTO addressDTO, User user);

    List<AddressDTO> getAddresses();

    AddressDTO getAddressById(Long addressId);

    List<AddressDTO> getUserAddresses(User user);

    AddressDTO updateAddress(Long addressId, @Valid AddressDTO addressDTO);

    String deleteAddress(Long addressId);
}
