package org.example.udemyproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.udemyproject.payload.APIResponse;
import org.example.udemyproject.payload.OrderDTO;
import org.example.udemyproject.payload.OrderRequestDTO;
import org.example.udemyproject.service.OrderService;
import org.example.udemyproject.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Orders", description = "Order placement endpoints")
@SecurityRequirement(name = "bearer")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AuthUtil authUtil;

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
}
