package com.sumanth.Apple.repository;

import com.sumanth.Apple.models.User;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

//     @Query("from users where id like '8'")
     List<User> findAll();
     User findByEmail(String email);
     User findByUsernameAndPassword(String username, String password);
     User findByResetToken(String resetToken);
}
