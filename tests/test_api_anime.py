"""
动漫模块 API 测试
覆盖: 动漫列表、详情、排行、搜索、年份、字母筛选、评论
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


class TestAnimeList:
    """动漫列表相关"""

    def test_get_anime_list(self, base_url):
        """测试: 获取动漫列表"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        assert isinstance(data, list)
        assert len(data) > 0, "动漫列表为空，请先导入数据"

    def test_get_anime_list_page(self, base_url):
        """测试: 分页获取动漫列表"""
        resp = api_get("/api/anime/list/page", params={"page": 1, "size": 8})
        data = resp.json()
        assert "content" in data
        assert "totalElements" in data

    def test_anime_list_not_empty(self, base_url):
        """测试: 动漫列表不为空"""
        resp = api_get("/api/anime/list")
        data = resp.json()
        if isinstance(data, list):
            assert len(data) > 0, "动漫列表为空，请先导入数据"
        elif data.get("code") == 200:
            assert len(data["data"]) > 0, "动漫列表为空，请先导入数据"


class TestAnimeDetail:
    """动漫详情相关"""

    def test_get_anime_detail(self, base_url):
        """测试: 获取动漫详情"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            anime_id = data[0]["id"]
            resp = api_get(f"/api/anime/detail/{anime_id}")
            detail = resp.json()
            assert detail["id"] == anime_id

    def test_anime_detail_not_found(self, base_url):
        """测试: 获取不存在的动漫"""
        resp = api_get("/api/anime/detail/99999")
        # 不存在的动漫可能返回空内容或非 JSON
        if resp.status_code == 200:
            text = resp.text.strip()
            if text:
                data = resp.json()
                # 可能是 {code: 500} 或空对象
                assert data.get("code") != 200 or data.get("id") is None
            # 空响应也视为"未找到"


class TestAnimeRanking:
    """排行榜相关"""

    def test_weekly_ranking(self, base_url):
        """测试: 周榜"""
        resp = api_get("/api/anime/ranking/weekly")
        data = _get_data(resp)
        assert isinstance(data, list)

    def test_monthly_ranking(self, base_url):
        """测试: 月榜"""
        resp = api_get("/api/anime/ranking/monthly")
        data = _get_data(resp)
        assert isinstance(data, list)

    def test_yearly_ranking(self, base_url):
        """测试: 年榜"""
        resp = api_get("/api/anime/ranking/yearly")
        data = _get_data(resp)
        assert isinstance(data, list)


class TestAnimeFilter:
    """筛选相关"""

    def test_search_anime(self, base_url):
        """测试: 搜索动漫"""
        resp = api_post("/api/anime/search", json_data={"keyword": "火影"})
        data = _get_data(resp)
        assert isinstance(data, list)

    def test_search_empty(self, base_url):
        """测试: 空关键词搜索"""
        resp = api_post("/api/anime/search", json_data={"keyword": ""})
        data = _get_data(resp)
        assert isinstance(data, list)

    def test_filter_by_year(self, base_url):
        """测试: 按年份筛选"""
        resp = api_get("/api/anime/year", params={"year": "2023"})
        data = _get_data(resp)
        assert isinstance(data, list)

    def test_filter_by_letter(self, base_url):
        """测试: 按字母筛选"""
        resp = api_get("/api/anime/letter/A")
        data = _get_data(resp)
        assert isinstance(data, list)


class TestAnimeComment:
    """动漫评论相关"""

    def test_get_anime_comments(self, base_url):
        """测试: 获取动漫评论列表"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            anime_id = data[0]["id"]
            resp = api_post("/api/anime/comment/list", json_data={"animeId": anime_id})
            comment_data = resp.json()
            assert comment_data["code"] == 200

    def test_add_anime_comment(self, base_url, auth_headers):
        """测试: 添加动漫评论"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            anime_id = data[0]["id"]
            resp = api_post("/api/anime/comment/add", headers=auth_headers, json_data={
                "animeId": anime_id,
                "content": "自动化测试评论",
                "username": "testuser"
            })
            comment_data = resp.json()
            assert comment_data["code"] == 200

    def test_get_comment_by_author(self, base_url):
        """测试: 按作者获取动漫评论"""
        resp = api_get("/api/anime/comment/author/1")
        data = resp.json()
        assert data["code"] == 200

    def test_get_comment_replies(self, base_url):
        """测试: 获取评论回复"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            anime_id = data[0]["id"]
            # 先获取评论列表，找到有评论的
            resp = api_post("/api/anime/comment/list", json_data={"animeId": anime_id})
            comments = resp.json()
            if comments["code"] == 200 and len(comments["data"]) > 0:
                parent_id = comments["data"][0]["id"]
                resp = api_post("/api/anime/comment/replies", json_data={
                    "animeId": anime_id,
                    "parentId": parent_id
                })
                replies = resp.json()
                assert replies["code"] == 200


class TestAnimeInteraction:
    """动漫评论互动相关"""

    def test_like_anime_comment(self, base_url, auth_headers):
        """测试: 点赞动漫评论"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            anime_id = data[0]["id"]
            resp = api_post("/api/anime/comment/list", json_data={"animeId": anime_id})
            comments = resp.json()
            if comments["code"] == 200 and len(comments["data"]) > 0:
                comment_id = comments["data"][0]["id"]
                resp = api_post("/api/anime/comment/like", headers=auth_headers, json_data={
                    "commentId": comment_id,
                    "username": "testuser"
                })
                like_data = resp.json()
                assert like_data["code"] in (200, 400)  # 200成功或400已点赞

    def test_dislike_anime_comment(self, base_url, auth_headers):
        """测试: 点踩动漫评论"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            anime_id = data[0]["id"]
            resp = api_post("/api/anime/comment/list", json_data={"animeId": anime_id})
            comments = resp.json()
            if comments["code"] == 200 and len(comments["data"]) > 0:
                comment_id = comments["data"][0]["id"]
                resp = api_post("/api/anime/comment/dislike", headers=auth_headers, json_data={
                    "commentId": comment_id,
                    "username": "testuser"
                })
                dislike_data = resp.json()
                assert dislike_data["code"] in (200, 400)  # 200成功或400已点踩


class TestAnimeExtra:
    """动漫扩展功能"""

    def test_get_anime_by_rating(self, base_url):
        """测试: 按评分排序获取动漫"""
        resp = api_get("/api/anime/rating")
        data = _get_data(resp)
        assert isinstance(data, list)

    def test_get_popular_anime(self, base_url):
        """测试: 获取热门动漫（按观看次数）"""
        resp = api_get("/api/anime/popular")
        data = _get_data(resp)
        assert isinstance(data, list)

    def test_get_anime_watch_count(self, base_url):
        """测试: 获取单个动漫观看次数"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            anime_id = data[0]["id"]
            resp = api_get(f"/api/anime/watch-count/{anime_id}")
            wc = resp.json()
            assert "watchCount" in wc

    def test_get_all_watch_counts(self, base_url):
        """测试: 批量获取所有动漫观看次数"""
        resp = api_get("/api/anime/watch-counts")
        data = resp.json()
        assert data["code"] == 200


class TestEpisode:
    """集数相关"""

    def test_get_episodes_by_anime(self, base_url):
        """测试: 获取动漫的所有集数"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            anime_id = data[0]["id"]
            resp = api_get(f"/api/episode/anime/{anime_id}")
            ep_data = resp.json()
            assert ep_data["code"] == 200
            assert isinstance(ep_data["data"], list)

    def test_get_specific_episode(self, base_url):
        """测试: 获取特定集数"""
        resp = api_get("/api/anime/list")
        data = _get_data(resp)
        if isinstance(data, list) and len(data) > 0:
            anime_id = data[0]["id"]
            ep_resp = api_get(f"/api/episode/anime/{anime_id}")
            ep_data = ep_resp.json()
            if ep_data["code"] == 200 and len(ep_data["data"]) > 0:
                ep_num = ep_data["data"][0]["episodeNumber"]
                resp = api_get(f"/api/episode/anime/{anime_id}/episode/{ep_num}")
                detail = resp.json()
                assert detail["code"] == 200


class TestRecommendation:
    """推荐相关"""

    def test_get_popular_anime(self, base_url):
        """测试: 获取热门动漫"""
        resp = api_get("/api/recommendation/popular")
        data = resp.json()
        assert data["code"] == 200
        assert isinstance(data["data"], list)

    def test_get_personalized_recommendations(self, base_url):
        """测试: 获取个性化推荐"""
        resp = api_post("/api/recommendation/personalized", json_data={
            "username": "testuser"
        })
        data = resp.json()
        assert data["code"] == 200