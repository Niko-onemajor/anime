"""
管理员模块 API 测试
覆盖: 用户管理、动漫管理、论坛管理、删除记录管理
需要: admin 账号已存在
"""
import pytest
from conftest import api_post, assert_code_200


class TestAdminUserManagement:
    """管理员 - 用户管理"""

    def test_get_users(self, base_url, admin_headers):
        """测试: 获取用户列表"""
        resp = api_post("/api/admin/users", headers=admin_headers, json_data={
            "page": 0, "size": 8
        })
        data = assert_code_200(resp, "获取用户列表")
        assert "content" in data

    def test_search_users(self, base_url, admin_headers):
        """测试: 搜索用户"""
        resp = api_post("/api/admin/users/search", headers=admin_headers, json_data={
            "keyword": "test", "page": 0, "size": 8
        })
        data = resp.json()
        assert data["code"] == 200

    def test_add_user(self, base_url, admin_headers):
        """测试: 添加用户"""
        resp = api_post("/api/admin/users/add", headers=admin_headers, json_data={
            "username": "admin_test_user",
            "password": "AdminTest1",
            "email": "admintest@test.com",
            "role": "0"
        })
        data = resp.json()
        assert data["code"] == 200

    def test_add_user_weak_password(self, base_url, admin_headers):
        """测试: 弱密码添加用户被拒绝"""
        resp = api_post("/api/admin/users/add", headers=admin_headers, json_data={
            "username": "weakadmin",
            "password": "12345678",
            "email": "weak@test.com",
            "role": "0"
        })
        data = resp.json()
        assert data["code"] != 200  # 弱密码应被拒绝

    def test_update_user(self, base_url, admin_headers):
        """测试: 更新用户信息"""
        resp = api_post("/api/admin/users/update", headers=admin_headers, json_data={
            "id": 2,
            "username": "testuser",
            "email": "test_updated@test.com",
            "role": "0"
        })
        data = resp.json()
        assert data["code"] == 200

    def test_reset_password(self, base_url, admin_headers):
        """测试: 重置用户密码"""
        resp = api_post("/api/admin/users/reset-password", headers=admin_headers, json_data={
            "id": 2,
            "newPassword": "ResetPass1"
        })
        data = resp.json()
        assert data["code"] == 200


class TestAdminAnimeManagement:
    """管理员 - 动漫管理"""

    def test_get_animes(self, base_url, admin_headers):
        """测试: 获取动漫管理列表"""
        resp = api_post("/api/admin/animes", headers=admin_headers, json_data={
            "page": 0, "size": 8
        })
        data = assert_code_200(resp, "获取动漫列表")
        assert "content" in data

    def test_search_animes(self, base_url, admin_headers):
        """测试: 搜索动漫"""
        resp = api_post("/api/admin/animes/search", headers=admin_headers, json_data={
            "keyword": "火影", "page": 0, "size": 8
        })
        data = resp.json()
        assert data["code"] == 200

    def test_add_anime(self, base_url, admin_headers):
        """测试: 添加动漫"""
        resp = api_post("/api/admin/animes/add", headers=admin_headers, json_data={
            "title": "自动化测试动漫",
            "description": "自动化测试描述",
            "genre": "动作",
            "year": "2024",
            "letter": "Z",
            "image": "",
            "rating": 8.0
        })
        data = resp.json()
        assert data["code"] == 200


class TestAdminForumManagement:
    """管理员 - 论坛管理"""

    def test_get_forum_posts(self, base_url, admin_headers):
        """测试: 获取论坛帖子管理列表"""
        resp = api_post("/api/admin/forum/posts", headers=admin_headers, json_data={
            "page": 0, "size": 6
        })
        data = assert_code_200(resp, "获取论坛帖子")
        assert "content" in data

    def test_search_forum_posts(self, base_url, admin_headers):
        """测试: 搜索论坛帖子"""
        resp = api_post("/api/admin/forum/posts/search", headers=admin_headers, json_data={
            "keyword": "测试", "page": 0, "size": 6
        })
        data = resp.json()
        assert data["code"] == 200

    def test_get_forum_comments(self, base_url, admin_headers):
        """测试: 获取论坛评论管理列表"""
        resp = api_post("/api/admin/forum/comments", headers=admin_headers, json_data={
            "page": 0, "size": 6
        })
        data = resp.json()
        assert data["code"] == 200


class TestAdminDeletedRecords:
    """管理员 - 删除记录管理"""

    def test_get_deleted_users(self, base_url, admin_headers):
        """测试: 获取已删除用户"""
        resp = api_post("/api/admin/users/deleted", headers=admin_headers, json_data={
            "page": 0, "size": 6
        })
        data = resp.json()
        assert data["code"] == 200

    def test_get_deleted_animes(self, base_url, admin_headers):
        """测试: 获取已删除动漫"""
        resp = api_post("/api/admin/animes/deleted", headers=admin_headers, json_data={
            "page": 0, "size": 6
        })
        data = resp.json()
        assert data["code"] == 200


class TestAdminUnauthorizedAccess:
    """管理员 - 未授权访问"""

    def test_no_token_access(self, base_url):
        """测试: 无 Token 访问管理接口"""
        resp = api_post("/api/admin/users", json_data={"page": 0, "size": 8})
        data = resp.json()
        assert data["code"] != 200  # 应返回未授权

    def test_user_token_access(self, base_url, auth_headers):
        """测试: 普通用户 Token 访问管理接口"""
        resp = api_post("/api/admin/users", headers=auth_headers, json_data={
            "page": 0, "size": 8
        })
        data = resp.json()
        assert data["code"] != 200  # 普通用户应被拒绝