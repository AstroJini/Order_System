package com.beyond.ordersystem.ordering.controller;

import com.beyond.ordersystem.common.dto.CommonDto;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.OrderCreateDto;
import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.beyond.ordersystem.ordering.service.OrderingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ordering")
public class OrderController {
    private final OrderingService orderingService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody List<OrderCreateDto> orderCreateDtos){
        Long id = orderingService.createConcurrent(orderCreateDtos);
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(id)
                        .status_code(HttpStatus.CREATED.value())
                        .status_message("배달의 민족 주문!")
                        .build(),HttpStatus.CREATED);
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findAll(){
        List<OrderListResDto> orderListResDtos = orderingService.findAll();
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(orderListResDtos)
                        .status_code(HttpStatus.OK.value())
                        .status_message("주문내역 조회 성공!")
                        .build(),HttpStatus.OK);
    }

    @GetMapping("/myorders")
    public ResponseEntity<?> myOrders(){
        List<OrderListResDto> orderListResDtos = orderingService.myOrders();
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(orderListResDtos)
                        .status_code(HttpStatus.OK.value())
                        .status_message("내 주문목록 조회 성공!")
                        .build(),HttpStatus.OK);
    }
    @DeleteMapping("/cancel/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> ordercancel(@PathVariable Long id){
        Ordering ordering = orderingService.cancel(id);
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(ordering.getId())
                        .status_code(HttpStatus.OK.value())
                        .status_message("주문 취소 성공")
                        .build(),HttpStatus.OK);    }
}
