package org.example.udemyproject.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.udemyproject.config.AppConstants;
import org.example.udemyproject.payload.*;
import org.example.udemyproject.service.OrderService;
import org.example.udemyproject.service.StripeService;
import org.example.udemyproject.util.AuthUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Orders", description = "Order placement endpoints")
@SecurityRequirement(name = "bearer")
public class OrderController {

    private final OrderService orderService;
    private final AuthUtil authUtil;
    private final StripeService stripeService;

    public OrderController(OrderService orderService, AuthUtil authUtil, StripeService stripeService) {
        this.orderService = orderService;
        this.authUtil = authUtil;
        this.stripeService = stripeService;
    }

    @PostMapping("/order/users/payments/{paymentMethod}")
    @Operation(summary = "Place order", description = "Places an order for the authenticated user using the selected payment method.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order placed successfully",
                    content = @Content(schema = @Schema(implementation = OrderDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid order request",
                    content = @Content(schema = @Schema(implementation = APIResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Cart, address, or product not found",
                    content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<OrderDTO> orderProducts(@PathVariable String paymentMethod,
                                                  @RequestBody OrderRequestDTO request) {

        String emailId = authUtil.loggedInEmail();

        OrderDTO order = orderService.placeOrder(emailId,
                request.getAddressId(),
                paymentMethod,
                request.getPgName(),
                request.getPgPaymentId(),
                request.getPgStatus(),
                request.getPgResponseMessage());

        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @PostMapping("/order/stripe-client-secret")
    @Operation(summary = "Create Stripe client secret", description = "Creates a Stripe payment intent and returns its client secret.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Stripe client secret created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid Stripe payment payload",
                    content = @Content(schema = @Schema(implementation = APIResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<String> createStripeClientSecret(@RequestBody StripePaymentDTO stripePaymentDTO) throws StripeException {
        PaymentIntent paymentIntent = stripeService.paymentIntent(stripePaymentDTO);
        return new ResponseEntity<>(paymentIntent.getClientSecret(), HttpStatus.CREATED);
    }

    @GetMapping("/admin/orders")
    @Operation(summary = "List all orders", description = "Returns a paginated list of all orders for admins.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders returned successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<OrderResponse> getAllOrders(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_ORDERS_NAME, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder
    ) {
     OrderResponse response = orderService.getAllOrders(pageNumber, pageSize, sortBy, sortOrder);
     return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/admin/orders/{orderId}/status")
    @Operation(summary = "Update order status as admin", description = "Updates the status of an order using admin privileges.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status updated successfully",
                    content = @Content(schema = @Schema(implementation = OrderDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status update payload",
                    content = @Content(schema = @Schema(implementation = APIResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable Long orderId,
                                                      @RequestBody OrderStatusUpdateDto orderStatusUpdateDto){
        OrderDTO order = orderService.updateOrder(orderId, orderStatusUpdateDto.getStatus());
        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @GetMapping("/seller/orders")
    @Operation(summary = "List seller orders", description = "Returns a paginated list of orders that belong to the authenticated seller.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seller orders returned successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<OrderResponse> getAllSellerOrders(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_ORDERS_NAME, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder
    ) {
        OrderResponse response = orderService.getAllSellerOrders(pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping("/seller/orders/{orderId}/status")
    @Operation(summary = "Update seller order status", description = "Updates the status of an order that belongs to the authenticated seller.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status updated successfully",
                    content = @Content(schema = @Schema(implementation = OrderDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status update payload",
                    content = @Content(schema = @Schema(implementation = APIResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<OrderDTO> updateOrderStatusSeller(@PathVariable Long orderId,
                                                      @RequestBody OrderStatusUpdateDto orderStatusUpdateDto){
        OrderDTO order = orderService.updateOrder(orderId, orderStatusUpdateDto.getStatus());
        return new ResponseEntity<>(order, HttpStatus.OK);
    }

    @GetMapping("/orders")
    @Operation(summary = "List user orders", description = "Returns a paginated list of orders that belong to the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seller orders returned successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<OrderResponse> getAllUserOrders(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_ORDERS_NAME, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder
    ) {
        OrderResponse response = orderService.getAllUserOrders(pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
