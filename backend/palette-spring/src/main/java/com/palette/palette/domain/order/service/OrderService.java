package com.palette.palette.domain.order.service;


import com.palette.palette.domain.delivery.dto.DeliveryResDto;
import com.palette.palette.domain.delivery.entity.Delivery;
import com.palette.palette.domain.delivery.entity.DeliveryStatus;
import com.palette.palette.domain.delivery.repository.DeliveryRepository;
import com.palette.palette.domain.feed.dto.BaseUserResDto;
import com.palette.palette.domain.item.entity.Item;
import com.palette.palette.domain.item.repository.ItemRepository;
import com.palette.palette.domain.itemOption.entity.ItemOption;
import com.palette.palette.domain.itemOption.repository.ItemOptionRepository;
import com.palette.palette.domain.order.dto.create.OrderCreateReqDto;
import com.palette.palette.domain.order.dto.detail.OrderDetailResDto;
import com.palette.palette.domain.order.dto.list.OrderListResDto;
import com.palette.palette.domain.orderItem.dto.OrderItemDto;
import com.palette.palette.domain.orderItem.dto.OrderItemOptionDto;
import com.palette.palette.domain.order.entity.Order;
import com.palette.palette.domain.orderItem.dto.detail.OrderItemDetailResDto;
import com.palette.palette.domain.orderItem.dto.detail.OrderItemOptionDetailResDto;
import com.palette.palette.domain.orderItem.entity.OrderItem;
import com.palette.palette.domain.order.entity.OrderStatus;
import com.palette.palette.domain.order.repository.OrderRepository;
import com.palette.palette.domain.orderItem.entity.OrderItemOption;
import com.palette.palette.domain.orderItem.respository.OrderItemOptionRepository;
import com.palette.palette.domain.orderItem.respository.OrderItemRepository;
import com.palette.palette.domain.user.entity.User;
import com.palette.palette.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final ItemRepository itemRepository;
    private final ItemOptionRepository itemOptionRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemOptionRepository orderItemOptionRepository;
    private final UserRepository userRepository;


    /**
     * 주문 생성
     */
    @Transactional
    public void orderCreate(OrderCreateReqDto orderCreateReqDto, User user) {

        /////// 주문
        // Order 생성
        Order order = Order.builder()
                .user(user)
                .requirement(orderCreateReqDto.getRequirement())
                .orderStatus(OrderStatus.ORDER)
                .orderAt(LocalDateTime.now())
                .build();

        // 주문 총 가격, 개수
        Integer totalPrice = 0;
        Integer totalCount = 0;

        // 각 OrderItem 생성
        for (OrderItemDto orderItemDto : orderCreateReqDto.getItems()) {

            // DB의 Item 가져오기
            Item item = itemRepository.findById(orderItemDto.getItemId())
                            .orElseThrow(() -> new IllegalArgumentException("아이템을 찾지 못했습니다. Item ID: " + orderItemDto.getItemId()));


            OrderItem orderItem = OrderItem.builder()
                    .item(item)
                    .order(order)
                    .build();


            Integer itemPrice = 0;
            Integer itemCount = 0;


            for (OrderItemOptionDto orderItemOptionDto : orderItemDto.getItemOptions()) {

                // DB에 ItemOption 가져오기
                ItemOption itemOption = itemOptionRepository.findById(orderItemOptionDto.getItemOptionId())
                        .orElseThrow(() -> new IllegalArgumentException("아이템을 찾지 못했습니다. Item ID: " + orderItemOptionDto.getItemOptionId()));

                // 주문 총 가격 합, 개수 계산
                totalPrice += itemOption.getItem().getPrice() * orderItemOptionDto.getItemOptionCount();
                totalCount += orderItemOptionDto.getItemOptionCount();

                itemCount += orderItemOptionDto.getItemOptionCount();
                itemPrice += orderItemOptionDto.getItemOptionCount() * item.getPrice();

                OrderItemOption orderItemOption = OrderItemOption.builder()
                        .itemOption(itemOption)
                        .orderItem(orderItem)
                        .orderCount(orderItemOptionDto.getItemOptionCount())
                        .orderPrice(orderItemOptionDto.getItemOptionCount() * item.getPrice())
                        .build();



//                OrderItem orderItem = OrderItem.builder()
//                        .item(item)
//                        .itemOption(itemOption)
//                        .order(order)
//                        .orderCount(orderItemOptionDto.getItemOptionCount())
//                        .orderPrice(itemOption.getItem().getPrice() * orderItemOptionDto.getItemOptionCount())
//                        .build();

                orderItem.setOrderCount(orderItemOptionDto.getItemOptionCount());
                orderItem.setOrderPrice(itemOption.getItem().getPrice() * orderItemOptionDto.getItemOptionCount());

                // 아이템 옵션의 재고 감소
                orderItemOption.getItemOption().removeStock(orderItemOptionDto.getItemOptionCount());
//                orderItemRepository.save(orderItem);
                orderItemOptionRepository.save(orderItemOption);

            }

            orderItemRepository.save(orderItem);

            totalPrice += item.getDeliveryFee();
        }

        order.setPrice(totalPrice);
        order.setCount(totalCount);

        ////// 배송
        Delivery delivery = Delivery.builder()
                .order(order)
                .zipcode(orderCreateReqDto.getAddress().getZipcode())
                .address(orderCreateReqDto.getAddress().getAddress())
                .receiver(orderCreateReqDto.getAddress().getReceiver())
                .phone(orderCreateReqDto.getAddress().getPhone())
                .deliveryStatus(DeliveryStatus.READY)
                .build();


        order.setDelivery(delivery);
        orderRepository.save(order);

        /**
         * -> order 랑 delivery 를 따로 save() 하고 있지만,
         *    양방향 매핑이랑 cascade를 통해 좀 더 효율적으로 개선시킬 수 있을것 같음.
         */
    }


    /**
     * 주문 목록 조회
     * 기간 또는 개수로 페이징 처리 해야할 필요가 있음.
     */
    public List<OrderListResDto> list(User user) {

        return orderRepository.findAllByUser(user).stream()
                .map(OrderListResDto::toDto)
                .collect(Collectors.toList());
    }


    /**
     * 주문 상세 조회
     * @param orderId
     */
    public OrderDetailResDto detail(Long orderId) {

        Order order = orderRepository.findById(orderId).get();
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

        List<OrderItemDetailResDto> orderItemDetailResDtoList = new ArrayList<>();

        for (OrderItem orderItem : orderItems) {
            List<OrderItemOption> orderItemOptions = orderItemOptionRepository.findByOrderItemId(orderItem.getId());

            List<OrderItemOptionDetailResDto> orderItemOptionDetailResDtoList = new ArrayList<>();

            for (OrderItemOption orderItemOption : orderItemOptions) {

                orderItemOptionDetailResDtoList.add(
                        OrderItemOptionDetailResDto.builder()
                                .itemOptionId(orderItemOption.getId())
                                .optionName(orderItemOption.getItemOption().getOptionName())
                                .orderCount(orderItemOption.getOrderCount())
                                .orderPrice(orderItemOption.getOrderPrice())
                                .build()
                );
            }

            orderItemDetailResDtoList.add(
                    OrderItemDetailResDto.builder()
                            .itemId(orderItem.getId())
                            .itemName(orderItem.getItem().getName())
                            .orderPrice(orderItem.getOrderPrice())
                            .orderCount(orderItem.getOrderCount())
                            .orderItemOptions(orderItemOptionDetailResDtoList)
                            .build()
            );

        }

        return OrderDetailResDto.builder()
                .id(order.getId())
                .user(BaseUserResDto.toDto(order.getUser()))
                .price(order.getPrice())
                .count(order.getCount())
                .requirement(order.getRequirement())
                .orderStatus(order.getOrderStatus())
                .orderAt(order.getOrderAt())
                .items(orderItemDetailResDtoList)
                .delivery(DeliveryResDto.toDto(order.getDelivery()))
                .build();
    }


    /**
     * 주문 취소
     * @param orderId
     * @param id
     */
    @Transactional
    public void cancel(Long orderId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다."));

        if (! order.getUser().getEmail().equals(user.getEmail())) {
            throw new IllegalArgumentException("해당 주문 건은 사용자의 주문이 아닙니다.");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        for (OrderItem orderItem : orderItems) {
            List<OrderItemOption> orderItemOptions = orderItemOptionRepository.findAllByOrderAndUser(orderItem.getId());
            order.cancel(orderItemOptions);
        }
//        List<OrderItemOption> orderItemOption  = orderItemOptionRepository.findAllByOrderitemAndUser(, user.getId());

    }
}
