import pymysql

conn = pymysql.connect(
    host='localhost', port=3306, user='root', password='123456',
    database='anime', charset='utf8mb4'
)
cursor = conn.cursor()

# Find non-test users
cursor.execute("SELECT id, username, role FROM users WHERE is_test = FALSE AND deleted = FALSE")
rows = cursor.fetchall()
print("Real users (non-test, non-deleted):")
for r in rows:
    print(f"  id={r[0]}, username={r[1]}, role={r[2]}")

# Check existing posts
cursor.execute("SELECT COUNT(*) FROM posts WHERE is_test = FALSE OR is_test IS NULL")
print(f"\nNon-test posts: {cursor.fetchone()[0]}")

conn.close()