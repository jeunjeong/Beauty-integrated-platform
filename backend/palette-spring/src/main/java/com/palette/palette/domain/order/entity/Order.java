package com.palette.palette.domain.order.entity;


import com.palette.palette.domain.delivery.entity.Delivery;
import com.palette.palette.domain.feed.dto.BaseUserResDto;
import com.palette.palette.domain.order.dto.create.OrderReqDto;
import com.palette.palette.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    @Column(nullable = false)
    private Integer price = 0;

    private String requirement;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    private LocalDateTime orderAt;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;


    /**
     * dto -> entity
     */
    public static Order toEntity(OrderReqDto orderReqDto, User user) {

        return Order.builder()
                .requirement(orderReqDto.getRequirement())
                .orderStatus(OrderStatus.ORDER)
                .orderAt(LocalDateTime.now())
                .user(user)
                .build();


    }

}
