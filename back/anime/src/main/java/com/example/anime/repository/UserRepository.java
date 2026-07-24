package com.example.anime.repository;

import com.example.anime.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    List<User> findByUsernameContaining(String keyword);
    List<User> findByDeletedFalse();
    List<User> findByDeletedTrue();
    User findByIdAndDeletedFalse(Long id);
    User findByUsernameAndDeletedFalse(String username);
    List<User> findByUsernameContainingAndDeletedFalse(String keyword);
    // 大小写不敏感的查询方法
    User findByUsernameIgnoreCase(String username);
    User findByUsernameIgnoreCaseAndDeletedFalse(String username);
    List<User> findByUsernameContainingIgnoreCase(String keyword);
    List<User> findByUsernameContainingIgnoreCaseAndDeletedFalse(String keyword);
    User findByActivationCode(String activationCode);
    // 分页查询所有未删除的用户
    Page<User> findByDeletedFalse(Pageable pageable);
    // 分页查询根据用户名搜索的未删除用户
    Page<User> findByUsernameContainingAndDeletedFalse(String keyword, Pageable pageable);
}
