package com.globalmobilityapex.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class WorkerApplication {

    public static void main(String[] args) {
        log.info("Starting Worker Order Processing Service...");
        SpringApplication.run(WorkerApplication.class, args);
        log.info("Worker Order Processing Service started successfully");
    }
}