package com.example.anime;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseTest {
    public static void main(String[] args) {
        try {
            // 加载驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // 建立连接
            Connection connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/anime?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai",
                "root",
                "123456"
            );
            
            System.out.println("数据库连接成功！");
            
            // 创建Statement
            Statement statement = connection.createStatement();
            
            // 执行SQL语句
            String sql = "INSERT INTO forum_comment_interactions (comment_id, user_id, interaction_type, create_time) VALUES (8, 1, 1, '2026-03-07 12:30:00')";
            int rows = statement.executeUpdate(sql);
            
            System.out.println("SQL执行成功，影响了" + rows + "行！");
            
            // 关闭连接
            statement.close();
            connection.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}