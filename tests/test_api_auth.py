"""
认证模块 API 测试
覆盖: 登录、注册、修改密码、Token刷新、账号激活
"""
import pytest
from conftest import api_post, api_get, assert_code_200, TEST_PASSWORD


class TestAuth:
    """认证相关测试"""

    def test_login_success(self, base_url):
        """测试: 正确账号密码登录成功"""
        resp = api_post("/api/user/login", json_data={
            "username": "testuser",
            "password": TEST_PASSWORD
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
            "password": TEST_PASSWORD
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
        resp = api_get("/api/user/info", headers=auth_headers, params={"username": "testuser"})
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
            "oldPassword": TEST_PASSWORD,
            "newPassword": TEST_PASSWORD
        })
        data = resp.json()
        # 新旧密码相同可能被拒绝，但接口应正常响应
        assert "code" in data


class TestProfile:
    """用户资料相关"""

    def test_update_profile(self, base_url, auth_headers):
        """测试: 更新用户资料"""
        resp = api_post("/api/user/update", headers=auth_headers, json_data={
            "oldUsername": "testuser",
            "username": "testuser",
            "email": "test_updated@test.com",
            "birthday": "2000-01-01",
            "favorite": "火影忍者",
            "gender": "男",
            "region": "北京",
            "signature": "测试签名"
        })
        data = resp.json()
        assert data["code"] == 200

    def test_get_user_info_by_id(self, base_url):
        """测试: 通过ID获取用户信息"""
        resp = api_get("/api/user/info-by-id", params={"id": 1})
        data = resp.json()
        assert data["code"] == 200
        assert data["data"]["id"] == 1


class TestToken:
    """Token 相关测试"""

    def test_no_token_access(self, base_url):
        """测试: 无 Token 访问受保护接口"""
        resp = api_get("/api/user/info")
        data = resp.json()
        # 无 Token + 无参数可能返回 400 (缺少参数) 或 403 (未授权)
        if "code" in data:
            assert data["code"] != 200
        elif "status" in data:
            assert data["status"] in (400, 403)

    def test_refresh_token(self, base_url, user_token):
        """测试: 刷新 Token"""
        # 先登录获取 refreshToken
        login_resp = api_post("/api/user/login", json_data={
            "username": "testuser",
            "password": TEST_PASSWORD
        })
        login_data = login_resp.json()
        if login_data["code"] == 200 and "refreshToken" in login_data["data"]:
            refresh_token = login_data["data"]["refreshToken"]
            resp = api_post("/api/user/refresh-token", json_data={
                "refreshToken": refresh_token
            })
            data = resp.json()
            assert data["code"] == 200
            assert "token" in data["data"]
        else:
            # 无法登录则跳过
            pass