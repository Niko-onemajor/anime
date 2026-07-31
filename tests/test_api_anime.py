"""
动漫模块 API 测试
覆盖: 动漫列表、详情、排行、搜索、年份、字母筛选
"""
import pytest
from conftest import api_get, api_post, assert_code_200


class TestAnimeList:
    """动漫列表相关"""

    def test_get_anime_list(self, base_url):
        """测试: 获取动漫列表"""
        resp = api_get("/api/anime/list")
        data = assert_code_200(resp, "获取动漫列表")
        assert isinstance(data["data"], list)

    def test_get_anime_list_page(self, base_url):
        """测试: 分页获取动漫列表"""
        resp = api_get("/api/anime/list/page", params={"page": 0, "size": 8})
        data = assert_code_200(resp, "分页获取动漫")
        assert "content" in data  # Spring Data Page 格式

    def test_anime_list_not_empty(self, base_url):
        """测试: 动漫列表不为空"""
        resp = api_get("/api/anime/list")
        data = resp.json()
        if data["code"] == 200:
            assert len(data["data"]) > 0, "动漫列表为空，请先导入数据"


class TestAnimeDetail:
    """动漫详情相关"""

    def test_get_anime_detail(self, base_url):
        """测试: 获取动漫详情"""
        # 先获取列表中的第一个动漫 ID
        resp = api_get("/api/anime/list")
        data = resp.json()
        if data["code"] == 200 and len(data["data"]) > 0:
            anime_id = data["data"][0]["id"]
            resp = api_get(f"/api/anime/detail/{anime_id}")
            detail = assert_code_200(resp, f"获取动漫详情 id={anime_id}")
            assert detail["data"]["id"] == anime_id

    def test_anime_detail_not_found(self, base_url):
        """测试: 获取不存在的动漫"""
        resp = api_get("/api/anime/detail/99999")
        data = resp.json()
        assert data["code"] != 200


class TestAnimeRanking:
    """排行榜相关"""

    def test_weekly_ranking(self, base_url):
        """测试: 周榜"""
        resp = api_get("/api/anime/ranking/weekly")
        data = assert_code_200(resp, "周榜")
        assert isinstance(data["data"], list)

    def test_monthly_ranking(self, base_url):
        """测试: 月榜"""
        resp = api_get("/api/anime/ranking/monthly")
        data = assert_code_200(resp, "月榜")
        assert isinstance(data["data"], list)

    def test_yearly_ranking(self, base_url):
        """测试: 年榜"""
        resp = api_get("/api/anime/ranking/yearly")
        data = assert_code_200(resp, "年榜")
        assert isinstance(data["data"], list)


class TestAnimeFilter:
    """筛选相关"""

    def test_search_anime(self, base_url):
        """测试: 搜索动漫"""
        resp = api_post("/api/anime/search", json_data={"keyword": "火影"})
        data = resp.json()
        assert data["code"] == 200

    def test_search_empty(self, base_url):
        """测试: 空关键词搜索"""
        resp = api_post("/api/anime/search", json_data={"keyword": ""})
        data = resp.json()
        assert data["code"] == 200

    def test_filter_by_year(self, base_url):
        """测试: 按年份筛选"""
        resp = api_get("/api/anime/year", params={"year": "2023"})
        data = resp.json()
        assert data["code"] == 200

    def test_filter_by_letter(self, base_url):
        """测试: 按字母筛选"""
        resp = api_get("/api/anime/letter/A")
        data = resp.json()
        assert data["code"] == 200


class TestAnimeComment:
    """动漫评论相关"""

    def test_get_anime_comments(self, base_url):
        """测试: 获取动漫评论列表"""
        resp = api_get("/api/anime/list")
        data = resp.json()
        if data["code"] == 200 and len(data["data"]) > 0:
            anime_id = data["data"][0]["id"]
            resp = api_post("/api/anime/comment/list", json_data={"animeId": anime_id})
            comment_data = resp.json()
            assert comment_data["code"] == 200

    def test_add_anime_comment(self, base_url, auth_headers):
        """测试: 添加动漫评论"""
        resp = api_get("/api/anime/list")
        data = resp.json()
        if data["code"] == 200 and len(data["data"]) > 0:
            anime_id = data["data"][0]["id"]
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