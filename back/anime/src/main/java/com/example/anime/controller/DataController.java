package com.example.anime.controller;

import com.example.anime.utils.DataGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DataController {
    @Autowired
    private DataGenerator dataGenerator;

    @PostMapping("/generate")
    public Map<String, Object> generateData() {
        Map<String, Object> response = new HashMap<>();
        try {
            dataGenerator.generateData();
            response.put("code", 200);
            response.put("msg", "数据生成成功");
        } catch (Exception e) {
            response.put("code", 500);
            response.put("msg", "数据生成失败: " + e.getMessage());
        }
        return response;
    }
}