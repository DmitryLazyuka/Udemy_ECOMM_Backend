package org.example.udemyproject.service;

import org.example.udemyproject.payload.OrderDTO;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface OrderService {
    OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage);
}
