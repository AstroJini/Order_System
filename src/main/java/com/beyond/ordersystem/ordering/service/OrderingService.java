package com.beyond.ordersystem.ordering.service;

import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.dto.MemberResDto;
import com.beyond.ordersystem.member.repository.MemberRepository;
import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.OrderCreateDto;
import com.beyond.ordersystem.ordering.dto.OrderDetailResDto;
import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.beyond.ordersystem.ordering.repository.OrderDetailRepository;
import com.beyond.ordersystem.ordering.repository.OrderingRepository;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OrderDetailRepository orderDetailRepository;

    public Long create(List<OrderCreateDto> orderCreateDtoList){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 사용자입니다."));

        Ordering ordering = Ordering.builder()
                .member(member)
                .build();

        orderingRepository.save(ordering);

        for (OrderCreateDto dto : orderCreateDtoList){
            Product product = productRepository.findById(dto.getProductId()).orElseThrow(()->new EntityNotFoundException("Product is not found"));
            if (product.getStockQuantity() < dto.getProductCount()){
//                예외를 강제 발생 시킴으로서, 모두 임시저장사항들을 rollback처리
                throw new IllegalArgumentException("재고가 부족합니다.");
            }
            product.updateStockQuantity(dto.getProductCount());
            OrderDetail orderDetail = OrderDetail.builder()
                    .product(product)
                    .quantity(dto.getProductCount())
                    .ordering(ordering)
                    .build();
            orderDetailRepository.save(orderDetail);
            ordering.getOrderDetailsList().add(orderDetail);
        }
        return ordering.getId();
    }

    @Transactional(readOnly = true)
    public List<OrderListResDto> findAll(){
        List<Ordering> orderingList = orderingRepository.findAll();
        List<OrderListResDto> orderListResDtoList = new ArrayList<>();
        for (Ordering ordering : orderingList){
            List<OrderDetail> orderDetailList = ordering.getOrderDetailsList();
            List<OrderDetailResDto> orderDetailResDtosList = new ArrayList<>();
            for (OrderDetail orderDetail : orderDetailList]){
                OrderDetailResDto orderDetailResDto = OrderDetailResDto.builder()
                        .detailId(orderDetail)
                        .build();
            }

            OrderListResDto dto = OrderListResDto.builder()
                    .id(ordering.getId())
                    .memberEmail(ordering.getMember().getEmail())
                    .orderStatus(ordering.getOrderStatus())
                    .orderDetails(orderDetailResDtosList)
                    .build();
            orderListResDtoList.add(dto);
        }
        return  orderListResDtoList;
    }
}
