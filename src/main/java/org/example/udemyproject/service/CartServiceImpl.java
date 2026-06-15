package org.example.udemyproject.service;

import org.example.udemyproject.exceptions.APIException;
import org.example.udemyproject.exceptions.ResourceNotFoundException;
import org.example.udemyproject.model.Cart;
import org.example.udemyproject.model.CartItem;
import org.example.udemyproject.model.Product;
import org.example.udemyproject.payload.CartDTO;
import org.example.udemyproject.payload.CartItemDTO;
import org.example.udemyproject.payload.ProductDTO;
import org.example.udemyproject.repository.CartItemRepository;
import org.example.udemyproject.repository.CartRepository;
import org.example.udemyproject.repository.ProductRepository;
import org.example.udemyproject.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {

        Cart cart = createCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem existingCartItem =
                cartItemRepository.findCartItemByProductIdAndCartId(productId, cart.getCartId());

        if (existingCartItem != null) {
            throw new APIException("Product \"" + product.getProductName() + "\" already exists in the Cart");
        }

        if (product.getQuantity() == 0) {
            throw new APIException("Product \"" + product.getProductName() + "\" is not available");
        }

        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the \"" + product.getProductName()
                    + "\" less or equal to the quantity " + product.getQuantity());
        }

        CartItem newCartItem = new CartItem();
        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());

        CartItem savedCartItem = cartItemRepository.save(newCartItem);

        cart.getCartItems().add(savedCartItem);

        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));

        Cart savedCart = cartRepository.save(cart);

        CartDTO cartDTO = modelMapper.map(savedCart, CartDTO.class);

        List<ProductDTO> products = savedCart.getCartItems().stream()
                .map(item -> {
                    ProductDTO productDTO = modelMapper.map(item.getProduct(), ProductDTO.class);
                    productDTO.setQuantity(item.getQuantity());
                    return productDTO;
                })
                .toList();

        cartDTO.setProducts(products);

        return cartDTO;
    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();

        if (carts.isEmpty()) {
            throw new APIException("No cart exists");
        }

        List<CartDTO> cartDTOs = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

            List<ProductDTO> products = cart.getCartItems().stream().map(cartItem -> {
                ProductDTO productDTO = modelMapper.map(cartItem.getProduct(), ProductDTO.class);
                productDTO.setQuantity(cartItem.getQuantity()); // Set the quantity from CartItem
                return productDTO;
            }).collect(Collectors.toList());


            cartDTO.setProducts(products);

            return cartDTO;

        }).collect(Collectors.toList());

        return cartDTOs;
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(emailId, cartId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "cartId", cartId);
        }
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        cart.getCartItems().forEach(c -> {
            c.getProduct().setQuantity(c.getQuantity());
        });
        List<ProductDTO> products = cart.getCartItems().stream()
                .map(p -> modelMapper.map(p.getProduct(), ProductDTO.class))
                .toList();
        cartDTO.setProducts(products);
        return cartDTO;
    }

    @Override
    @Transactional
    public CartDTO updateProductQuantityInCart(Long productId, int quantity) {
        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);
        Long cartId = userCart.getCartId();

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (product.getQuantity() == 0) {
            throw new APIException("Product \"" + product.getProductName() + "\" is not available");
        }
        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the \"" + product.getProductName()
                    + "\" less or equal to the quantity " + product.getQuantity());
        }

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(productId, cartId);
        if (cartItem == null) {
            throw new APIException("Product \"" + product.getProductName() + "\" is not available in the Cart");
        }

        int newQuantity = cartItem.getQuantity() + quantity;
        if (newQuantity < 0) {
            throw new APIException("The resulting quantity can not be negative");
        }
        if (newQuantity == 0) {
            deleteProductFromCart(cartId, productId);
        } else {
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * quantity));
            cartRepository.save(cart);
        }

        CartItem updatedCartItem = cartItemRepository.save(cartItem);

        if (updatedCartItem.getQuantity() == 0) {
            cartItemRepository.deleteById(updatedCartItem.getCartItemId());
        }

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<CartItem> cartItems = cart.getCartItems();

        Stream<ProductDTO> productStream = cartItems.stream()
                .map(item -> {
                    ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
                    map.setQuantity(item.getQuantity());
                    return map;
                });
        cartDTO.setProducts(productStream.toList());
        return cartDTO;
    }

    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(productId, cartId);
        if (cartItem == null) {
            throw new ResourceNotFoundException("Product", "productId", productId);
        }

        cart.setTotalPrice(cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity()));
        cartItemRepository.deleteCartItemByProductIdAndCartId(productId, cartId);

        return "Product " + cartItem.getProduct().getProductName() + " has been deleted from the Cart";
    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(productId, cartId);
        if (cartItem == null) {
            throw new APIException("Product \"" + product.getProductName() + "\" not available");
        }

        double cartPrice = cart.getTotalPrice() - (cartItem.getProductPrice() * cartItem.getQuantity());
        cartItem.setProductPrice(product.getSpecialPrice());

        cart.setTotalPrice(cartPrice + (cartItem.getProductPrice() * product.getQuantity()));

        cartItemRepository.save(cartItem);
    }

    @Override
    public String createOrUpdateCartWithItems(List<CartItemDTO> cartItems) {
        String emailId = authUtil.loggedInEmail();

        Cart existingCart = cartRepository.findCartByEmail(emailId);
        if (existingCart == null) {
            existingCart = new Cart();
            existingCart.setTotalPrice(0.0);
            existingCart.setUser(authUtil.loggedInUser());
            existingCart = cartRepository.save(existingCart);
        } else {
            cartItemRepository.deleteAllByCartId(existingCart.getCartId());
        }
        double totalPrice = 0.0;
        for (CartItemDTO cartItemDTO : cartItems) {
            Long productId = cartItemDTO.getProductId();
            Integer quantity = cartItemDTO.getQuantity();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
//            product.setQuantity(product.getQuantity() - quantity);
            totalPrice += product.getSpecialPrice() * quantity;

            CartItem cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setCart(existingCart);
            cartItem.setQuantity(quantity);
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setDiscount(product.getDiscount());
            cartItemRepository.save(cartItem);
        }

        existingCart.setTotalPrice(totalPrice);
        cartRepository.save(existingCart);
        return "Cart has been updated with new items successfully";
    }


    private Cart createCart() {
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if (userCart != null) {
            return userCart;
        }
        Cart cart = new Cart();
        cart.setTotalPrice(0.0);
        cart.setUser(authUtil.loggedInUser());
        return cartRepository.save(cart);
    }
}
