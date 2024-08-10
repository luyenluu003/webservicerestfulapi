package com.example.webservicerestfulapi.controller;

import com.example.webservicerestfulapi.entity.User;
import com.example.webservicerestfulapi.exception.CustomServiceException;
import com.example.webservicerestfulapi.publisher.RabbitMQJsonUser;
import com.example.webservicerestfulapi.publisher.RabbitMQUser;
import com.example.webservicerestfulapi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private RabbitMQUser rabbitMQUser;

    @Autowired
    RabbitMQJsonUser rabbitMQJsonUser;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @GetMapping("/users")
    public ResponseEntity<List<User>> showUserList() {
        LOGGER.info("Request to list all users received");
        try{
            List<User> users = userService.listAll();
            if(users == null ||users.isEmpty()){
                LOGGER.info("No users found");
                return ResponseEntity.noContent().build();
            }
            LOGGER.info("Returning list of users: {}", users);
            return ResponseEntity.ok(users);
        }catch (Exception e){
            LOGGER.error("Error retrieving users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/users/addnewuser")
    public ResponseEntity<String> addNewUser(@RequestBody User user){
        LOGGER.info("Request to add new user: {}", user);
        try{
            userService.addUser(user);
            rabbitMQJsonUser.sendJsonMessage(user);
            LOGGER.info("User added successfully: {}",user);
            return ResponseEntity.ok("User added successfully");
        } catch (CustomServiceException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error while adding user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
    }

    @GetMapping("/users/edit/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Integer id){
        LOGGER.info("Request to get user by id: {}", id);
        try{
            Optional<User> user = userService.findById(id);
            if(user.isPresent()){
                LOGGER.info("User found: {}", user.get());
                return ResponseEntity.ok(user.get());
            }
            LOGGER.warn("User not found with ID: {}", id);
            throw new CustomServiceException("User not found with ID: " + id);
        }catch (CustomServiceException e){
            LOGGER.error("CustomServiceException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PutMapping("/users/edit/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody User userDetail){
        LOGGER.info("Request to update user with ID: {} and details: {}", id, userDetail);
        try{
            User updatedUser = userService.updateUser(id, userDetail);
            rabbitMQJsonUser.sendJsonMessage(updatedUser);
            LOGGER.info("User updated successfully: {}", updatedUser);
            return ResponseEntity.ok(updatedUser);
        }catch (CustomServiceException e){
            LOGGER.error("CustomServiceException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }catch (Exception e){
            LOGGER.error("Unexpected error while updating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
        }
    }

    @DeleteMapping("/users/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id){
        LOGGER.info("Request to delete user with ID: {}", id);
        try{
            userService.deleteById(id);
            rabbitMQUser.sendMessage("User with ID " + id + " deleted");
            LOGGER.info("User deleted successfully: {}", id);
            return ResponseEntity.ok("User deleted successfully");
        }catch (CustomServiceException e){
            LOGGER.error("CustomServiceException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/users/cached/{id}")
    public ResponseEntity<User> getUserByIdWithCache(@PathVariable Integer id){
        LOGGER.info("Request to get user by id with cache: {}", id);
        try{
            User user = userService.findUserByIdWithCache(id);
            if(user != null){
                LOGGER.info("User found: {}", user);
                return ResponseEntity.ok(user);
            }
            LOGGER.warn("User not found with ID: {}", id);
            throw new CustomServiceException("User not found with ID: " + id);
        }catch (CustomServiceException e){
            LOGGER.error("CustomServiceException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
