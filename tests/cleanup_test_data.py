"""手动清理测试数据脚本"""
import pymysql

DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "anime",
    "charset": "utf8mb4",
}

def cleanup():
    conn = pymysql.connect(**DB_CONFIG)
    c = conn.cursor()

    # 找到所有测试用户
    c.execute("SELECT id, username FROM users WHERE username LIKE %s", ("%test%",))
    test_users = c.fetchall()
    print(f"测试用户: {test_users}")
    test_ids = [u[0] for u in test_users]
    if not test_ids:
        print("没有测试用户，跳过清理")
        c.close()
        conn.close()
        return

    p = ','.join(['%s'] * len(test_ids))
    args = tuple(test_ids)

    # 按照FK依赖顺序删除
    cleanup_steps = [
        # 1. 评论互动（先删用户直接关联的）
        (f"DELETE FROM comment_interactions WHERE user_id IN ({p})", args, "评论互动(用户)"),
        (f"DELETE FROM comment_interactions WHERE comment_id IN (SELECT id FROM anime_comments WHERE author_id IN ({p}))", args, "动漫评论互动(用户)"),
        # 2. 论坛评论互动
        (f"DELETE FROM forum_comment_interactions WHERE user_id IN ({p})", args, "论坛评论互动(用户)"),
        (f"DELETE FROM forum_comment_interactions WHERE comment_id IN (SELECT id FROM comments WHERE author_id IN ({p}))", args, "论坛评论互动(用户评论)"),
        # 3. 动漫评分
        (f"DELETE FROM anime_ratings WHERE user_id IN ({p})", args, "动漫评分"),
        # 4. 聊天消息
        (f"DELETE FROM chat_messages WHERE sender_id IN ({p}) OR receiver_id IN ({p})", args + args, "聊天消息"),
        # 5. 通知
        (f"DELETE FROM notifications WHERE user_id IN ({p})", args, "通知"),
        # 6. 观看记录
        (f"DELETE FROM watch_history WHERE user_id IN ({p})", args, "观看记录"),
        # 7. 收藏
        (f"DELETE FROM favorites WHERE user_id IN ({p})", args, "收藏"),
        # 8. 关注
        (f"DELETE FROM follows WHERE follower_id IN ({p}) OR followed_id IN ({p})", args + args, "关注"),
        # 9. 动漫评论
        (f"DELETE FROM anime_comments WHERE author_id IN ({p})", args, "动漫评论"),
        # 10. 论坛评论
        (f"DELETE FROM comments WHERE author_id IN ({p})", args, "论坛评论"),
        # 11. 帖子
        (f"DELETE FROM posts WHERE author_id IN ({p})", args, "帖子"),
        # 12. 测试用户
        (f"DELETE FROM users WHERE id IN ({p})", args, "测试用户"),
    ]

    for sql, params, desc in cleanup_steps:
        try:
            c.execute(sql, params)
            if c.rowcount > 0:
                print(f"  [OK] 删除 {desc}: {c.rowcount} 条")
        except Exception as e:
            print(f"  [WARN] 清理 {desc} 失败: {e}")

    conn.commit()
    c.close()
    conn.close()
    print("清理完成!")

if __name__ == "__main__":
    cleanup()