package org.example.udemyproject.repository;

import org.example.udemyproject.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("SELECT ci FROM CartItem ci WHERE ci.product.productId = ?1 AND ci.cart.cartId = ?2")
    CartItem findCartItemByProductIdAndCartId(Long productId, Long cartId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.product.productId = ?1 AND ci.cart.cartId = ?2")
    void deleteCartItemByProductIdAndCartId(Long productId, Long cartId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem ci WHERE ci.cart.cartId = ?1")
    void deleteAllByCartId(Long cartId);
}
