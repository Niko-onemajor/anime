package com.example.anime.service;

import com.example.anime.model.User;
import com.example.anime.model.CommentInteraction;

import com.example.anime.model.Post;
import com.example.anime.model.Comment;
import com.example.anime.model.ForumCommentInteraction;
import com.example.anime.repository.UserRepository;
import com.example.anime.repository.PostRepository;
import com.example.anime.repository.ForumCommentInteractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private WatchHistoryService watchHistoryService;
    
    @Autowired
    private FavoriteService favoriteService;
    
    @Autowired
    private AnimeRatingService animeRatingService;
    
    @Autowired
    private AnimeCommentService animeCommentService;
    
    @Autowired
    private CommentInteractionService commentInteractionService;
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private ForumCommentInteractionRepository forumCommentInteractionRepository;
    
    @Autowired
    private PostRepository postRepository;

    // 注册用户
    public User register(String username, String password) {
        // 验证用户名格式
        if (username == null || username.trim().length() < 3 || username.trim().length() > 10) {
            throw new IllegalArgumentException("用户名长度必须在3-10个字符之间");
        }
        if (username.contains(" ")) {
            throw new IllegalArgumentException("用户名不能包含空格");
        }
        // 检查特殊字符（只允许字母、数字、下划线）
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("用户名只能包含字母、数字和下划线");
        }
        
        // 验证密码格式
        validatePasswordComplexity(password);

        // 检查用户名是否已存在（只检查未删除的用户，区分大小写）
        // 先查询所有未删除的用户
        List<User> users = userRepository.findByDeletedFalse();
        // 遍历检查是否有大小写不同但实际相同的用户名
        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                throw new IllegalArgumentException("用户名已存在");
            }
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodePassword(password)); // 加密存储密码
        user.setRole("user"); // 默认普通用户
        user.setDeleted(false); // 明确设置为未删除
        user.setActivated(false); // 默认未激活
        user.setActivationCode(generateActivationCode()); // 生成激活码

        User savedUser = userRepository.save(user);
        
        // 发送激活邮件
        sendActivationEmail("test@example.com", username, user.getActivationCode()); // 实际项目中应该从请求中获取邮箱
        
        return savedUser;
    }

    // 根据用户名查找用户（区分大小写）
    public User findByUsername(String username) {
        // 先使用数据库查询（可能不区分大小写）
        User user = userRepository.findByUsernameAndDeletedFalse(username);
        // 再在应用层进行大小写检查
        if (user != null && !user.getUsername().equals(username)) {
            return null;
        }
        return user;
    }

    // 验证密码
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        System.out.println("=== 密码验证 ===");
        System.out.println("明文密码: " + rawPassword);
        System.out.println("加密密码: " + encodedPassword);
        System.out.println("加密密码长度: " + encodedPassword.length());
        System.out.println("加密密码是否以$2开头: " + encodedPassword.startsWith("$2"));
        
        // 首先尝试直接比较明文密码
        if (rawPassword.equals(encodedPassword)) {
            System.out.println("明文比较结果: true");
            return true;
        }
        
        // 检查encodedPassword是否是BCrypt加密的密码（以$2开头）
        if (encodedPassword.startsWith("$2")) {
            // 使用BCrypt验证密码
            try {
                System.out.println("尝试使用BCrypt验证密码");
                boolean result = passwordEncoder.matches(rawPassword, encodedPassword);
                System.out.println("BCrypt验证结果: " + result);
                return result;
            } catch (Exception e) {
                System.out.println("BCrypt验证异常: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        } else {
            // 对于其他格式的密码，直接比较
            boolean result = rawPassword.equals(encodedPassword);
            System.out.println("其他格式密码比较结果: " + result);
            return result;
        }
    }

    // 更新用户资料
    public User update(String oldUsername, String newUsername, String email, String birthday, String favorite, String gender, String region, String signature) {
        System.out.println("=== 更新用户资料 ===");
        System.out.println("旧用户名: " + oldUsername);
        System.out.println("新用户名: " + newUsername);
        // 查找用户（区分大小写）
        User user = userRepository.findByUsernameAndDeletedFalse(oldUsername);
        System.out.println("查找结果: " + (user != null ? user.getUsername() : "null"));
        if (user == null) {
            return null;
        }

        // 检查新用户名是否已被其他用户使用（区分大小写）
        if (!oldUsername.equals(newUsername)) {
            User existingUser = userRepository.findByUsername(newUsername);
            System.out.println("新用户名检查结果: " + (existingUser != null ? existingUser.getUsername() : "null"));
            // 只有当存在其他用户使用该用户名时，才抛出异常
            if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                throw new IllegalArgumentException("用户名已存在，请换一个名字试试");
            }
        }

        // 更新用户资料
        user.setUsername(newUsername);
        user.setEmail(email);
        if (birthday != null && !birthday.isEmpty()) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                user.setBirthday(sdf.parse(birthday));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        user.setFavorite(favorite);
        user.setGender(gender);
        user.setRegion(region);
        user.setSignature(signature);

        User savedUser = userRepository.save(user);
        System.out.println("保存结果: " + savedUser.getUsername());
        return savedUser;
    }

    // 更新用户头像
    public User updateAvatar(String username, String avatar) {
        // 查找用户（区分大小写）
        User user = userRepository.findByUsernameAndDeletedFalse(username);
        if (user == null) {
            return null;
        }

        // 更新用户头像
        user.setAvatar(avatar);

        return userRepository.save(user);
    }

    // 保存用户
    public User save(User user) {
        return userRepository.save(user);
    }

    // 根据ID查找用户
    public User findById(Long id) {
        return userRepository.findByIdAndDeletedFalse(id);
    }

    // 获取所有用户（非删除）
    public List<User> findAll() {
        return userRepository.findByDeletedFalse();
    }

    // 根据用户名搜索用户（非删除，区分大小写）
    public List<User> findByUsernameContaining(String keyword) {
        return userRepository.findByUsernameContainingAndDeletedFalse(keyword);
    }
    
    // 分页查询所有未删除的用户
    public Page<User> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size); // 页面从1开始，PageRequest从0开始
        return userRepository.findByDeletedFalse(pageable);
    }
    
    // 分页查询根据用户名搜索的未删除用户
    public Page<User> findByUsernameContaining(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size); // 页面从1开始，PageRequest从0开始
        return userRepository.findByUsernameContainingAndDeletedFalse(keyword, pageable);
    }

    // 根据ID删除用户（逻辑删除）
    public void deleteById(Long id) {
        User user = userRepository.findByIdAndDeletedFalse(id);
        if (user != null) {
            user.setDeleted(true);
            user.setDeletedAt(new java.util.Date());
            userRepository.save(user);
        }
    }

    // 密码加密
    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }
    
    // 密码复杂度校验
    public void validatePasswordComplexity(String password) {
        // 长度校验
        if (password == null || password.length() < 8 || password.length() > 20) {
            throw new IllegalArgumentException("密码长度必须在8-20个字符之间");
        }
        if (password.contains(" ")) {
            throw new IllegalArgumentException("密码不能包含空格");
        }
        
        // 弱密码黑名单
        String[] weakPasswords = {
            "12345678", "123456789", "1234567890", "password", "Password",
            "qwertyui", "asdfghjkl", "zxcvbnm", "11111111", "00000000",
            "admin123", "admin1234", "user1234", "test1234"
        };
        for (String weak : weakPasswords) {
            if (password.equalsIgnoreCase(weak)) {
                throw new IllegalArgumentException("密码过于简单，请使用更复杂的密码");
            }
        }
        
        // 复杂度：必须包含大写字母、小写字母、数字中的至少两种
        boolean hasUpper = false, hasLower = false, hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        int categoryCount = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + (hasDigit ? 1 : 0);
        if (categoryCount < 2) {
            throw new IllegalArgumentException("密码必须包含大写字母、小写字母、数字中的至少两种");
        }
    }
    
    // 生成激活码
    private String generateActivationCode() {
        return java.util.UUID.randomUUID().toString();
    }
    
    // 发送激活邮件
    private void sendActivationEmail(String email, String username, String activationCode) {
        // 这里实现发送邮件的逻辑
        // 实际项目中需要配置邮件服务器
        System.out.println("发送激活邮件到: " + email);
        System.out.println("激活链接: http://localhost:8080/api/user/activate?code=" + activationCode + "&username=" + username);
    }
    
    // 激活用户
    public boolean activateUser(String activationCode) {
        User user = userRepository.findByActivationCode(activationCode);
        if (user != null && !user.getActivated()) {
            user.setActivated(true);
            user.setActivationCode(null); // 激活后清空激活码
            userRepository.save(user);
            return true;
        }
        return false;
    }
    
    // 获取所有已删除的用户
    public List<User> findAllDeleted() {
        return userRepository.findByDeletedTrue();
    }
    
    // 恢复已删除的用户
    public void restore(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setDeleted(false);
            user.setDeletedAt(null);
            userRepository.save(user);
        }
    }
    
    // 删除用户（逻辑删除，同时删除用户的相关数据）
    @javax.transaction.Transactional
    public void deleteUser(Long id) {
        try {
            System.out.println("开始删除用户，ID: " + id);
            
            // 1. 删除用户的观看记录
            System.out.println("删除用户观看记录...");
            watchHistoryService.deleteByUserId(id);
            
            // 2. 删除用户的收藏
            System.out.println("删除用户收藏...");
            favoriteService.deleteByUserId(id);
            
            // 3. 删除用户的评分
            System.out.println("删除用户评分...");
            animeRatingService.deleteByUserId(id);
            
            // 4. 删除用户的评论互动记录
            System.out.println("删除用户评论互动记录...");
            List<CommentInteraction> commentInteractions = commentInteractionService.getByUserId(id);
            for (CommentInteraction interaction : commentInteractions) {
                commentInteractionService.deleteById(interaction.getId());
            }
            
            // 5. 删除用户的论坛评论互动记录
            System.out.println("删除用户论坛评论互动记录...");
            List<ForumCommentInteraction> forumCommentInteractions = forumCommentInteractionRepository.findAll().stream()
                    .filter(interaction -> interaction.getUserId().equals(id))
                    .collect(java.util.stream.Collectors.toList());
            for (ForumCommentInteraction interaction : forumCommentInteractions) {
                forumCommentInteractionRepository.delete(interaction);
            }
            
            // 6. 删除用户的动漫评论
            System.out.println("删除用户动漫评论...");
            animeCommentService.deleteByAuthorId(id);
            
            // 7. 删除用户的帖子
            System.out.println("删除用户帖子...");
            List<Post> userPosts = postRepository.findByAuthorId(id);
            for (Post post : userPosts) {
                // 先删除帖子的所有评论
                List<Comment> comments = commentService.getCommentsByPostId(post.getId());
                for (Comment comment : comments) {
                    commentService.deleteById(comment.getId());
                }
                // 再删除帖子
                postRepository.deleteById(post.getId());
            }
            
            // 8. 最后逻辑删除用户
            System.out.println("逻辑删除用户本身...");
            deleteById(id);
            System.out.println("用户删除成功，ID: " + id);
        } catch (Exception e) {
            System.out.println("删除用户失败: " + e.getMessage());
            e.printStackTrace();
            throw e; // 重新抛出异常，让调用方知道删除失败
        }
    }
    
    // 彻底删除用户
    @javax.transaction.Transactional
    public void hardDelete(Long id) {
        try {
            System.out.println("开始彻底删除用户，ID: " + id);
            
            // 1. 删除用户的观看记录
            System.out.println("删除用户观看记录...");
            watchHistoryService.deleteByUserId(id);
            
            // 2. 删除用户的收藏
            System.out.println("删除用户收藏...");
            favoriteService.deleteByUserId(id);
            
            // 3. 删除用户的评分
            System.out.println("删除用户评分...");
            animeRatingService.deleteByUserId(id);
            
            // 4. 删除用户的评论互动记录
            System.out.println("删除用户评论互动记录...");
            List<CommentInteraction> commentInteractions = commentInteractionService.getByUserId(id);
            for (CommentInteraction interaction : commentInteractions) {
                commentInteractionService.deleteById(interaction.getId());
            }
            
            // 5. 删除用户的论坛评论互动记录
            System.out.println("删除用户论坛评论互动记录...");
            List<ForumCommentInteraction> forumCommentInteractions = forumCommentInteractionRepository.findAll().stream()
                    .filter(interaction -> interaction.getUserId().equals(id))
                    .collect(java.util.stream.Collectors.toList());
            for (ForumCommentInteraction interaction : forumCommentInteractions) {
                forumCommentInteractionRepository.delete(interaction);
            }
            
            // 6. 删除用户的动漫评论
            System.out.println("删除用户动漫评论...");
            animeCommentService.deleteByAuthorId(id);
            
            // 7. 删除用户的帖子
            System.out.println("删除用户帖子...");
            List<Post> userPosts = postRepository.findByAuthorId(id);
            for (Post post : userPosts) {
                // 先删除帖子的所有评论
                List<Comment> comments = commentService.getCommentsByPostId(post.getId());
                for (Comment comment : comments) {
                    commentService.deleteById(comment.getId());
                }
                // 再删除帖子
                postRepository.deleteById(post.getId());
            }
            
            // 8. 删除用户的论坛评论（直接由用户发布的评论，不是帖子的评论）
            System.out.println("删除用户的论坛评论...");
            List<Comment> userComments = commentService.getCommentsByAuthorId(id);
            for (Comment comment : userComments) {
                commentService.deleteById(comment.getId());
            }
            
            // 9. 最后删除用户本身
            System.out.println("删除用户本身...");
            userRepository.deleteById(id);
            System.out.println("用户彻底删除成功，ID: " + id);
        } catch (Exception e) {
            System.out.println("彻底删除用户失败: " + e.getMessage());
            e.printStackTrace();
            throw e; // 重新抛出异常，让调用方知道删除失败
        }
    }
}
