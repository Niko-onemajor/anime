"""
测试配置文件 - 全局 fixtures 和 配置
"""
import pytest
import requests
import json
import os

# --- 基础配置 ---
BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080")
FRONTEND_URL = os.environ.get("FRONTEND_URL", "http://localhost:5173")
TEST_USERNAME = "testuser"
TEST_PASSWORD = "Test1234"

# 测试报告输出目录
REPORT_DIR = os.path.join(os.path.dirname(__file__), "reports")


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


@pytest.fixture(scope="session")
def admin_token(base_url):
    """获取管理员 JWT Token（前提：admin 账号已存在）"""
    resp = requests.post(f"{base_url}/api/user/login", json={
        "username": "admin",
        "password": "Admin123"
    })
    data = resp.json()
    if data.get("code") == 200:
        return data["data"]["token"]
    pytest.skip("管理员账号不存在或密码错误，跳过管理员相关测试")
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
        return data["data"]["token"]

    # 注册新账号
    resp = requests.post(f"{base_url}/api/user/register", json={
        "username": TEST_USERNAME,
        "password": TEST_PASSWORD,
        "email": "test@test.com"
    })
    data = resp.json()
    if data.get("code") == 200:
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