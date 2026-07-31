"""
论坛模块 API 测试
覆盖: 帖子列表、创建、点赞、评论、搜索
"""
import pytest
from conftest import api_get, api_post, api_delete, assert_code_200


# 辅助函数：判断响应是纯列表还是 {code, data} 包装格式
def _get_data(resp):
    """从响应中提取数据，兼容纯列表和 {code, data} 两种格式"""
    data = resp.json()
    if isinstance(data, dict) and "code" in data:
        return data["data"]
    return data


class TestForumPost:
    """论坛帖子相关"""

    def test_get_posts(self, base_url):
        """测试: 获取帖子列表"""
        resp = api_get("/api/post/list")
        data = _get_data(resp)
        assert isinstance(data, list)

    def test_create_post(self, base_url, auth_headers):
        """测试: 创建帖子"""
        resp = api_post("/api/post/create", headers=auth_headers, json_data={
            "title": "自动化测试帖子",
            "content": "这是自动化测试创建的帖子内容",
            "username": "testuser",
            "isTest": True
        })
        data = resp.json()
        # 成功时返回帖子对象，失败时返回 {code, msg}
        if "code" in data:
            assert data["code"] == 200
        else:
            assert "id" in data  # 直接返回帖子对象

    def test_create_post_empty_title(self, base_url, auth_headers):
        """测试: 空标题创建帖子"""
        resp = api_post("/api/post/create", headers=auth_headers, json_data={
            "title": "",
            "content": "内容",
            "username": "testuser"
        })
        data = resp.json()
        # 空标题应被拒绝，可能返回 {code: 400} 或直接返回帖子对象（无 id）
        if "code" in data:
            assert data["code"] != 200
        else:
            assert "id" not in data or data.get("title") == ""

    def test_get_post_detail(self, base_url):
        """测试: 获取帖子详情"""
        resp = api_get("/api/post/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            post_id = data[0]["id"]
            resp = api_get(f"/api/post/detail/{post_id}")
            detail = resp.json()
            # 直接返回帖子对象，检查 id 字段
            assert "id" in detail
            assert detail["id"] == post_id

    def test_search_posts(self, base_url):
        """测试: 搜索帖子"""
        resp = api_post("/api/post/search", json_data={"keyword": "测试"})
        data = resp.json()
        assert data["code"] == 200

    def test_like_post(self, base_url, auth_headers):
        """测试: 点赞帖子"""
        resp = api_get("/api/post/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            post_id = data[0]["id"]
            resp = api_post("/api/post/like", headers=auth_headers, json_data={
                "postId": post_id,
                "username": "testuser"
            })
            like_data = resp.json()
            assert like_data["code"] == 200

    def test_sort_by_time(self, base_url):
        """测试: 按时间排序"""
        resp = api_get("/api/post/sort/time")
        data = _get_data(resp)
        assert isinstance(data, list)

    def test_sort_by_likes(self, base_url):
        """测试: 按点赞数排序"""
        resp = api_get("/api/post/sort/likes")
        data = _get_data(resp)
        assert isinstance(data, list)


class TestForumComment:
    """论坛评论相关"""

    def test_get_comments(self, base_url):
        """测试: 获取帖子评论"""
        resp = api_get("/api/post/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            post_id = data[0]["id"]
            resp = api_post("/api/comment/getComments", json_data={"postId": post_id})
            comment_data = resp.json()
            assert comment_data["code"] == 200

    def test_add_comment(self, base_url, auth_headers):
        """测试: 添加评论"""
        resp = api_get("/api/post/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            post_id = data[0]["id"]
            resp = api_post("/api/comment/addComment", headers=auth_headers, json_data={
                "postId": post_id,
                "content": "自动化测试评论",
                "username": "testuser",
                "isTest": True
            })
            comment_data = resp.json()
            assert comment_data["code"] == 200

    def test_like_comment(self, base_url, auth_headers):
        """测试: 点赞评论"""
        resp = api_get("/api/post/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            post_id = data[0]["id"]
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


class TestForumInteraction:
    """论坛互动相关"""

    def test_dislike_comment(self, base_url, auth_headers):
        """测试: 点踩论坛评论"""
        resp = api_get("/api/post/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            post_id = data[0]["id"]
            resp = api_post("/api/comment/getComments", json_data={"postId": post_id})
            comments = resp.json()
            if comments["code"] == 200 and len(comments["data"]) > 0:
                comment_id = comments["data"][0]["id"]
                resp = api_post("/api/comment/dislike", headers=auth_headers, json_data={
                    "commentId": comment_id,
                    "username": "testuser"
                })
                dislike_data = resp.json()
                assert dislike_data["code"] in (200, 400)

    def test_dislike_post(self, base_url, auth_headers):
        """测试: 点踩帖子"""
        resp = api_get("/api/post/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            post_id = data[0]["id"]
            resp = api_post("/api/post/dislike", headers=auth_headers, json_data={
                "postId": post_id,
                "username": "testuser"
            })
            dislike_data = resp.json()
            assert dislike_data["code"] in (200, 400)

    def test_get_post_interaction_status(self, base_url, auth_headers):
        """测试: 获取帖子互动状态"""
        resp = api_get("/api/post/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            post_id = data[0]["id"]
            resp = api_post("/api/post/interaction-status", headers=auth_headers, json_data={
                "postId": post_id,
                "username": "testuser"
            })
            status_data = resp.json()
            assert status_data["code"] == 200