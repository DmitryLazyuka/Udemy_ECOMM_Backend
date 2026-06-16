package org.example.udemyproject.service;

import org.example.udemyproject.exceptions.APIException;
import org.example.udemyproject.exceptions.ResourceNotFoundException;
import org.example.udemyproject.model.Cart;
import org.example.udemyproject.model.Category;
import org.example.udemyproject.model.Product;
import org.example.udemyproject.payload.CartDTO;
import org.example.udemyproject.payload.ProductDTO;
import org.example.udemyproject.payload.ProductResponse;
import org.example.udemyproject.repository.CartRepository;
import org.example.udemyproject.repository.CategoryRepository;
import org.example.udemyproject.repository.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    @Value("${image.base.url}")
    private String imageBaseUrl;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartService cartService;

    @Value("${project.image}")
    private String path;

    @Override
    public ProductDTO addProduct(ProductDTO productDTO, Long categoryId) {
        if (productRepository.existsProductByProductNameAndDescription(productDTO.getProductName(), productDTO.getDescription())) {
            throw new APIException("Product already exists");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        Product product = modelMapper.map(productDTO, Product.class);

        product.setImage("default.png");
        product.setCategory(category);
        product.setSpecialPrice(countSpecialPrice(product.getPrice(), product.getDiscount()));
        Product savedProduct = productRepository.save(product);
        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    @Override
    public ProductResponse getAllProducts(Integer pageNumber,
                                          Integer pageSize,
                                          String sortBy,
                                          String sortOrder,
                                          String keyword,
                                          String category) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Specification<Product> spec = Specification.unrestricted();

        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
        }
        if (category != null && !category.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(root.get("category").get("categoryName"), category));
        }

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<Product> products = productPage.getContent();
        if (products.isEmpty()) {
            throw new APIException("No products found");
        }

        List<ProductDTO> productDTOS = products.stream()
                .map(p -> {
                            ProductDTO productDTO = modelMapper.map(p, ProductDTO.class);
                            productDTO.setImage(constructImageUrl(p.getImage()));
                            return productDTO;
                        }
                )
                .toList();

        ProductResponse productResponse = new ProductResponse();

        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setLastPage(productPage.isLast());

        return productResponse;
    }

    private String constructImageUrl(String imageName) {
        return imageBaseUrl.endsWith("/") ? imageBaseUrl + imageName : imageBaseUrl + "/" + imageName;
    }

    @Override
    public ProductResponse searchByCategory(Long categoryId,
                                            Integer pageNumber,
                                            Integer pageSize,
                                            String sortBy,
                                            String sortOrder) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Page<Product> productPage = productRepository.findByCategoryOrderByPriceAsc(category, pageable);
        List<Product> products = productPage.getContent();

        if (products.isEmpty()) {
            throw new APIException("No products found");
        }
        List<ProductDTO> productDTOS = products.stream()
                .map(p -> modelMapper.map(p, ProductDTO.class))
                .toList();

        ProductResponse productResponse = new ProductResponse();

        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setLastPage(productPage.isLast());

        return productResponse;
    }

    @Override
    public ProductResponse searchProductsByKeyword(String keyword,
                                                   Integer pageNumber,
                                                   Integer pageSize,
                                                   String sortBy,
                                                   String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);

        Page<Product> productPage = productRepository.findByProductNameLikeIgnoreCase('%' + keyword + '%', pageable);

        List<Product> products = productPage.getContent();
        if (products.isEmpty()) {
            throw new APIException("No products found");
        }
        List<ProductDTO> productDTOS = products.stream()
                .map(p -> modelMapper.map(p, ProductDTO.class))
                .toList();

        ProductResponse productResponse = new ProductResponse();

        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setLastPage(productPage.isLast());

        return productResponse;
    }

    @Override
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        Product product = modelMapper.map(productDTO, Product.class);

        productFromDB.setProductName(product.getProductName());
        productFromDB.setDescription(product.getDescription());
        productFromDB.setQuantity(product.getQuantity());
        productFromDB.setPrice(product.getPrice());
        productFromDB.setDiscount(product.getDiscount());
        productFromDB.setSpecialPrice(countSpecialPrice(productFromDB.getPrice(), productFromDB.getDiscount()));

        Product savedProduct = productRepository.save(productFromDB);

        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        List<CartDTO> cartDTOs = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
            List<ProductDTO> products = cart.getCartItems().stream().map(p -> modelMapper.map(p.getProduct(), ProductDTO.class)).toList();
            cartDTO.setProducts(products);
            return cartDTO;
        }).toList();

        cartDTOs.forEach(cart -> cartService.updateProductInCarts(cart.getCartId(), productId));

        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    @Override
    public ProductDTO deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        carts.forEach(cart -> cartService.deleteProductFromCart(cart.getCartId(), productId));

        productRepository.delete(product);
        return modelMapper.map(product, ProductDTO.class);
    }

    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        String fileName = fileService.uploadImage(path, image);

        productFromDB.setImage(fileName);
        Product savedProduct = productRepository.save(productFromDB);
        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    @Override
    public ProductResponse getAllProductsForAdmin(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sortByAndOrder);


        Page<Product> productPage = productRepository.findAll(pageable);
        List<Product> products = productPage.getContent();
        if (products.isEmpty()) {
            throw new APIException("No products found");
        }

        List<ProductDTO> productDTOS = products.stream()
                .map(p -> {
                            ProductDTO productDTO = modelMapper.map(p, ProductDTO.class);
                            productDTO.setImage(constructImageUrl(p.getImage()));
                            return productDTO;
                        }
                )
                .toList();

        ProductResponse productResponse = new ProductResponse();

        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(productPage.getNumber());
        productResponse.setPageSize(productPage.getSize());
        productResponse.setTotalPages(productPage.getTotalPages());
        productResponse.setTotalElements(productPage.getTotalElements());
        productResponse.setLastPage(productPage.isLast());

        return productResponse;
    }

    private Double countSpecialPrice(Double price, Double discount) {
        return price - ((discount * 0.01) * price);
    }
}