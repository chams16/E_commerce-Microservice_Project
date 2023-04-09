package com.chams.orderservice.service;

import com.chams.orderservice.dto.OrderLineItemsDto;
import com.chams.orderservice.dto.OrderRequest;
import com.chams.orderservice.model.Order;
import com.chams.orderservice.model.OrderLineItems;
import com.chams.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDto()
                .stream()
                .map(this::mapToDto).collect(Collectors.toList());
        order.setOrderLineItems(orderLineItems);
        orderRepository.save(order);
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLine) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLine.getPrice());
        orderLineItems.setQuantity(orderLine.getQuantity());
        orderLineItems.setSkuCode(orderLine.getSkuCode());
        return orderLineItems;
    }
}
