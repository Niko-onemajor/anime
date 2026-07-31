"""
用户/社交模块 API 测试
覆盖: 关注、收藏、评分、观看记录、搜索、通知、聊天
"""
import pytest
from conftest import api_get, api_post, assert_code_200


# 辅助函数：判断响应是纯列表还是 {code, data} 包装格式
def _get_data(resp):
    """从响应中提取数据，兼容纯列表和 {code, data} 两种格式"""
    data = resp.json()
    if isinstance(data, dict) and "code" in data:
        return data["data"]
    return data


class TestFollow:
    """关注相关"""

    def test_toggle_follow(self, base_url, auth_headers):
        """测试: 关注/取消关注"""
        resp = api_post("/api/follow/toggle", headers=auth_headers, json_data={
            "followerId": 2,
            "followedId": 1
        })
        data = resp.json()
        assert data["code"] == 200

    def test_follow_status(self, base_url):
        """测试: 查询关注状态"""
        resp = api_get("/api/follow/status", params={
            "followerId": 2, "followedId": 1
        })
        data = resp.json()
        assert data["code"] == 200

    def test_follower_count(self, base_url):
        """测试: 粉丝数"""
        resp = api_get("/api/follow/follower-count", params={"userId": 1})
        data = resp.json()
        assert data["code"] == 200

    def test_following_count(self, base_url):
        """测试: 关注数"""
        resp = api_get("/api/follow/following-count", params={"userId": 2})
        data = resp.json()
        assert data["code"] == 200

    def test_following_list(self, base_url):
        """测试: 关注列表"""
        resp = api_get("/api/follow/following-list", params={"userId": 2})
        data = resp.json()
        assert data["code"] == 200

    def test_follower_list(self, base_url):
        """测试: 粉丝列表"""
        resp = api_get("/api/follow/follower-list", params={"userId": 1})
        data = resp.json()
        assert data["code"] == 200


class TestFavorite:
    """收藏相关"""

    def test_add_favorite(self, base_url, auth_headers):
        """测试: 添加收藏"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            anime_id = data[0]["id"]
            resp = api_post("/api/favorites/add", headers=auth_headers, json_data={
                "username": "testuser",
                "animeId": anime_id
            })
            fav_data = resp.json()
            assert fav_data["code"] == 200

    def test_check_favorite(self, base_url, auth_headers):
        """测试: 检查收藏状态"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            anime_id = data[0]["id"]
            resp = api_post("/api/favorites/check", headers=auth_headers, json_data={
                "username": "testuser",
                "animeId": anime_id
            })
            check = resp.json()
            assert check["code"] == 200

    def test_list_favorites(self, base_url):
        """测试: 收藏列表"""
        resp = api_post("/api/favorites/list", json_data={"username": "testuser"})
        data = resp.json()
        assert data["code"] == 200

    def test_remove_favorite(self, base_url, auth_headers):
        """测试: 取消收藏"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            anime_id = data[0]["id"]
            resp = api_post("/api/favorites/remove", headers=auth_headers, json_data={
                "username": "testuser",
                "animeId": anime_id
            })
            rm_data = resp.json()
            assert rm_data["code"] == 200


class TestRating:
    """评分相关"""

    def test_submit_rating(self, base_url, auth_headers):
        """测试: 提交评分"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            anime_id = data[0]["id"]
            resp = api_post("/api/anime/rating/submit", headers=auth_headers, json_data={
                "username": "testuser",
                "animeId": anime_id,
                "rating": 8.5
            })
            rating_data = resp.json()
            assert rating_data["code"] == 200

    def test_user_rating(self, base_url, auth_headers):
        """测试: 获取用户对某动漫的评分"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            anime_id = data[0]["id"]
            resp = api_post("/api/anime/rating/user", headers=auth_headers, json_data={
                "username": "testuser",
                "animeId": anime_id
            })
            rating = resp.json()
            assert rating["code"] == 200

    def test_user_rating_list(self, base_url):
        """测试: 用户评分列表"""
        resp = api_post("/api/anime/rating/user/list", json_data={"username": "testuser"})
        data = resp.json()
        assert data["code"] == 200


class TestWatchHistory:
    """观看记录相关"""

    def test_add_watch_history(self, base_url, auth_headers):
        """测试: 添加观看记录"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            anime_id = data[0]["id"]
            # 获取该动漫的集数列表，拿到真实的 episodeId
            ep_resp = api_get(f"/api/episode/anime/{anime_id}")
            ep_data = ep_resp.json()
            if ep_data["code"] == 200 and len(ep_data["data"]) > 0:
                episode_id = ep_data["data"][0]["id"]
                resp = api_post("/api/watch-history/add", headers=auth_headers, json_data={
                    "username": "testuser",
                    "animeId": anime_id,
                    "episodeId": episode_id
                })
                wh_data = resp.json()
                assert wh_data["code"] == 200

    def test_list_watch_history(self, base_url):
        """测试: 观看记录列表"""
        resp = api_post("/api/watch-history/list", json_data={"username": "testuser"})
        data = resp.json()
        assert data["code"] == 200


class TestUserSearch:
    """用户搜索相关"""

    def test_search_user(self, base_url):
        """测试: 搜索用户"""
        resp = api_get("/api/user/search", params={"keyword": "test"})
        data = resp.json()
        assert data["code"] == 200
        assert isinstance(data["data"], list)

    def test_search_user_empty(self, base_url):
        """测试: 空关键词搜索"""
        resp = api_get("/api/user/search", params={"keyword": ""})
        data = resp.json()
        assert data["code"] == 200


class TestNotification:
    """通知相关"""

    def test_get_notifications(self, base_url, auth_headers):
        """测试: 获取通知列表"""
        resp = api_get("/api/notifications/list", headers=auth_headers, params={
            "username": "testuser"
        })
        data = resp.json()
        assert data["code"] == 200

    def test_unread_count(self, base_url, auth_headers):
        """测试: 未读通知数"""
        resp = api_get("/api/notifications/unread-count", headers=auth_headers, params={
            "username": "testuser"
        })
        data = resp.json()
        assert data["code"] == 200

    def test_sync_notifications(self, base_url, auth_headers):
        """测试: 同步通知"""
        resp = api_post("/api/notifications/sync", headers=auth_headers, json_data={
            "username": "testuser"
        })
        data = resp.json()
        assert data["code"] == 200


class TestNotificationAction:
    """通知操作相关"""

    def test_mark_notification_read(self, base_url, auth_headers):
        """测试: 标记通知已读"""
        resp = api_get("/api/notifications/list", headers=auth_headers, params={
            "username": "testuser"
        })
        data = resp.json()
        if data["code"] == 200 and isinstance(data.get("data"), list) and len(data["data"]) > 0:
            notif_id = data["data"][0]["id"]
            resp = api_post("/api/notifications/read", headers=auth_headers, json_data={
                "id": notif_id
            })
            read_data = resp.json()
            assert read_data["code"] == 200

    def test_mark_all_notifications_read(self, base_url, auth_headers):
        """测试: 全部标记已读"""
        resp = api_post("/api/notifications/read-all", headers=auth_headers, json_data={
            "username": "testuser"
        })
        data = resp.json()
        assert data["code"] == 200


class TestChat:
    """聊天相关"""

    def test_get_conversations(self, base_url, auth_headers):
        """测试: 获取会话列表"""
        # API 需要 userId 参数，从用户信息中获取
        info_resp = api_get("/api/user/info", headers=auth_headers, params={"username": "testuser"})
        info_data = info_resp.json()
        user_id = info_data["data"]["id"]
        resp = api_get("/api/chat/conversations", headers=auth_headers, params={
            "userId": user_id
        })
        data = resp.json()
        assert data["code"] == 200

    def test_send_message(self, base_url, auth_headers):
        """测试: 发送消息"""
        resp = api_post("/api/chat/send", headers=auth_headers, json_data={
            "senderId": 2,
            "receiverId": 1,
            "content": "自动化测试消息"
        })
        data = resp.json()
        assert data["code"] == 200


class TestChatAction:
    """聊天操作相关"""

    def test_get_conversation(self, base_url, auth_headers):
        """测试: 获取两人对话记录"""
        resp = api_get("/api/chat/conversation", headers=auth_headers, params={
            "userId1": 2,
            "userId2": 1
        })
        data = resp.json()
        assert data["code"] == 200

    def test_mark_chat_read(self, base_url, auth_headers):
        """测试: 标记聊天消息已读"""
        resp = api_post("/api/chat/mark-read", headers=auth_headers, json_data={
            "senderId": 1,
            "receiverId": 2
        })
        data = resp.json()
        assert data["code"] == 200

    def test_get_chat_unread_count(self, base_url, auth_headers):
        """测试: 获取聊天未读数"""
        info_resp = api_get("/api/user/info", headers=auth_headers, params={"username": "testuser"})
        info_data = info_resp.json()
        user_id = info_data["data"]["id"]
        resp = api_get("/api/chat/unread-count", headers=auth_headers, params={
            "userId": user_id
        })
        data = resp.json()
        assert data["code"] == 200


class TestPrivacy:
    """隐私设置相关"""

    def test_update_privacy(self, base_url, auth_headers):
        """测试: 更新隐私设置"""
        resp = api_post("/api/user/privacy", headers=auth_headers, json_data={
            "username": "testuser",
            "profilePublic": True,
            "showWatchHistory": False,
            "showFavorites": True,
            "showRatings": True,
            "showPosts": True,
            "showComments": True,
            "showFollows": True
        })
        data = resp.json()
        assert data["code"] == 200