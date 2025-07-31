package com.beyond.ordersystem.ordering.dto;

import com.beyond.ordersystem.ordering.domain.OrderDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class OrderDetailResDto {
    private Long detailId;
    private String productName;
    private Integer productCount;

    public static OrderDetailResDto fromEntity(OrderDetail orderDetail){
        OrderDetailResDto orderDetailResDto = OrderDetailResDto.builder()
                .detailId(orderDetail.getId())
                .productName(orderDetail.getProduct().getName())
                .productCount(orderDetail.getQuantity())
                .build();
        return orderDetailResDto;
    }
}
