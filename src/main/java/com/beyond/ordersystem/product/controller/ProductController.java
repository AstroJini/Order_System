package com.beyond.ordersystem.product.controller;

import com.beyond.ordersystem.common.dto.CommonDto;
import com.beyond.ordersystem.product.dto.ProductSearchDto;
import com.beyond.ordersystem.product.dto.ProductCreateDto;
import com.beyond.ordersystem.product.dto.ProductResDto;
import com.beyond.ordersystem.product.dto.ProductUpdateDto;
import com.beyond.ordersystem.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
public class ProductController {
    private final ProductService productService;

//    @PostMapping("/create")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<?> save(@ModelAttribute ProductCreateDto productCreateDto){
//        productService.save(productCreateDto);
//        return new ResponseEntity<>(
//                CommonDto.builder()
//                        .result(productCreateDto.getProductImage().getOriginalFilename())
//                        .status_code(HttpStatus.CREATED.value())
//                        .status_message("상품 등록 완료!")
//                        .build(),HttpStatus.CREATED);
//    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@ModelAttribute @Valid ProductCreateDto productCreateDto) {
        try {
            Long id = this.productService.save(productCreateDto);
            return (new ResponseEntity<>(
                    CommonDto.builder()
                            .result(id)
                            .status_code(HttpStatus.CREATED.value())
                            .status_message("상품 등록 성공!")
                            .build(),
                    HttpStatus.CREATED));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return (new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> findAll(Pageable pageable, ProductSearchDto productSearchDto){
        Page<ProductResDto> productResDtoList = productService.findAll(pageable, productSearchDto);
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(productResDtoList)
                        .status_code(HttpStatus.OK.value())
                        .status_message("상품목록조회 성공")
                        .build(), HttpStatus.OK);
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id){
        ProductResDto productResDto = productService.findById(id);
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(productResDto)
                        .status_code(HttpStatus.OK.value())
                        .status_message("id " + id + "번 상품 상세정보 입니다.")
                        .build(),HttpStatus.OK);
    }
//    @PutMapping("/update/{id}")
//    public ResponseEntity<?> updateProduct(@PathVariable Long productId, @ModelAttribute ProductUpdateDto productUpdateDto) {
//        Long id = productService.updateProduct(productUpdateDto, productId);
//        return new ResponseEntity<>(
//                CommonDto.builder()
//                        .result(id)
//                        .status_message("상품정보 수정 완료")
//                        .status_code(HttpStatus.OK.value())
//                        .build(),HttpStatus.OK);
//    }
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@ModelAttribute @Valid ProductUpdateDto productUpdateDto, @PathVariable Long id) {
        try {
            this.productService.update(productUpdateDto, id);
            return (new ResponseEntity<>(
                    CommonDto.builder()
                            .result(id)
                            .status_code(HttpStatus.OK.value())
                            .status_message("상품 수정 성공!")
                            .build(),
                    HttpStatus.OK));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return (new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST));
        }
    }
}
