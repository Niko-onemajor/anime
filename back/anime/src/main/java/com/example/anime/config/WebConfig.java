package com.example.anime.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${local.storage.videoPath}")
    private String videoPath;

    @Value("${local.storage.avatarPath}")
    private String avatarPath;

    @Value("${local.storage.coverPath}")
    private String coverPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置视频文件的访问路径（支持子文件夹）
        registry.addResourceHandler("/videos/**")
                .addResourceLocations("file:///" + videoPath.replace("\\", "/") + "/")
                .setCachePeriod(0);

        // 配置头像文件的访问路径
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:///C:/Users/22294/Pictures/avatars/")
                .setCachePeriod(0);

        // 配置封面文件的访问路径
        registry.addResourceHandler("/covers/**")
                .addResourceLocations("file:///C:/Users/22294/Pictures/covers/")
                .setCachePeriod(0);

        // 配置应用程序工作目录中storage目录的访问路径
        String storageLocation = "file:///" + System.getProperty("user.dir").replace("\\", "/");
        if (!storageLocation.endsWith("/")) {
            storageLocation += "/";
        }
        storageLocation += "storage/";
        registry.addResourceHandler("/storage/**")
                .addResourceLocations(storageLocation)
                .setCachePeriod(0);
    }
}
