package com.chams.orderservice.service;

import com.chams.orderservice.dto.InventoryResponse;
import com.chams.orderservice.dto.OrderLineItemsDto;
import com.chams.orderservice.dto.OrderRequest;
import com.chams.orderservice.event.OrderPlaceEvent;
import com.chams.orderservice.model.Order;
import com.chams.orderservice.model.OrderLineItems;
import com.chams.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webclientbuilder;
    private final KafkaTemplate<String,OrderPlaceEvent> kafkaTemplate;

    public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDto()
                .stream()
                .map(this::mapToDto).collect(Collectors.toList());
        order.setOrderLineItems(orderLineItems);

        List<String> skuCodes = order.getOrderLineItems().stream()
                .map(OrderLineItems::getSkuCode)
                .collect(Collectors.toList());

        //inventory call to check if product in stock
        InventoryResponse[] result = webclientbuilder.build().get().uri("http://inventory-service/api/inventory", uriBuilder -> uriBuilder.queryParam("skucode",skuCodes).build())
                        .retrieve()
                        .bodyToMono(InventoryResponse[].class)
                        .block();

        boolean allProductInStock = Arrays.stream(result).allMatch(InventoryResponse::isInStock);

        if(allProductInStock){
            orderRepository.save(order);
            kafkaTemplate.send("notificationTopic",new OrderPlaceEvent(order.getOrderNumber(),order.getOrderLineItems().stream().map(OrderLineItems::getSkuCode).collect(Collectors.toList()).toString()));
        }else {
            throw new IllegalArgumentException("Product is not in stock please try later");
        }


    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLine) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLine.getPrice());
        orderLineItems.setQuantity(orderLine.getQuantity());
        orderLineItems.setSkuCode(orderLine.getSkuCode());
        return orderLineItems;

    }
}
