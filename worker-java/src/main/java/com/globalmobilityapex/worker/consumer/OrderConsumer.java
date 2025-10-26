package com.globalmobilityapex.worker.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalmobilityapex.worker.model.OrderMessage;
import com.globalmobilityapex.worker.service.OrderProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderProcessingService orderProcessingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "${kafka.topics.orders}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrder(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        long startTime = System.currentTimeMillis();
        
        log.info("Received message - Partition: {}, Offset: {}", partition, offset);
        log.info("Raw message: {}", message);

        try {
            OrderMessage orderMessage = parseMessage(message);
            
            log.info("Order ID: {}, Customer ID: {}, Products: {}", 
                orderMessage.getOrderId(), 
                orderMessage.getCustomerId(), 
                orderMessage.getProductIds().size());

            orderProcessingService.processOrder(orderMessage)
                    .doOnSuccess(order -> {
                        long duration = System.currentTimeMillis() - startTime;
                        
                        log.info("Order processed successfully: {} (Total: ${}, Time: {}ms)", 
                            order.getOrderId(), order.getTotalAmount(), duration);
                        
                        if (acknowledgment != null) {
                            acknowledgment.acknowledge();
                            log.info("Message acknowledged (offset: {})", offset);
                        }
                    })
                    .doOnError(error -> {
                        long duration = System.currentTimeMillis() - startTime;
                        
                        log.error("Order processing failed: {} - {} (Time: {}ms)", 
                            orderMessage.getOrderId(), error.getMessage(), duration);
                        
                        log.warn("Message NOT acknowledged - will be reprocessed");
                    })
                    .subscribe();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            log.error("Critical error processing message - Partition: {}, Offset: {} (Time: {}ms)", 
                partition, offset, duration);
            log.error("Error: {}", e.getMessage(), e);
            
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                log.warn("Message acknowledged despite error");
            }
        }
    }

    private OrderMessage parseMessage(String message) {
        try {
            return objectMapper.readValue(message, OrderMessage.class);
        } catch (Exception e) {
            log.error("Failed to parse message: {}", message);
            throw new RuntimeException("Invalid message format", e);
        }
    }
}