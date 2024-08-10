package com.example.webservicerestfulapi.entity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MongoUserRepository extends MongoRepository<UserMongodb, String> {
    Optional<UserMongodb> findById(String id);
}
