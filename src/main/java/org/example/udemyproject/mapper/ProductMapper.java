package org.example.udemyproject.mapper;

import org.example.udemyproject.model.Product;
import org.example.udemyproject.payload.ProductDTO;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    @Value("${image.base.url}")
    private String imageBaseUrl;

    private final ModelMapper modelMapper;

    public ProductMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public ProductDTO toDTO(Product product) {
        ProductDTO dto = modelMapper.map(product, ProductDTO.class);
        dto.setImage(constructImageUrl(product.getImage()));
        return dto;
    }

    private String constructImageUrl(String imageName) {
        return imageBaseUrl.endsWith("/")
                ? imageBaseUrl + imageName
                : imageBaseUrl + "/" + imageName;
    }
}
