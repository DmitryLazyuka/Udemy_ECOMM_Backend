package org.example.udemyproject.service;

import org.example.udemyproject.payload.OrderDTO;
import org.example.udemyproject.payload.OrderResponse;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface OrderService {
    OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage);

    OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    OrderDTO updateOrder(Long orderId, String status);
}
