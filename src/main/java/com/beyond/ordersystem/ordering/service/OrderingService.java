package com.beyond.ordersystem.ordering.service;

import com.beyond.ordersystem.common.service.SseAlarmService;
import com.beyond.ordersystem.common.service.StockRabbitMqService;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.repository.MemberRepository;
import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.OrderCreateDto;
import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.beyond.ordersystem.ordering.repository.OrderDetailRepository;
import com.beyond.ordersystem.ordering.repository.OrderingRepository;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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
    private final StockInventoryService stockInventoryService;
    private final StockRabbitMqService stockRabbitMqService;
    private final SseAlarmService sseAlarmService;

    public Long create(List<OrderCreateDto> orderCreateDtoList){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 사용자입니다."));

        Ordering ordering = Ordering.builder()
                .member(member)
                .build();

        for (OrderCreateDto dto : orderCreateDtoList){
            Product product = productRepository.findById(dto.getProductId()).orElseThrow(()->new EntityNotFoundException("Product is not found"));
            if (product.getStockQuantity() < dto.getProductCount()){
//                예외를 강제 발생 시킴으로서, 모두 임시저장사항들을 rollback처리
                throw new IllegalArgumentException("재고가 부족합니다.");
            }
//            1. 동시에 접근하는 상황에서 update값의 정합성이 깨지고 갱신이상이 발생할 수 있다.
//            2. spring 버전이나 mysql버전에 따라 jpa에서 강제에러(deadlock)를 유발시켜 대부분의 요청실패 발생
            product.updateStockQuantity(dto.getProductCount());

            OrderDetail orderDetail = OrderDetail.builder()
                    .product(product)
                    .quantity(dto.getProductCount())
                    .ordering(ordering)
                    .build();
//            orderDetailRepository.save(orderDetail);
            ordering.getOrderDetailsList().add(orderDetail);
        }
        orderingRepository.save(ordering);
//        큐에 메시지를 담는다.
        return ordering.getId();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED) // 격리레벨을 낮춤으로서, 성능향상과 lock관련 문제 원천 차단
    public Long createConcurrent(List<OrderCreateDto> orderCreateDtoList){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 사용자입니다."));

        Ordering ordering = Ordering.builder()
                .member(member)
                .build();

        for (OrderCreateDto dto : orderCreateDtoList){
            Product product = productRepository.findById(dto.getProductId()).orElseThrow(()->new EntityNotFoundException("Product is not found"));
//            redis에서 재고수량 확인 및 재고수량 감소 처리
            int newQuantity = stockInventoryService.decreaseStockQuantity(product.getId(), dto.getProductCount());
            if (newQuantity < 0){
                throw new IllegalArgumentException("재고부족");
            }
            OrderDetail orderDetail = OrderDetail.builder()
                    .product(product)
                    .quantity(dto.getProductCount())
                    .ordering(ordering)
                    .build();
//            orderDetailRepository.save(orderDetail);
            ordering.getOrderDetailsList().add(orderDetail);
//          큐에 메시지를 담는다.
//            rdb에 사후 update를 위한 메시지 발행(비동기 처리)
            stockRabbitMqService.publish(dto.getProductId(), dto.getProductCount());
        }
        orderingRepository.save(ordering);

//        주문 성공시 admin 유저에게 알림메시지 전송
        sseAlarmService.publishMessage("admin@naver.com", email, ordering.getId());

        return ordering.getId();
    }

    public List<OrderListResDto> findAll(){
        return orderingRepository.findAll().stream().map(o->OrderListResDto.fromEntity(o)).collect(Collectors.toList());
    }
    public List<OrderListResDto> myOrders(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("없는 사용자"));
        return orderingRepository.findAllByMember(member).stream().map(o->OrderListResDto.fromEntity(o)).collect(Collectors.toList());
    }

    public Ordering cancel(Long id){
//        Ordering DB에 상태값 변경 Canceled
        Ordering ordering = orderingRepository.findById(id).orElseThrow(()->new EntityNotFoundException("주문내역이 없습니다."));
        ordering.cancelStatus();

        for (OrderDetail orderDetail : ordering.getOrderDetailsList()){
//        rdb재고 업데이트
            orderDetail.getProduct().cancelOrder(orderDetail.getQuantity());
//        redis의 재고값 증가
            stockInventoryService.increaseStockQuantity(orderDetail.getProduct().getId(), orderDetail.getQuantity());
        }
        return ordering;
    }
}
