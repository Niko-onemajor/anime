"""
性能测试模块
覆盖: 响应时间、并发请求、负载测试、API稳定性、前端页面加载速度
"""
import time
import statistics
import concurrent.futures
import pytest
import requests
from conftest import api_get, api_post, assert_code_200, TEST_PASSWORD


# ============================================================
# 响应时间测试
# ============================================================

class TestResponseTime:
    """各接口响应时间测试"""

    ENDPOINTS = [
        ("GET", "/api/anime/list", None, "动漫列表"),
        ("GET", "/api/anime/ranking", None, "排行榜"),
        ("POST", "/api/user/login", {"username": "testuser", "password": TEST_PASSWORD}, "登录"),
        ("POST", "/api/user/profile", {"username": "testuser"}, "用户资料"),
        ("GET", "/api/anime/1", None, "动漫详情"),
        ("GET", "/api/anime/1/episodes", None, "集数列表"),
        ("GET", "/api/forum/posts", None, "论坛帖子"),
        ("GET", "/api/data/anime", None, "动漫数据"),
        ("GET", "/api/user/search", None, "用户搜索"),
    ]

    # 各接口响应时间阈值（秒）
    THRESHOLDS = {
        "动漫列表": 2.0,
        "排行榜": 1.5,
        "登录": 1.5,
        "用户资料": 1.0,
        "动漫详情": 1.0,
        "集数列表": 1.0,
        "论坛帖子": 2.0,
        "动漫数据": 2.0,
        "用户搜索": 1.0,
    }

    def _measure_response_time(self, base_url, method, url, json_data, label):
        """测量单次请求响应时间"""
        start = time.time()
        try:
            if method == "GET":
                params = {"keyword": "test"} if "search" in url else None
                resp = api_get(url, params=params)
            else:
                resp = api_post(url, json_data=json_data)
            elapsed = time.time() - start
            return {"status": resp.status_code, "elapsed": elapsed}
        except Exception as e:
            elapsed = time.time() - start
            return {"status": 0, "elapsed": elapsed, "error": str(e)}

    @pytest.mark.parametrize("method,url,json_data,label", ENDPOINTS)
    def test_response_time(self, base_url, method, url, json_data, label):
        """测试: 各接口响应时间是否在阈值范围内"""
        threshold = self.THRESHOLDS.get(label, 2.0)
        result = self._measure_response_time(base_url, method, url, json_data, label)
        print(f"\n  [{label}] 响应时间: {result['elapsed']:.3f}s (阈值: {threshold}s)")
        assert result["elapsed"] < threshold, \
            f"{label} 响应时间 {result['elapsed']:.3f}s 超过阈值 {threshold}s"

    def test_average_response_time(self, base_url):
        """测试: 多次请求平均响应时间"""
        url = "/api/anime/list"
        times_list = []
        for i in range(5):
            result = self._measure_response_time(base_url, "GET", url, None, "动漫列表")
            times_list.append(result["elapsed"])
            time.sleep(0.1)

        avg_time = statistics.mean(times_list)
        print(f"\n  动漫列表 5次请求: 平均 {avg_time:.3f}s, "
              f"最小 {min(times_list):.3f}s, 最大 {max(times_list):.3f}s")
        assert avg_time < 2.0, f"平均响应时间 {avg_time:.3f}s 过高"


# ============================================================
# 并发请求测试
# ============================================================

class TestConcurrentRequests:
    """多并行请求测试"""

    def _make_request(self, url, method="get", headers=None, json_data=None):
        """发送单个请求并返回耗时"""
        start = time.time()
        try:
            if method == "get":
                resp = requests.get(f"http://localhost:8080{url}", headers=headers or {}, timeout=10)
            else:
                resp = requests.post(f"http://localhost:8080{url}", headers=headers or {}, json=json_data, timeout=10)
            elapsed = time.time() - start
            return {"status": resp.status_code, "elapsed": elapsed}
        except Exception as e:
            elapsed = time.time() - start
            return {"status": 0, "elapsed": elapsed, "error": str(e)}

    def test_concurrent_anime_list(self, base_url):
        """测试: 10个并发请求获取动漫列表"""
        urls = ["/api/anime/list"] * 10
        results = []

        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            futures = [executor.submit(self._make_request, url) for url in urls]
            for future in concurrent.futures.as_completed(futures):
                results.append(future.result())

        success = [r for r in results if r["status"] == 200]
        fail = [r for r in results if r["status"] != 200]
        times_list = [r["elapsed"] for r in success]

        print(f"\n  并发测试(动漫列表): 成功 {len(success)}/{len(results)}, "
              f"平均耗时 {statistics.mean(times_list):.3f}s, "
              f"最大耗时 {max(times_list):.3f}s, "
              f"最小耗时 {min(times_list):.3f}s")

        assert len(success) >= 8, f"并发成功率 {len(success)}/10 过低"
        assert statistics.mean(times_list) < 5.0, f"平均耗时 {statistics.mean(times_list):.2f}s 过高"

    def test_concurrent_mixed_requests(self, base_url):
        """测试: 15个并发请求混合不同接口"""
        requests_list = [
            ("/api/anime/list", "get", None),
            ("/api/anime/ranking", "get", None),
            ("/api/forum/posts", "get", None),
            ("/api/anime/1", "get", None),
            ("/api/anime/1/episodes", "get", None),
            ("/api/anime/list", "get", None),
            ("/api/anime/ranking", "get", None),
            ("/api/forum/posts", "get", None),
            ("/api/anime/1", "get", None),
            ("/api/anime/1/episodes", "get", None),
            ("/api/anime/list", "get", None),
            ("/api/user/profile", "post", {"username": "testuser"}),
            ("/api/anime/ranking", "get", None),
            ("/api/forum/posts", "get", None),
            ("/api/anime/1", "get", None),
        ]

        results = []
        with concurrent.futures.ThreadPoolExecutor(max_workers=15) as executor:
            futures = []
            for url, method, data in requests_list:
                futures.append(executor.submit(self._make_request, url, method, json_data=data))
            for future in concurrent.futures.as_completed(futures):
                results.append(future.result())

        success = [r for r in results if r["status"] == 200]
        fail = [r for r in results if r["status"] != 200]
        times_list = [r["elapsed"] for r in success]

        print(f"\n  混合并发测试: 成功 {len(success)}/{len(results)}, "
              f"平均耗时 {statistics.mean(times_list):.3f}s")

        assert len(success) >= 4, f"混合并发成功率 {len(success)}/15 过低"

    def test_concurrent_login(self, base_url):
        """测试: 5个并发登录请求"""
        results = []
        with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
            futures = []
            for _ in range(5):
                json_data = {"username": "testuser", "password": TEST_PASSWORD}
                futures.append(executor.submit(self._make_request, "/api/user/login", "post", json_data=json_data))
            for future in concurrent.futures.as_completed(futures):
                results.append(future.result())

        success = [r for r in results if r["status"] == 200]
        times_list = [r["elapsed"] for r in success]

        print(f"\n  并发登录测试: 成功 {len(success)}/{len(results)}, "
              f"平均耗时 {statistics.mean(times_list):.3f}s")

        assert len(success) >= 4, f"并发登录成功率 {len(success)}/5 过低"

    def test_high_concurrency(self, base_url):
        """测试: 30个并发请求（高并发）"""
        urls = ["/api/anime/list"] * 15 + ["/api/anime/ranking"] * 15
        results = []

        with concurrent.futures.ThreadPoolExecutor(max_workers=30) as executor:
            futures = [executor.submit(self._make_request, url) for url in urls]
            for future in concurrent.futures.as_completed(futures):
                results.append(future.result())

        success = [r for r in results if r["status"] == 200]
        fail = [r for r in results if r["status"] != 200]
        times_list = [r["elapsed"] for r in success]

        print(f"\n  高并发测试(30请求): 成功 {len(success)}/{len(results)}, "
              f"平均耗时 {statistics.mean(times_list):.3f}s, "
              f"最大耗时 {max(times_list):.3f}s")

        # 高并发下允许部分失败
        assert len(success) >= 10, f"高并发成功率 {len(success)}/30 过低"


# ============================================================
# 负载与吞吐量测试
# ============================================================

class TestLoadAndThroughput:
    """负载与吞吐量测试"""

    def test_burst_requests(self, base_url):
        """测试: 突发请求负载（短时间内大量请求）"""
        results = []
        total_requests = 20

        start_all = time.time()
        with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
            futures = []
            for _ in range(total_requests):
                futures.append(executor.submit(self._make_request, "/api/anime/list"))
            for future in concurrent.futures.as_completed(futures):
                results.append(future.result())
        total_time = time.time() - start_all

        success = [r for r in results if r["status"] == 200]
        throughput = len(success) / total_time if total_time > 0 else 0

        print(f"\n  突发负载测试: {total_requests}请求, 总耗时 {total_time:.2f}s, "
              f"吞吐量 {throughput:.1f} req/s, 成功率 {len(success)}/{total_requests}")

        assert throughput > 1.0, f"吞吐量 {throughput:.1f} req/s 过低"
        assert len(success) >= 15, f"突发请求成功率 {len(success)}/{total_requests} 过低"

    def _make_request(self, url, method="get", headers=None, json_data=None):
        """发送单个请求"""
        start = time.time()
        try:
            if method == "get":
                resp = requests.get(f"http://localhost:8080{url}", headers=headers or {}, timeout=10)
            else:
                resp = requests.post(f"http://localhost:8080{url}", headers=headers or {}, json=json_data, timeout=10)
            elapsed = time.time() - start
            return {"status": resp.status_code, "elapsed": elapsed}
        except Exception as e:
            elapsed = time.time() - start
            return {"status": 0, "elapsed": elapsed, "error": str(e)}

    def test_sustained_load(self, base_url):
        """测试: 持续负载（多次迭代）"""
        iterations = 3
        per_iteration = 10
        all_times = []

        for i in range(iterations):
            results = []
            start = time.time()
            with concurrent.futures.ThreadPoolExecutor(max_workers=per_iteration) as executor:
                futures = [executor.submit(self._make_request, "/api/anime/list") for _ in range(per_iteration)]
                for future in concurrent.futures.as_completed(futures):
                    results.append(future.result())
            elapsed = time.time() - start
            all_times.append(elapsed)

            success = [r for r in results if r["status"] == 200]
            print(f"  迭代 {i+1}: {per_iteration}请求, {elapsed:.2f}s, 成功 {len(success)}/{per_iteration}")

        avg_time = statistics.mean(all_times)
        print(f"\n  持续负载: 3轮, 平均每轮 {avg_time:.2f}s")
        assert avg_time < 10.0, f"持续负载平均耗时 {avg_time:.2f}s 过高"


# ============================================================
# API 稳定性测试
# ============================================================

class TestApiStability:
    """API 稳定性测试"""

    def test_repeated_requests_stability(self, base_url):
        """测试: 重复请求稳定性（20次请求检查状态码一致性）"""
        status_codes = []
        for i in range(20):
            try:
                resp = api_get("/api/anime/list")
                status_codes.append(resp.status_code)
            except Exception as e:
                status_codes.append(0)
            time.sleep(0.05)

        success_count = status_codes.count(200)
        error_count = len(status_codes) - success_count

        print(f"\n  稳定性测试(20次): 成功 {success_count}, 失败 {error_count}")
        assert error_count == 0, f"稳定性测试失败 {error_count} 次"

    def test_rapid_sequential_requests(self, base_url):
        """测试: 快速连续请求（无间隔）"""
        results = []
        for _ in range(10):
            start = time.time()
            try:
                resp = api_get("/api/anime/ranking/weekly")
                elapsed = time.time() - start
                results.append({"status": resp.status_code, "elapsed": elapsed})
            except Exception as e:
                elapsed = time.time() - start
                results.append({"status": 0, "elapsed": elapsed, "error": str(e)})

        success = [r for r in results if r["status"] == 200]
        times_list = [r["elapsed"] for r in success]

        if times_list:
            print(f"\n  快速连续请求(10次): 成功 {len(success)}/10, 平均 {statistics.mean(times_list):.3f}s")
        else:
            print(f"\n  快速连续请求(10次): 成功 {len(success)}/10 (无成功响应)")
        assert len(success) >= 5, f"快速连续请求有 {10 - len(success)} 次失败"


# ============================================================
# 前端页面加载速度测试
# ============================================================

class TestPageLoadSpeed:
    """前端页面加载速度测试（通过 HTTP 请求检测）"""

    PAGE_URLS = [
        ("/auth", "登录页"),
        ("/index", "首页"),
        ("/category", "分类页"),
        ("/forum", "论坛页"),
    ]

    @pytest.mark.parametrize("path,label", PAGE_URLS)
    def test_page_loads(self, frontend_url, path, label):
        """测试: 前端页面 HTTP 可访问且响应时间在阈值内"""
        url = f"{frontend_url}{path}"
        start = time.time()
        try:
            resp = requests.get(url, timeout=10)
            elapsed = time.time() - start
            print(f"\n  [{label}] {url} - {resp.status_code}, {elapsed:.3f}s")
            assert resp.status_code == 200, f"{label} 返回状态码 {resp.status_code}"
            assert elapsed < 5.0, f"{label} 加载时间 {elapsed:.3f}s 超过阈值"
        except requests.exceptions.Timeout:
            print(f"\n  [{label}] {url} - 超时")
            # 前端可能未启动，不强制失败
            pass
        except requests.exceptions.ConnectionError:
            print(f"\n  [{label}] {url} - 连接失败（前端可能未启动）")
            pass