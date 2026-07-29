package com.example.anime.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Component
public class OSSUtil {

    @Value("${local.storage.videoPath}")
    private String videoPath;

    @Value("${local.storage.avatarPath}")
    private String avatarPath;

    @Value("${local.storage.coverPath}")
    private String coverPath;

    @Value("${local.storage.baseUrl}")
    private String baseUrl;

    /**
     * 上传用户头像
     * @param file 头像文件
     * @return 上传后的文件URL
     */
    public String uploadAvatar(MultipartFile file) throws IOException, InterruptedException {
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        return uploadToLocal(file, avatarPath, fileName, "/avatars/");
    }

    /**
     * 上传动漫封面
     * @param file 封面文件
     * @return 上传后的文件URL
     */
    public String uploadCover(MultipartFile file) throws IOException, InterruptedException {
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        return uploadToLocal(file, coverPath, fileName, "/covers/");
    }

    /**
     * 上传动漫视频
     * @param file 视频文件
     * @param fileName 文件名（包含路径）
     * @return 上传后的文件URL
     */
    public String uploadVideo(MultipartFile file, String fileName) throws IOException, InterruptedException {
        // 处理反斜杠，确保路径格式正确
        fileName = fileName.replace("\\", "/");
        
        // 提取文件名和子路径
        String actualFileName;
        String subPath = "";
        
        int lastSlashIndex = fileName.lastIndexOf("/");
        if (lastSlashIndex != -1) {
            actualFileName = fileName.substring(lastSlashIndex + 1);
            subPath = fileName.substring(0, lastSlashIndex);
        } else {
            actualFileName = fileName;
        }
        
        String uniqueFileName = UUID.randomUUID().toString() + "_" + actualFileName;
        
        // 构建完整的存储路径
        String fullStoragePath = videoPath;
        if (!subPath.isEmpty()) {
            fullStoragePath = fullStoragePath + "/" + subPath;
        }
        
        // 构建URL路径
        String urlPath = "/videos";
        if (!subPath.isEmpty()) {
            urlPath = urlPath + "/" + subPath;
        }
        urlPath = urlPath + "/";
        
        // 调用uploadToLocal方法上传文件
        return uploadToLocal(file, fullStoragePath, uniqueFileName, urlPath);
    }

    /**
     * 上传到本地
     */
    private String uploadToLocal(MultipartFile file, String storagePath, String fileName, String urlPath) throws IOException, InterruptedException {
        // 构建本地文件路径
        // 使用Paths.get()来处理路径，避免编码问题
        Path destPath = Paths.get(storagePath, fileName);
        File dest = destPath.toFile();
        String localFilePath = dest.getAbsolutePath();
        
        log.debug("存储路径: {}", storagePath);
        log.debug("文件名: {}", fileName);
        log.debug("本地文件路径: {}", localFilePath);
        log.debug("用户主目录: {}", System.getProperty("user.home"));
        log.debug("当前工作目录: {}", System.getProperty("user.dir"));
        log.debug("Java版本: {}", System.getProperty("java.version"));
        log.debug("操作系统: {}", System.getProperty("os.name"));
        log.debug("操作系统版本: {}", System.getProperty("os.version"));
        
        // 检查目标文件的父目录
        File parentDir = dest.getParentFile();
        log.debug("父目录路径: {}", parentDir);
        log.debug("父目录是否存在: {}", parentDir.exists());
        
        // 保存文件
        log.debug("开始保存文件: {}", localFilePath);
        try {
            // 方法15: 使用应用程序工作目录作为临时存储，然后创建符号链接
            log.debug("方法15: 使用应用程序工作目录作为临时存储，然后创建符号链接");
            
            // 创建应用程序工作目录下的存储目录
            String appStoragePath = System.getProperty("user.dir") + File.separator + "storage" + File.separator + urlPath;
            File appStorageDir = new File(appStoragePath);
            if (!appStorageDir.exists()) {
                appStorageDir.mkdirs();
                log.debug("应用程序存储目录创建成功: {}", appStorageDir.getAbsolutePath());
            }
            
            // 构建应用程序存储路径
            String appFilePath = appStoragePath + fileName;
            File appFile = new File(appFilePath);
            
            // 保存文件到应用程序工作目录
            try (FileOutputStream fos = new FileOutputStream(appFile)) {
                fos.write(file.getBytes());
                fos.flush();
            }
            log.debug("文件保存到应用程序工作目录成功: {}", appFilePath);
            
            // 检查应用程序存储文件
            log.debug("应用程序存储文件是否存在: {}", appFile.exists());
            log.debug("应用程序存储文件大小: {} bytes", appFile.length());
            
            // 尝试创建符号链接
            try {
                // 构建mklink命令
                // 使用ProcessBuilder来执行命令，避免编码问题
                ProcessBuilder pb = new ProcessBuilder();
                pb.command("cmd", "/c", "mklink", "/H", dest.getAbsolutePath(), appFile.getAbsolutePath());
                pb.redirectErrorStream(true);
                log.debug("执行命令: {}", pb.command());
                
                Process process = pb.start();
                int exitCode = process.waitFor();
                log.debug("命令执行结果: {}", exitCode);
                
                // 读取命令输出
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.debug("命令输出: {}", line);
                    }
                }
                
                // 读取错误输出
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.debug("错误输出: {}", line);
                    }
                }
                
                if (exitCode != 0) {
                    // 如果符号链接创建失败，尝试直接复制文件
                    log.debug("符号链接创建失败，尝试直接复制文件");
                    // 确保目标文件的父目录存在
                    if (!parentDir.exists()) {
                        log.debug("目标目录不存在，开始创建");
                        try {
                            boolean created = parentDir.mkdirs();
                            log.debug("目录创建结果: {}", created);
                            if (!created) {
                                log.debug("目录创建失败，使用应用程序工作目录作为替代");
                                // 如果目录创建失败，直接返回应用程序工作目录中的文件
                    log.debug("文件保存成功（使用应用程序工作目录）");
                    // 确保路径中没有双斜杠
                    String storageUrl = "/storage" + urlPath + fileName;
                    return storageUrl.replace("//", "/");
                            }
                            log.debug("目录创建成功");
                        } catch (Exception e) {
                            log.debug("目录创建失败: {}", e.getMessage());
                            log.debug("使用应用程序工作目录作为替代");
                            // 如果目录创建失败，直接返回应用程序工作目录中的文件
                            log.debug("文件保存成功（使用应用程序工作目录）");
                            // 确保路径中没有双斜杠
                            String storageUrl = "/storage" + urlPath + fileName;
                            return storageUrl.replace("//", "/");
                        }
                    }
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(appFile);
                         java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                        log.debug("文件直接复制成功");
                    }
                } else {
                    log.debug("符号链接创建成功");
                }
            } catch (Exception e) {
                log.debug("符号链接创建失败: {}", e.getMessage());
                // 尝试直接复制文件
                log.debug("尝试直接复制文件");
                // 确保目标文件的父目录存在
                if (!parentDir.exists()) {
                    log.debug("目标目录不存在，开始创建");
                    try {
                        boolean created = parentDir.mkdirs();
                        log.debug("目录创建结果: {}", created);
                        if (!created) {
                            log.debug("目录创建失败，使用应用程序工作目录作为替代");
                            // 如果目录创建失败，直接返回应用程序工作目录中的文件
                            log.debug("文件保存成功（使用应用程序工作目录）");
                            return "/storage/" + urlPath + fileName;
                        }
                        log.debug("目录创建成功");
                    } catch (Exception ex) {
                        log.debug("目录创建失败: {}", ex.getMessage());
                        log.debug("使用应用程序工作目录作为替代");
                        // 如果目录创建失败，直接返回应用程序工作目录中的文件
                        log.debug("文件保存成功（使用应用程序工作目录）");
                            // 确保路径中没有双斜杠
                            String storageUrl = "/storage" + urlPath + fileName;
                            return storageUrl.replace("//", "/");
                    }
                }
                try (java.io.FileInputStream fis = new java.io.FileInputStream(appFile);
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                    log.debug("文件直接复制成功");
                } catch (Exception ex) {
                    log.debug("文件复制失败: {}", ex.getMessage());
                    log.debug("使用应用程序工作目录作为替代");
                    // 如果文件复制失败，直接返回应用程序工作目录中的文件
                    log.debug("文件保存成功（使用应用程序工作目录）");
                    // 确保路径中没有双斜杠
                    String storageUrl = "/storage" + urlPath + fileName;
                    return storageUrl.replace("//", "/");
                }
            }
            
            log.debug("文件保存成功");
        } catch (Exception e) {
            log.error("文件保存失败: {}", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        // 返回本地文件系统的绝对路径，格式为 C:/Users/22294/Videos/mp4/[文件夹名]/[文件名]
        String fullPath = storagePath + "/" + fileName;
        // 确保路径使用正斜杠
        return fullPath.replace("\\", "/");
    }
}