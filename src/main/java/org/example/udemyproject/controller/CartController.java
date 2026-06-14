package org.example.udemyproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.udemyproject.model.Cart;
import org.example.udemyproject.payload.APIResponse;
import org.example.udemyproject.payload.CartDTO;
import org.example.udemyproject.payload.CartItemDTO;
import org.example.udemyproject.repository.CartRepository;
import org.example.udemyproject.service.CartService;
import org.example.udemyproject.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Cart", description = "Shopping cart endpoints")
@SecurityRequirement(name = "bearer")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private CartRepository cartRepository;

    @PostMapping("/cart/create")
    public ResponseEntity<String> createOrUpdateCart(@RequestBody List<CartItemDTO> cartItems) {
        String response = cartService.createOrUpdateCartWithItems(cartItems);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/carts/products/{productId}/quantity/{quantity}")
    @Operation(summary = "Add product to cart", description = "Adds a product to the authenticated user's cart.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product added to cart successfully",
                    content = @Content(schema = @Schema(implementation = CartDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid cart update request",
                    content = @Content(schema = @Schema(implementation = APIResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Product or cart not found",
                    content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<CartDTO> addProductToCart(@PathVariable Long productId,
                                                    @PathVariable Integer quantity) {
        CartDTO cartDto = cartService.addProductToCart(productId, quantity);
        return new ResponseEntity<>(cartDto, HttpStatus.OK);
    }

    @GetMapping("/carts")
    @Operation(summary = "List carts", description = "Returns all carts.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Carts returned successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CartDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<List<CartDTO>> getCarts() {
        List<CartDTO> cartDTOs = cartService.getAllCarts();
        return new ResponseEntity<>(cartDTOs, HttpStatus.FOUND);
    }

    @GetMapping("/carts/users/cart")
    @Operation(summary = "Get current user cart", description = "Returns the cart that belongs to the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart returned successfully",
                    content = @Content(schema = @Schema(implementation = CartDTO.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Cart not found",
                    content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<CartDTO> getCartById() {
        String emailId = authUtil.loggedInEmail();
        Cart cart = cartRepository.findCartByEmail(emailId);
        Long cartId = cart.getCartId();

        CartDTO cartDTO = cartService.getCart(emailId, cartId);
        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }

    @PutMapping("/cart/products/{productId}/quantity/{operation}")
    @Operation(summary = "Update cart item quantity", description = "Updates the quantity of a product in the authenticated user's cart.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart updated successfully",
                    content = @Content(schema = @Schema(implementation = CartDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid cart update request",
                    content = @Content(schema = @Schema(implementation = APIResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Product or cart not found",
                    content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<CartDTO> updateCartProduct(@PathVariable Long productId,
                                                     @PathVariable String operation) {

        CartDTO updatedCart = cartService.updateProductQuantityInCart(productId,
                operation.equalsIgnoreCase("delete") ? -1 : 1);
        return new ResponseEntity<>(updatedCart, HttpStatus.OK);
    }

    @DeleteMapping("/carts/{cartId}/product/{productId}")
    @Operation(summary = "Remove product from cart", description = "Removes a product from a cart by cart and product IDs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product removed from cart successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "Product or cart not found",
                    content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<String> deleteProductFromCart(@PathVariable Long cartId,
                                                        @PathVariable Long productId) {

        String status = cartService.deleteProductFromCart(cartId, productId);
        return new ResponseEntity<>(status, HttpStatus.OK);
    }
}
