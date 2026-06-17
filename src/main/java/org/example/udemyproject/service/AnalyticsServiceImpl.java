package org.example.udemyproject.service;

import org.example.udemyproject.payload.AnalyticsResponse;
import org.example.udemyproject.repository.OrderRepository;
import org.example.udemyproject.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public AnalyticsServiceImpl(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public AnalyticsResponse getAnalyticsData() {
        AnalyticsResponse response = new AnalyticsResponse();

        long productCount = productRepository.count();
        long totalOrders = orderRepository.count();
        Double totalRevenue = orderRepository.getTotalRevenue();

        response.setProductCount(String.valueOf(productCount));
        response.setTotalOrders(String.valueOf(totalOrders));
        response.setTotalRevenue(String.valueOf(totalRevenue != null ? totalRevenue : 0));

        return response;
    }
}
