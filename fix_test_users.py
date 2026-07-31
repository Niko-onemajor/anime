import pymysql

conn = pymysql.connect(
    host='localhost', port=3306, user='root', password='123456',
    database='anime', charset='utf8mb4'
)
cursor = conn.cursor()

# 1. 标记所有含 "test" 的用户为测试用户
cursor.execute("UPDATE users SET is_test = TRUE WHERE username LIKE '%test%'")
print(f"Marked {cursor.rowcount} test users")

# 2. 标记这些用户的帖子
cursor.execute("""
    UPDATE posts SET is_test = TRUE 
    WHERE author_id IN (SELECT id FROM users WHERE is_test = TRUE)
    AND (is_test IS NULL OR is_test = FALSE)
""")
print(f"Marked {cursor.rowcount} test posts")

# 3. 标记这些用户的论坛评论
cursor.execute("""
    UPDATE comments SET is_test = TRUE 
    WHERE author_id IN (SELECT id FROM users WHERE is_test = TRUE)
    AND (is_test IS NULL OR is_test = FALSE)
""")
print(f"Marked {cursor.rowcount} test comments")

# 4. 标记这些用户的动漫评论
cursor.execute("""
    UPDATE anime_comments SET is_test = TRUE 
    WHERE author_id IN (SELECT id FROM users WHERE is_test = TRUE)
    AND (is_test IS NULL OR is_test = FALSE)
""")
print(f"Marked {cursor.rowcount} test anime_comments")

# 5. 删除测试用户的通知
cursor.execute("DELETE FROM notifications WHERE user_id IN (SELECT id FROM users WHERE is_test = TRUE)")
print(f"Deleted {cursor.rowcount} test notifications")

# 6. 删除测试用户的聊天
cursor.execute("""
    DELETE FROM chat_messages 
    WHERE sender_id IN (SELECT id FROM users WHERE is_test = TRUE)
    OR receiver_id IN (SELECT id FROM users WHERE is_test = TRUE)
""")
print(f"Deleted {cursor.rowcount} test chat messages")

# 7. 删除测试用户的关注
cursor.execute("""
    DELETE FROM follows 
    WHERE follower_id IN (SELECT id FROM users WHERE is_test = TRUE)
    OR followed_id IN (SELECT id FROM users WHERE is_test = TRUE)
""")
print(f"Deleted {cursor.rowcount} test follows")

conn.commit()

# 验证
cursor.execute("""
    SELECT id, username, is_test FROM users 
    WHERE username LIKE '%test%' OR is_test = TRUE
""")
rows = cursor.fetchall()
print("\n=== Verification ===")
for r in rows:
    print(f"  id={r[0]}, username={r[1]}, is_test={r[2]}")

conn.close()
print("\nDone!")