package com.example.anime.controller;

import com.example.anime.utils.OSSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);
    
    @Autowired
    private OSSUtil ossUtil;
    
    @PostMapping
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam(value = "type", defaultValue = "avatar") String type) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("开始上传文件: {}", file.getOriginalFilename());
            logger.info("文件大小: {} bytes", file.getSize());
            logger.info("上传类型: {}", type);
            
            // 根据类型选择上传方法
            String fileUrl;
            if ("cover".equals(type)) {
                fileUrl = ossUtil.uploadCover(file);
            } else {
                fileUrl = ossUtil.uploadAvatar(file);
            }
            
            logger.info("上传成功，文件URL: {}", fileUrl);
            
            response.put("code", 200);
            response.put("data", fileUrl);
            response.put("msg", "上传成功");
        } catch (IOException | InterruptedException e) {
            logger.error("上传失败: {}", e.getMessage(), e);
            response.put("code", 500);
            response.put("msg", "上传失败: " + e.getMessage());
        }
        
        return response;
    }
    
    @PostMapping("/cover")
    public Map<String, Object> uploadCover(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("开始上传封面: {}", file.getOriginalFilename());
            logger.info("文件大小: {} bytes", file.getSize());
            
            // 使用OSSUtil上传封面
            String fileUrl = ossUtil.uploadCover(file);
            logger.info("上传成功，文件URL: {}", fileUrl);
            
            response.put("code", 200);
            response.put("data", fileUrl);
            response.put("msg", "上传成功");
        } catch (IOException | InterruptedException e) {
            logger.error("上传失败: {}", e.getMessage(), e);
            response.put("code", 500);
            response.put("msg", "上传失败: " + e.getMessage());
        }
        
        return response;
    }
}
