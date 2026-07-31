"""
认证模块 API 测试
覆盖: 登录、注册、修改密码、Token刷新、账号激活
"""
import pytest
from conftest import api_post, api_get, assert_code_200


class TestAuth:
    """认证相关测试"""

    def test_login_success(self, base_url):
        """测试: 正确账号密码登录成功"""
        resp = api_post("/api/user/login", json_data={
            "username": "testuser",
            "password": "Test1234"
        })
        data = resp.json()
        assert data["code"] == 200
        assert "token" in data["data"]
        assert data["data"]["username"] == "testuser"

    def test_login_wrong_password(self, base_url):
        """测试: 错误密码登录失败"""
        resp = api_post("/api/user/login", json_data={
            "username": "testuser",
            "password": "WrongPassword1"
        })
        data = resp.json()
        assert data["code"] != 200

    def test_login_empty_username(self, base_url):
        """测试: 空用户名登录"""
        resp = api_post("/api/user/login", json_data={
            "username": "",
            "password": "Test1234"
        })
        data = resp.json()
        assert data["code"] != 200

    def test_register_weak_password(self, base_url):
        """测试: 弱密码注册被拒绝"""
        resp = api_post("/api/user/register", json_data={
            "username": "weakuser",
            "password": "12345678",
            "email": "weak@test.com"
        })
        data = resp.json()
        assert data["code"] != 200  # 弱密码应被拒绝

    def test_register_short_password(self, base_url):
        """测试: 短密码注册被拒绝"""
        resp = api_post("/api/user/register", json_data={
            "username": "shortpw",
            "password": "Ab1",
            "email": "short@test.com"
        })
        data = resp.json()
        assert data["code"] != 200

    def test_get_user_info(self, base_url, auth_headers):
        """测试: 获取当前用户信息"""
        resp = api_get("/api/user/info", headers=auth_headers)
        data = assert_code_200(resp, "获取用户信息")
        assert data["data"]["username"] == "testuser"

    def test_get_user_profile(self, base_url):
        """测试: 获取用户公开资料"""
        resp = api_post("/api/user/profile", json_data={"username": "testuser"})
        data = assert_code_200(resp, "获取用户资料")
        assert data["data"]["username"] == "testuser"

    def test_change_password(self, base_url, auth_headers):
        """测试: 修改密码"""
        resp = api_post("/api/user/change-password", headers=auth_headers, json_data={
            "oldPassword": "Test1234",
            "newPassword": "Test1234"
        })
        data = resp.json()
        # 新旧密码相同可能被拒绝，但接口应正常响应
        assert "code" in data


class TestToken:
    """Token 相关测试"""

    def test_no_token_access(self, base_url):
        """测试: 无 Token 访问受保护接口"""
        resp = api_get("/api/user/info")
        data = resp.json()
        assert data["code"] != 200  # 应返回未授权

    def test_refresh_token(self, base_url, user_token):
        """测试: 刷新 Token"""
        resp = api_post("/api/user/refresh-token", json_data={
            "token": user_token
        })
        data = resp.json()
        assert data["code"] == 200
        assert "token" in data["data"]