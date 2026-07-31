"""
测试配置文件 - 全局 fixtures 和 配置
"""
import pytest
import requests
import json
import os
import pymysql

# --- 基础配置 ---
BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080")
FRONTEND_URL = os.environ.get("FRONTEND_URL", "http://localhost:5173")
TEST_USERNAME = "testuser"
TEST_PASSWORD = "Test@1234"
ADMIN_USERNAME = "admin"
ADMIN_PASSWORD = "Admin@123"

# 数据库配置（用于创建 admin 账号）
DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "anime",
    "charset": "utf8mb4",
}

# 测试报告输出目录
REPORT_DIR = os.path.join(os.path.dirname(__file__), "reports")

# 用于终端报告钩子
_terminal = None


# ============================================================
# pytest 钩子: 在测试结束时打印详细摘要
# ============================================================

def pytest_terminal_summary(terminalreporter, exitstatus, config):
    """测试结束后打印详细失败摘要"""
    global _terminal
    _terminal = terminalreporter

    # 统计
    stats = terminalreporter.stats
    passed = len(stats.get('passed', []))
    failed = len(stats.get('failed', []))
    skipped = len(stats.get('skipped', []))
    error = len(stats.get('error', []))

    total = passed + failed + skipped + error
    if total == 0:
        return

    terminalreporter.write_sep("=", "测试结果摘要")
    terminalreporter.write_line(
        f"总计: {total}  |  "
        f"通过: {passed}  |  "
        f"失败: {failed}  |  "
        f"跳过: {skipped}  |  "
        f"错误: {error}"
    )

    if failed > 0:
        terminalreporter.write_sep("-", "失败用例详情")
        for report in stats.get('failed', []):
            test_name = report.nodeid
            # 提取失败原因
            longrepr = str(report.longrepr) if report.longrepr else ""
            # 提取 AssertionError 或关键错误行
            error_lines = []
            for line in longrepr.split('\n'):
                line = line.strip()
                if 'AssertionError' in line or 'assert ' in line:
                    error_lines.append(line)
                elif 'E   ' in line and ('Error' in line or 'assert' in line):
                    error_lines.append(line.replace('E   ', ''))

            reason = error_lines[0] if error_lines else longrepr[:200]
            terminalreporter.write_line(f"  X  {test_name}")
            terminalreporter.write_line(f"     原因: {reason}")

    if error > 0:
        terminalreporter.write_sep("-", "错误用例详情")
        for report in stats.get('error', []):
            terminalreporter.write_line(f"  !! {report.nodeid}")
            longrepr = str(report.longrepr) if report.longrepr else ""
            # 提取第一行错误
            first_line = longrepr.split('\n')[0] if longrepr else ""
            terminalreporter.write_line(f"     原因: {first_line}")


def pytest_collection_modifyitems(config, items):
    """为每个测试用例添加中文标记"""
    # 测试类名 → 中文描述映射
    DESC_MAP = {
        "TestAuth": "认证模块",
        "TestToken": "Token管理",
        "TestProfile": "用户资料",
        "TestAnimeList": "动漫列表",
        "TestAnimeDetail": "动漫详情",
        "TestAnimeRanking": "排行榜",
        "TestAnimeFilter": "动漫筛选",
        "TestAnimeComment": "动漫评论",
        "TestAnimeInteraction": "动漫互动",
        "TestAnimeExtra": "动漫扩展",
        "TestEpisode": "集数管理",
        "TestRecommendation": "推荐功能",
        "TestForumPost": "论坛帖子",
        "TestForumComment": "论坛评论",
        "TestForumInteraction": "论坛互动",
        "TestFollow": "关注功能",
        "TestFavorite": "收藏功能",
        "TestRating": "评分功能",
        "TestWatchHistory": "观看记录",
        "TestUserSearch": "用户搜索",
        "TestNotification": "通知功能",
        "TestNotificationAction": "通知操作",
        "TestChat": "聊天功能",
        "TestChatAction": "聊天操作",
        "TestPrivacy": "隐私设置",
        "TestAdminUserManagement": "管理员-用户管理",
        "TestAdminAnimeManagement": "管理员-动漫管理",
        "TestAdminForumManagement": "管理员-论坛管理",
        "TestAdminDeletedRecords": "管理员-删除记录",
        "TestAdminUnauthorizedAccess": "管理员-权限控制",
        "TestResponseTime": "性能-响应时间",
        "TestConcurrentRequests": "性能-并发请求",
        "TestLoadAndThroughput": "性能-负载吞吐量",
        "TestApiStability": "性能-API稳定性",
        "TestPageLoadSpeed": "性能-页面加载速度",
        "TestAuthorizationBypass": "安全-权限越界",
        "TestXSSProtection": "安全-XSS防护",
        "TestSQLInjectionProtection": "安全-SQL注入",
        "TestPathTraversal": "安全-路径遍历",
        "TestTokenSecurity": "安全-Token安全",
        "TestInformationLeakage": "安全-信息泄露",
        "TestCSRFProtection": "安全-CSRF防护",
        "TestInputValidation": "安全-输入验证",
        "TestRateLimiting": "安全-速率限制",
        "TestCORSSecurity": "安全-跨域安全",
        "TestFileUpload": "文件上传",
        "TestMisc": "杂项接口",
        "TestPageLoads": "页面加载",
        "TestNavigation": "页面导航",
        "TestLoginFlow": "登录流程",
        "TestAnimeDetail": "动漫详情页",
        "TestUserHome": "用户主页",
    }


# ============================================================
# 全局 Session Fixtures
# ============================================================

@pytest.fixture(scope="session")
def base_url():
    """后端 API 基础地址"""
    return BASE_URL


@pytest.fixture(scope="session")
def frontend_url():
    """前端页面基础地址"""
    return FRONTEND_URL


def _ensure_admin_account():
    """通过数据库直接创建 admin 账号（角色为 admin）"""
    try:
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor()
        # 检查 admin 是否存在
        cursor.execute("SELECT id, role FROM users WHERE username = %s", (ADMIN_USERNAME,))
        row = cursor.fetchone()
        if row:
            # 用户存在，检查角色
            if row[1] not in ("admin", "1"):
                cursor.execute("UPDATE users SET role = 'admin' WHERE username = %s", (ADMIN_USERNAME,))
                conn.commit()
        else:
            # 创建 admin 用户（密码使用 BCrypt 加密的 Admin@1234）
            # 先通过注册 API 创建，再通过 DB 改角色
            pass
        cursor.close()
        conn.close()
        return True
    except Exception as e:
        print(f"[WARN] 无法连接数据库创建 admin: {e}")
        return False


def _mark_test_user():
    """通过数据库将测试用户及其所有内容标记为 is_test = true"""
    try:
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor()
        # 标记测试用户
        cursor.execute("UPDATE users SET is_test = TRUE WHERE username = %s", (TEST_USERNAME,))
        if cursor.rowcount > 0:
            print(f"[INFO] 已将 {TEST_USERNAME} 标记为测试用户")
        # 标记测试用户的帖子
        cursor.execute("""
            UPDATE posts SET is_test = TRUE 
            WHERE author_id IN (SELECT id FROM users WHERE username = %s)
            AND (is_test IS NULL OR is_test = FALSE)
        """, (TEST_USERNAME,))
        if cursor.rowcount > 0:
            print(f"[INFO] 已将 {TEST_USERNAME} 的 {cursor.rowcount} 条帖子标记为测试数据")
        # 标记测试用户的论坛评论
        cursor.execute("""
            UPDATE comments SET is_test = TRUE 
            WHERE author_id IN (SELECT id FROM users WHERE username = %s)
            AND (is_test IS NULL OR is_test = FALSE)
        """, (TEST_USERNAME,))
        if cursor.rowcount > 0:
            print(f"[INFO] 已将 {TEST_USERNAME} 的 {cursor.rowcount} 条论坛评论标记为测试数据")
        # 标记测试用户的动漫评论
        cursor.execute("""
            UPDATE anime_comments SET is_test = TRUE 
            WHERE author_id IN (SELECT id FROM users WHERE username = %s)
            AND (is_test IS NULL OR is_test = FALSE)
        """, (TEST_USERNAME,))
        if cursor.rowcount > 0:
            print(f"[INFO] 已将 {TEST_USERNAME} 的 {cursor.rowcount} 条动漫评论标记为测试数据")
        conn.commit()
        cursor.close()
        conn.close()
    except Exception as e:
        print(f"[WARN] 无法标记测试用户: {e}")


@pytest.fixture(scope="session")
def admin_token(base_url):
    """获取管理员 JWT Token（自动创建 admin 账号）"""
    # 先尝试登录
    resp = requests.post(f"{base_url}/api/user/login", json={
        "username": ADMIN_USERNAME,
        "password": ADMIN_PASSWORD
    })
    data = resp.json()
    if data.get("code") == 200:
        return data["data"]["token"]

    # 尝试通过注册 API 创建 admin 用户
    resp = requests.post(f"{base_url}/api/user/register", json={
        "username": ADMIN_USERNAME,
        "password": ADMIN_PASSWORD,
        "email": "admin@anime.com"
    })
    data = resp.json()
    if data.get("code") == 200:
        # 注册成功，通过 DB 将角色改为 admin
        _ensure_admin_account()
        # 重新登录
        resp = requests.post(f"{base_url}/api/user/login", json={
            "username": ADMIN_USERNAME,
            "password": ADMIN_PASSWORD
        })
        data = resp.json()
        if data.get("code") == 200:
            return data["data"]["token"]

    # 如果 DB 连接失败，尝试通过 DB 直接创建
    if _ensure_admin_account():
        resp = requests.post(f"{base_url}/api/user/login", json={
            "username": ADMIN_USERNAME,
            "password": ADMIN_PASSWORD
        })
        data = resp.json()
        if data.get("code") == 200:
            return data["data"]["token"]

    pytest.skip("无法创建管理员账号，跳过管理员相关测试")
    return None


@pytest.fixture(scope="session")
def user_token(base_url):
    """获取普通用户 JWT Token（自动注册测试账号）"""
    # 尝试登录
    resp = requests.post(f"{base_url}/api/user/login", json={
        "username": TEST_USERNAME,
        "password": TEST_PASSWORD
    })
    data = resp.json()
    if data.get("code") == 200:
        # 确保测试用户已标记
        _mark_test_user()
        return data["data"]["token"]

    # 注册新账号
    resp = requests.post(f"{base_url}/api/user/register", json={
        "username": TEST_USERNAME,
        "password": TEST_PASSWORD,
        "email": "test@test.com"
    })
    data = resp.json()
    if data.get("code") == 200:
        # 标记为测试用户
        _mark_test_user()
        # 登录获取 token
        resp = requests.post(f"{base_url}/api/user/login", json={
            "username": TEST_USERNAME,
            "password": TEST_PASSWORD
        })
        return resp.json()["data"]["token"]

    pytest.skip("无法创建测试用户，跳过测试")
    return None


@pytest.fixture(scope="session")
def auth_headers(user_token):
    """带 JWT 认证的请求头"""
    return {"Authorization": f"Bearer {user_token}"}


@pytest.fixture(scope="session")
def admin_headers(admin_token):
    """管理员认证请求头"""
    return {"Authorization": f"Bearer {admin_token}"}


# ============================================================
# 工具函数
# ============================================================

def api_get(url, headers=None, params=None):
    """封装 GET 请求"""
    return requests.get(f"{BASE_URL}{url}", headers=headers or {}, params=params)


def api_post(url, headers=None, json_data=None, data=None):
    """封装 POST 请求"""
    return requests.post(f"{BASE_URL}{url}", headers=headers or {}, json=json_data, data=data)


def api_delete(url, headers=None):
    """封装 DELETE 请求"""
    return requests.delete(f"{BASE_URL}{url}", headers=headers or {})


def assert_code_200(resp, msg=""):
    """断言响应 code 为 200"""
    data = resp.json()
    assert data.get("code") == 200, f"{msg} 响应: {data}"
    return data