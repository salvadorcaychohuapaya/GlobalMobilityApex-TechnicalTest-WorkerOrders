package com.globalmobilityapex.worker.repository;

import com.globalmobilityapex.worker.model.Order;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRepository extends ReactiveMongoRepository<Order, String> {
    
    Mono<Order> findByOrderId(String orderId);
}