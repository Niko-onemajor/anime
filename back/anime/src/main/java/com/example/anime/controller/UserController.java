package com.example.anime.controller;

import com.example.anime.model.User;
import com.example.anime.service.UserService;
import com.example.anime.utils.JwtUtils;
import com.example.anime.utils.OSSUtil;
import com.example.anime.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private OSSUtil ossUtil;

    // 登录接口
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        boolean rememberMe = request.get("rememberMe") != null && (boolean) request.get("rememberMe");

        log.debug("=== 登录请求 ===");
        log.debug("用户名: {}", username);
        log.debug("记住我: {}", rememberMe);

        // 查找用户
        User user = userService.findByUsername(username);
        log.debug("查找用户结果: {}", (user != null ? user.getUsername() : "null"));
        if (user == null) {
            response.put("code", 401);
            response.put("msg", "用户名或密码错误");
            return response;
        }

        // 验证密码
        boolean isValid = userService.validatePassword(password, user.getPassword());
        log.debug("密码验证结果: {}", isValid);
        if (!isValid) {
            response.put("code", 401);
            response.put("msg", "用户名或密码错误");
            return response;
        }

        // 如果密码是明文，更新为加密版本
        if (!user.getPassword().startsWith("$2")) {
            user.setPassword(userService.encodePassword(password));
            userService.save(user);
            log.info("密码已更新为加密版本");
        }

        // 生成令牌
        String accessToken = jwtUtils.generateAccessToken(username, rememberMe);
        String refreshToken = jwtUtils.generateRefreshToken(username, rememberMe);

        // 返回响应
        response.put("code", 200);
        response.put("msg", "登录成功");
        Map<String, Object> data = new HashMap<>();
        data.put("token", accessToken);
        data.put("refreshToken", refreshToken);
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("email", user.getEmail());
        data.put("birthday", user.getBirthday() != null ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(user.getBirthday()) : "");
        data.put("favorite", user.getFavorite());
        data.put("avatar", user.getAvatar());
        response.put("data", data);

        return response;
    }

    // 注册接口
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String username = request.get("username");
        String password = request.get("password");

        try {
            // 注册用户
            userService.register(username, password);
            
            // 返回响应
            response.put("code", 200);
            response.put("msg", "注册成功");
        } catch (IllegalArgumentException e) {
            // 捕获参数异常
            response.put("code", 400);
            response.put("msg", e.getMessage());
        } catch (Exception e) {
            // 捕获其他异常
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "注册失败: " + e.getMessage());
        }

        return response;
    }

    // 更新用户资料接口
    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String oldUsername = request.get("oldUsername");
        String newUsername = request.get("username");
        String email = request.get("email");
        String birthday = request.get("birthday");
        String favorite = request.get("favorite");
        String gender = request.get("gender");
        String region = request.get("region");
        String signature = request.get("signature");

        try {
            // 更新用户资料
            User user = userService.update(oldUsername, newUsername, email, birthday, favorite, gender, region, signature);
            if (user == null) {
                response.put("code", 400);
                response.put("msg", "用户不存在");
                return response;
            }

            // 返回响应
            response.put("code", 200);
            response.put("msg", "更新成功");
        } catch (IllegalArgumentException e) {
            // 捕获用户名重复的异常
            response.put("code", 400);
            response.put("msg", "修改失败，该用户已存在");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "更新失败: " + e.getMessage());
        }

        return response;
    }

    // 获取用户信息接口（通过用户名）
    @GetMapping("/info")
    public Map<String, Object> getUserInfo(@RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        User user = userService.findByUsername(username);
        if (user == null) {
            response.put("code", 400);
            response.put("msg", "用户不存在");
            return response;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("avatar", user.getAvatar());
        response.put("code", 200);
        response.put("data", data);
        return response;
    }

    // 获取用户信息接口（通过用户ID）
    @GetMapping("/info-by-id")
    public Map<String, Object> getUserInfoById(@RequestParam Long id) {
        Map<String, Object> response = new HashMap<>();
        User user = userService.findById(id);
        if (user == null) {
            response.put("code", 400);
            response.put("msg", "用户不存在");
            return response;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("role", user.getRole());
        data.put("avatar", user.getAvatar());
        response.put("code", 200);
        response.put("data", data);
        return response;
    }

    // 获取用户资料接口
    @PostMapping("/profile")
    public Map<String, Object> getProfile(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String username = request.get("username");

        // 查找用户
        User user = userService.findByUsername(username);
        if (user == null) {
            response.put("code", 400);
            response.put("msg", "用户不存在");
            return response;
        }

        // 返回响应
            response.put("code", 200);
            response.put("msg", "获取成功");
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("role", user.getRole());
            data.put("email", user.getEmail());
            data.put("birthday", user.getBirthday() != null ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(user.getBirthday()) : "");
            data.put("favorite", user.getFavorite());
            data.put("avatar", user.getAvatar());
            data.put("gender", user.getGender());
            data.put("region", user.getRegion());
            data.put("signature", user.getSignature());
            // 隐私设置
            data.put("profilePublic", user.getProfilePublic() != null ? user.getProfilePublic() : true);
            data.put("showWatchHistory", user.getShowWatchHistory() != null ? user.getShowWatchHistory() : true);
            data.put("showFavorites", user.getShowFavorites() != null ? user.getShowFavorites() : true);
            data.put("showRatings", user.getShowRatings() != null ? user.getShowRatings() : true);
            data.put("showPosts", user.getShowPosts() != null ? user.getShowPosts() : true);
            data.put("showComments", user.getShowComments() != null ? user.getShowComments() : true);
            data.put("showFollows", user.getShowFollows() != null ? user.getShowFollows() : true);
            response.put("data", data);

        return response;
    }

    // 上传头像接口
    @PostMapping("/avatar")
    public Map<String, Object> uploadAvatar(@RequestParam("file") MultipartFile file, @RequestParam("username") String username) {
        Map<String, Object> response = new HashMap<>();

        // 查找用户
        User user = userService.findByUsername(username);
        if (user == null) {
            response.put("code", 400);
            response.put("msg", "用户不存在");
            return response;
        }

        // 保存头像
        try {
            log.debug("开始上传头像...");
            log.debug("用户名: {}", username);
            log.debug("文件名: {}", file.getOriginalFilename());
            log.debug("文件大小: {}", file.getSize());
            
            // 使用 OSSUtil 上传文件到本地存储
            String result = ossUtil.uploadAvatar(file);
            
            // 检查返回的是本地文件系统路径还是相对路径
            String avatarUrl;
            if (result.startsWith("http://") || result.startsWith("https://")) {
                // 如果是完整URL，直接使用
                avatarUrl = result;
            } else if (result.startsWith("C:/") || result.startsWith("D:/")) {
                // 如果是本地文件系统路径，直接使用
                avatarUrl = result;
            } else {
                // 如果是相对路径，构建本地URL
                avatarUrl = "http://localhost:8080" + result;
            }
            
            // 更新用户头像
            userService.updateAvatar(username, avatarUrl);
            log.info("头像上传成功: {}", avatarUrl);
            
            // 返回响应
            Map<String, Object> data = new HashMap<>();
            data.put("avatar", avatarUrl);
            response.put("code", 200);
            response.put("msg", "头像上传成功");
            response.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "头像上传失败: " + e.getMessage());
        }

        return response;
    }

    // 修改密码接口
    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> request) {
        log.debug("=== 修改密码请求 ===");
        Map<String, Object> response = new HashMap<>();
        String username = request.get("username");
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");
        log.debug("用户名: {}", username);

        try {
            // 查找用户
            User user = userService.findByUsername(username);
            if (user == null) {
                response.put("code", 400);
                response.put("msg", "用户不存在");
                return response;
            }

            // 验证旧密码
            boolean isValid = userService.validatePassword(oldPassword, user.getPassword());
            if (!isValid) {
                response.put("code", 400);
                response.put("msg", "旧密码错误");
                return response;
            }

            // 验证新密码格式
            if (newPassword.length() < 6 || newPassword.length() > 20 || newPassword.contains(" ")) {
                response.put("code", 400);
                response.put("msg", "新密码长度必须在6-20个字符之间，且不能包含空格");
                return response;
            }

            // 更新密码
            String encodedPassword = userService.encodePassword(newPassword);
            user.setPassword(encodedPassword);
            userService.save(user);

            response.put("code", 200);
            response.put("msg", "密码修改成功");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "密码修改失败: " + e.getMessage());
        }

        return response;
    }

    // 激活用户接口
    @GetMapping("/activate")
    public Map<String, Object> activate(@RequestParam String code) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean activated = userService.activateUser(code);
            if (activated) {
                response.put("code", 200);
                response.put("msg", "账号激活成功，请登录");
            } else {
                response.put("code", 400);
                response.put("msg", "激活码无效或已过期");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "激活失败: " + e.getMessage());
        }
        
        return response;
    }

    // 获取CSRF令牌接口
    @GetMapping("/csrf-token")
    public Map<String, Object> getCsrfToken(org.springframework.security.web.csrf.CsrfToken token) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", token.getToken());
        return response;
    }

    // 刷新令牌接口
    @PostMapping("/refresh-token")
    public Map<String, Object> refreshToken(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String refreshToken = request.get("refreshToken");
        
        try {
            // 验证刷新令牌
            if (!jwtUtils.validateToken(refreshToken)) {
                response.put("code", 401);
                response.put("msg", "刷新令牌无效");
                return response;
            }
            
            // 从刷新令牌中获取用户名
            String username = jwtUtils.getUsernameFromToken(refreshToken);
            
            // 生成新的访问令牌和刷新令牌
            String newAccessToken = jwtUtils.generateAccessToken(username);
            String newRefreshToken = jwtUtils.generateRefreshToken(username);
            
            response.put("code", 200);
            response.put("msg", "令牌刷新成功");
            Map<String, Object> data = new HashMap<>();
            data.put("token", newAccessToken);
            data.put("refreshToken", newRefreshToken);
            response.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("msg", "令牌刷新失败: " + e.getMessage());
        }
        
        return response;
    }

    // 更新隐私设置
    @PostMapping("/privacy")
    public Map<String, Object> updatePrivacy(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = (String) request.get("username");
            User user = userService.findByUsername(username);
            if (user == null) {
                response.put("code", 400);
                response.put("msg", "用户不存在");
                return response;
            }
            if (request.containsKey("profilePublic")) user.setProfilePublic((Boolean) request.get("profilePublic"));
            if (request.containsKey("showWatchHistory")) user.setShowWatchHistory((Boolean) request.get("showWatchHistory"));
            if (request.containsKey("showFavorites")) user.setShowFavorites((Boolean) request.get("showFavorites"));
            if (request.containsKey("showRatings")) user.setShowRatings((Boolean) request.get("showRatings"));
            if (request.containsKey("showPosts")) user.setShowPosts((Boolean) request.get("showPosts"));
            if (request.containsKey("showComments")) user.setShowComments((Boolean) request.get("showComments"));
            if (request.containsKey("showFollows")) user.setShowFollows((Boolean) request.get("showFollows"));
            userService.save(user);
            response.put("code", 200);
            response.put("msg", "隐私设置更新成功");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "更新失败：" + e.getMessage());
        }
        return response;
    }

    // 搜索用户
    @GetMapping("/search")
    public Map<String, Object> searchUsers(@RequestParam String keyword) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                response.put("code", 200);
                response.put("data", new ArrayList<>());
                return response;
            }
            List<User> users = userService.findByUsernameContaining(keyword.trim());
            List<Map<String, Object>> result = new ArrayList<>();
            for (User user : users) {
                // 非管理员不显示测试用户
                if (!SecurityUtils.isCurrentUserAdmin() && user.getIsTest() != null && user.getIsTest()) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>();
                item.put("id", user.getId());
                item.put("username", user.getUsername());
                item.put("avatar", user.getAvatar());
                item.put("signature", user.getSignature());
                result.add(item);
            }
            response.put("code", 200);
            response.put("data", result);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "搜索失败：" + e.getMessage());
        }
        return response;
    }

}
