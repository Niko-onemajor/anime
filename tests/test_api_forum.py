"""
论坛模块 API 测试
覆盖: 帖子列表、创建、点赞、评论、搜索
"""
import pytest
from conftest import api_get, api_post, api_delete, assert_code_200


class TestForumPost:
    """论坛帖子相关"""

    def test_get_posts(self, base_url):
        """测试: 获取帖子列表"""
        resp = api_get("/api/post/list")
        data = assert_code_200(resp, "获取帖子列表")
        assert isinstance(data["data"], list)

    def test_create_post(self, base_url, auth_headers):
        """测试: 创建帖子"""
        resp = api_post("/api/post/create", headers=auth_headers, json_data={
            "title": "自动化测试帖子",
            "content": "这是自动化测试创建的帖子内容",
            "username": "testuser"
        })
        data = resp.json()
        assert data["code"] == 200

    def test_create_post_empty_title(self, base_url, auth_headers):
        """测试: 空标题创建帖子"""
        resp = api_post("/api/post/create", headers=auth_headers, json_data={
            "title": "",
            "content": "内容",
            "username": "testuser"
        })
        data = resp.json()
        assert data["code"] != 200  # 空标题应被拒绝

    def test_get_post_detail(self, base_url):
        """测试: 获取帖子详情"""
        resp = api_get("/api/post/list")
        data = resp.json()
        if data["code"] == 200 and len(data["data"]) > 0:
            post_id = data["data"][0]["id"]
            resp = api_get(f"/api/post/detail/{post_id}")
            detail = resp.json()
            assert detail["code"] == 200

    def test_search_posts(self, base_url):
        """测试: 搜索帖子"""
        resp = api_post("/api/post/search", json_data={"keyword": "测试"})
        data = resp.json()
        assert data["code"] == 200

    def test_like_post(self, base_url, auth_headers):
        """测试: 点赞帖子"""
        resp = api_get("/api/post/list")
        data = resp.json()
        if data["code"] == 200 and len(data["data"]) > 0:
            post_id = data["data"][0]["id"]
            resp = api_post("/api/post/like", headers=auth_headers, json_data={
                "postId": post_id,
                "username": "testuser"
            })
            like_data = resp.json()
            assert like_data["code"] == 200

    def test_sort_by_time(self, base_url):
        """测试: 按时间排序"""
        resp = api_get("/api/post/sort/time")
        data = resp.json()
        assert data["code"] == 200

    def test_sort_by_likes(self, base_url):
        """测试: 按点赞数排序"""
        resp = api_get("/api/post/sort/likes")
        data = resp.json()
        assert data["code"] == 200


class TestForumComment:
    """论坛评论相关"""

    def test_get_comments(self, base_url):
        """测试: 获取帖子评论"""
        resp = api_get("/api/post/list")
        data = resp.json()
        if data["code"] == 200 and len(data["data"]) > 0:
            post_id = data["data"][0]["id"]
            resp = api_post("/api/comment/getComments", json_data={"postId": post_id})
            comment_data = resp.json()
            assert comment_data["code"] == 200

    def test_add_comment(self, base_url, auth_headers):
        """测试: 添加评论"""
        resp = api_get("/api/post/list")
        data = resp.json()
        if data["code"] == 200 and len(data["data"]) > 0:
            post_id = data["data"][0]["id"]
            resp = api_post("/api/comment/addComment", headers=auth_headers, json_data={
                "postId": post_id,
                "content": "自动化测试评论",
                "username": "testuser"
            })
            comment_data = resp.json()
            assert comment_data["code"] == 200

    def test_like_comment(self, base_url, auth_headers):
        """测试: 点赞评论"""
        resp = api_get("/api/post/list")
        data = resp.json()
        if data["code"] == 200 and len(data["data"]) > 0:
            post_id = data["data"][0]["id"]
            # 获取评论
            resp = api_post("/api/comment/getComments", json_data={"postId": post_id})
            comments = resp.json()
            if comments["code"] == 200 and len(comments["data"]) > 0:
                comment_id = comments["data"][0]["id"]
                resp = api_post("/api/comment/like", headers=auth_headers, json_data={
                    "commentId": comment_id,
                    "username": "testuser"
                })
                like_data = resp.json()
                assert like_data["code"] == 200

    def test_get_comment_by_author(self, base_url):
        """测试: 按作者获取论坛评论"""
        resp = api_get("/api/comment/author/1")
        data = resp.json()
        assert data["code"] == 200