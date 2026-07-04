package com.deevyanshu.red.Controller;

import com.deevyanshu.red.Dao.UserDao;
import com.deevyanshu.red.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("users")
public class UserController {

    @Autowired
    private UserDao userDao;

    /**
     * Create a new user
     * POST: /users/add
     */
    @PostMapping("/add")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        user.setUserId(UUID.randomUUID().toString());
        User savedUser = userDao.saveUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    /**
     * Get user by ID
     * GET: /users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable String userId) {
        User user = userDao.getUserById(userId);
        if (user != null) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /**
     * Get all users
     * GET: /users
     */
    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userDao.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Update an existing user
     * PUT: /users/{userId}
     */
    @PutMapping("/update/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable String userId, @RequestBody User user) {
        User existingUser = userDao.getUserById(userId);
        if (existingUser != null) {
            user.setUserId(userId);
            User updatedUser = userDao.saveUser(user);
            return ResponseEntity.ok(updatedUser);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /**
     * Delete user by ID
     * DELETE: /users/{userId}
     */
    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<Void> deleteUserById(@PathVariable String userId) {
        User user = userDao.getUserById(userId);
        if (user != null) {
            userDao.deleteUserById(userId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
