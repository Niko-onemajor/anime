import pymysql

conn = pymysql.connect(
    host='localhost', port=3306, user='root', password='123456',
    database='anime', charset='utf8mb4'
)
cursor = conn.cursor()

# Check users with test in name
cursor.execute("SELECT id, username, is_test FROM users WHERE username LIKE '%test%'")
rows = cursor.fetchall()
print("Users with 'test' in name:")
for r in rows:
    print(f"  id={r[0]}, username={r[1]}, is_test={r[2]}")

# Check if is_test column exists
cursor.execute("SHOW COLUMNS FROM users LIKE 'is_test'")
col = cursor.fetchone()
print(f"\nis_test column exists: {col is not None}")
if col:
    print(f"  Column info: {col}")

# Check NULL values
cursor.execute("SELECT COUNT(*) FROM users WHERE is_test IS NULL")
null_count = cursor.fetchone()[0]
print(f"\nUsers with NULL is_test: {null_count}")

cursor.execute("SELECT COUNT(*) FROM users WHERE is_test = TRUE")
true_count = cursor.fetchone()[0]
print(f"Users with is_test = TRUE: {true_count}")

cursor.execute("SELECT COUNT(*) FROM users WHERE is_test = FALSE")
false_count = cursor.fetchone()[0]
print(f"Users with is_test = FALSE: {false_count}")

conn.close()