package com.globalmobilityapex.worker.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Slf4j
@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.globalmobilityapex.worker.repository")
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Override
    protected String getDatabaseName() {
        String dbName = mongoUri.substring(mongoUri.lastIndexOf("/") + 1);
        if (dbName.contains("?")) {
            dbName = dbName.substring(0, dbName.indexOf("?"));
        }
        log.info("MongoDB Database: {}", dbName);
        return dbName;
    }

    @Bean
    @Override
    public MongoClient reactiveMongoClient() {
        log.info("Initializing Reactive MongoDB Client");
        log.info("MongoDB URI: {}", maskPassword(mongoUri));
        
        MongoClient client = MongoClients.create(mongoUri);
        
        log.info("Reactive MongoDB Client created successfully");
        return client;
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        log.info("Creating ReactiveMongoTemplate");
        return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
    }

    private String maskPassword(String uri) {
        if (uri.contains("@")) {
            String[] parts = uri.split("@");
            if (parts[0].contains(":")) {
                String[] credentials = parts[0].split("://");
                if (credentials.length > 1 && credentials[1].contains(":")) {
                    return credentials[0] + "://***:***@" + parts[1];
                }
            }
        }
        return uri;
    }
}