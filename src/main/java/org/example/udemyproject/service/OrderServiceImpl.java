package org.example.udemyproject.service;

import org.example.udemyproject.exceptions.APIException;
import org.example.udemyproject.exceptions.ResourceNotFoundException;
import org.example.udemyproject.model.*;
import org.example.udemyproject.payload.OrderDTO;
import org.example.udemyproject.payload.OrderItemDTO;
import org.example.udemyproject.payload.OrderResponse;
import org.example.udemyproject.repository.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    @Transactional
    public OrderDTO placeOrder(String emailId,
                               Long addressId,
                               String paymentMethod,
                               String pgName,
                               String pgPaymentId,
                               String pgStatus,
                               String pgResponseMessage) {

        if (addressId == null) {
            throw new APIException("Address id must not be null");
        }

        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new APIException("Payment method must not be null");
        }

        Cart cart = cartRepository.findCartByEmail(emailId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "email", emailId);
        }

        List<CartItem> cartItems = new ArrayList<>(cart.getCartItems());
        if (cartItems.isEmpty()) {
            throw new APIException("Cart is empty");
        }

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));

        Order order = new Order();
        order.setEmail(emailId);
        order.setDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Order Accepted");
        order.setAddress(address);

        Payment payment = new Payment();
        payment.setPaymentMethod(paymentMethod);
        payment.setPgName(pgName);
        payment.setPgPaymentId(pgPaymentId);
        payment.setPgStatus(pgStatus);
        payment.setPgResponseMessage(pgResponseMessage);

        Payment savedPayment = paymentRepository.save(payment);

        order.setPayment(savedPayment);

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if (product == null || product.getProductId() == null) {
                throw new APIException("Product in cart item is null");
            }

            if (product.getQuantity() < cartItem.getQuantity()) {
                throw new APIException(
                        "Not enough quantity for productId: " + product.getProductId()
                );
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderedProductPrice(cartItem.getProductPrice());
            orderItem.setOrder(savedOrder);

            orderItems.add(orderItem);
            savedOrder.getOrderItems().add(orderItem);

            product.setQuantity(product.getQuantity() - cartItem.getQuantity());
        }

        productRepository.saveAll(
                cartItems.stream()
                        .map(CartItem::getProduct)
                        .toList()
        );

        orderItems = orderItemRepository.saveAll(orderItems);

        cart.getCartItems().clear();
        cart.setTotalPrice(0.0);
        cartRepository.save(cart);

        OrderDTO orderDTO = modelMapper.map(savedOrder, OrderDTO.class);

        if (orderDTO.getOrderItems() == null) {
            orderDTO.setOrderItems(new ArrayList<>());
        }

        orderDTO.getOrderItems().clear();

        orderItems.forEach(item ->
                orderDTO.getOrderItems().add(
                        modelMapper.map(item, OrderItemDTO.class)
                )
        );

        orderDTO.setAddressId(addressId);

        return orderDTO;
    }

    @Override
    public OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Order> pageOrders = orderRepository.findAll(pageDetails);
        List<Order> orders = pageOrders.getContent();
        List<OrderDTO> orderDTOs = orders.stream().map(order -> modelMapper.map(order, OrderDTO.class)).toList();

        OrderResponse response = new OrderResponse();
        response.setContent(orderDTOs);
        response.setPageNumber(pageOrders.getNumber());
        response.setPageSize(pageOrders.getSize());
        response.setTotalPages(pageOrders.getTotalPages());
        response.setTotalElements(pageOrders.getTotalElements());
        response.setLastPage(pageOrders.isLast());

        return response;
    }
}

