package org.example.udemyproject.repository;

import org.example.udemyproject.model.Category;
import org.example.udemyproject.model.Product;
import org.example.udemyproject.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Page<Product> findByCategoryAndActiveTrueOrderByPriceAsc(Category category, Pageable pageable);

    Page<Product> findByProductNameLikeIgnoreCaseAndActiveTrue(String keyword, Pageable pageable);

    boolean existsProductByProductNameAndDescription(String productName, String description);

    boolean existsByCategoryCategoryId(Long categoryId);

    Page<Product> findByUserAndActiveTrue(User user, Pageable pageable);

    Page<Product> findByActiveTrue(Pageable pageable);
}