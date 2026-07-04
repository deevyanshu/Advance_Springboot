package com.deevyanshu.red.Dao;

import com.deevyanshu.red.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class UserDao {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String KEY="User";

    public User saveUser(User user) {
        // Consider Key as the table name and userId as the primary key for the table. The user object is the value that we are storing in Redis.
        redisTemplate.opsForHash().put(KEY, user.getUserId(), user);
        return user;
    }

    public User getUserById(String userId) {
        return (User) redisTemplate.opsForHash().get(KEY, userId);
    }

    public List<User> getAllUsers() {
        return redisTemplate.opsForHash().entries(KEY).values().stream().map(obj -> (User) obj).collect(Collectors.toList());
    }

    public void deleteUserById(String userId) {
        redisTemplate.opsForHash().delete(KEY, userId);
    }


}
