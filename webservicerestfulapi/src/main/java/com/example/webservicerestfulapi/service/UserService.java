package com.example.webservicerestfulapi.service;

import com.example.webservicerestfulapi.entity.MongoUserRepository;
import com.example.webservicerestfulapi.entity.User;
import com.example.webservicerestfulapi.entity.UserMongodb;
import com.example.webservicerestfulapi.entity.UserRepository;
import com.example.webservicerestfulapi.exception.CustomServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public static final String HASH_KEY = "User";
    @Autowired
    private RedisTemplate<String, Object> template;
    @Autowired
    private MongoUserRepository mongoUserRepository;


    public List<User> listAll() {
        return (List<User>) userRepository.findAll();
    }

    private void validateUser(User user) {
        if(user.getFullName()==null || user.getFullName().isEmpty()){
            throw new CustomServiceException("User name cannot be null or empty");
        }
        if(user.getBirthday()==null){
            throw new CustomServiceException("User birthday cannot be null");
        }
        if(user.getClassName()==null || user.getClassName().isEmpty()){
            throw new CustomServiceException("User class name cannot be null or empty");
        }
        if(user.getHomeTown()==null || user.getHomeTown().isEmpty()){
            throw new CustomServiceException("User home town cannot be null or empty");
        }
        if(user.getGender()==null || user.getGender().isEmpty()){
            throw new CustomServiceException("User gender cannot be null or empty");
        }
    }

    public void addUser(User user) {
        validateUser(user);
        try{
            User savedUser = userRepository.save(user);
            // Lưu vào MongoDB
            UserMongodb userMongo = new UserMongodb();
            userMongo.setId(String.valueOf(savedUser.getId()));
            userMongo.setFullName(user.getFullName());
            userMongo.setBirthday(user.getBirthday());
            userMongo.setClassName(user.getClassName());
            userMongo.setHomeTown(user.getHomeTown());
            userMongo.setGender(user.getGender());
            System.out.println("UserMongo: " + userMongo);
            mongoUserRepository.save(userMongo);
            if (template.hasKey(HASH_KEY)) {
                template.delete(HASH_KEY);
            }

            // Lưu vào Redis với Hash
            template.opsForHash().put(HASH_KEY, String.valueOf(user.getId()), user);
        }catch(DataIntegrityViolationException e){
            throw new CustomServiceException("Data integrity violation: " + e.getMessage(), e);
        }catch (Exception e){
            throw new CustomServiceException("An error occurred while saving the user: " + e.getMessage(), e);
        }
    }

    public Optional<User> findById(Integer id) {
        User user = (User) template.opsForHash().get(HASH_KEY, String.valueOf(id));
        if (user != null) {
            return Optional.of(user);
        }
        Optional<User> optionalUser = userRepository.findById(id);
        optionalUser.ifPresent(u -> template.opsForHash().put(HASH_KEY, String.valueOf(u.getId()), u));
        return optionalUser;
    }
    public Optional<UserMongodb> findUserByIdInMongo(Integer id) {
        String idString = String.valueOf(id);
        Optional<UserMongodb> optionalMongoUser = mongoUserRepository.findById(idString);
        System.out.println("mongoUser: " + optionalMongoUser);
        if (!optionalMongoUser.isPresent()) {
            throw new CustomServiceException("User not found in MongoDB");
        }
        return optionalMongoUser;
    }

    public User updateUser(Integer id,User userDetail) {
        Optional<User> optionalUser = userRepository.findById(id);
        if(!optionalUser.isPresent()) {
            throw new CustomServiceException("User not found");
        }
        validateUser(userDetail);

        User user = optionalUser.get();
        user.setFullName(userDetail.getFullName());
        user.setGender(userDetail.getGender());
        user.setHomeTown(userDetail.getHomeTown());
        user.setBirthday(userDetail.getBirthday());
        user.setClassName(userDetail.getClassName());
        User updatedUser = userRepository.save(user);

        Optional<UserMongodb> optionalMongoUser = findUserByIdInMongo(id);

        if (!optionalMongoUser.isPresent()) {
            throw new CustomServiceException("User not found in MongoDB");
        }
        // Cập nhật thông tin người dùng trong MongoDB
        UserMongodb mongoUser = optionalMongoUser.get();
        mongoUser.setFullName(userDetail.getFullName());
        mongoUser.setBirthday(userDetail.getBirthday());
        mongoUser.setClassName(userDetail.getClassName());
        mongoUser.setHomeTown(userDetail.getHomeTown());
        mongoUser.setGender(userDetail.getGender());
        mongoUserRepository.save(mongoUser);

        // Cập nhật cache
        template.opsForHash().put(HASH_KEY, String.valueOf(updatedUser.getId()), updatedUser);
        return updatedUser;
    }

    public void deleteById(Integer id) {
        Long count = userRepository.countById(id);
        if (count == null || count == 0) {
            throw new CustomServiceException("User not found");
        }
        userRepository.deleteById(id);

        String idString = String.valueOf(id);
        if (!mongoUserRepository.existsById(idString)) {
            throw new CustomServiceException("User not found in MongoDB");
        }

        mongoUserRepository.deleteById(idString);
        template.opsForHash().delete(HASH_KEY, idString);
    }
    public User findUserByIdWithCache(Integer id) {
        String cacheKey = "user_" + id;
        User user = (User) template.opsForValue().get(cacheKey);
        if (user == null) {
            Optional<User> optionalUser = userRepository.findById(id);
            if (optionalUser.isPresent()) {
                user = optionalUser.get();
                template.opsForValue().set(cacheKey, user, 10, TimeUnit.MINUTES);
            }
        }
        return user;
    }
}
