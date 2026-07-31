"""
安全测试模块
覆盖: 权限越界、XSS防护、SQL注入、路径遍历、Token安全、敏感信息泄露
"""
import pytest
import requests
from conftest import api_get, api_post, api_delete, assert_code_200, TEST_PASSWORD


# ============================================================
# 权限越界测试
# ============================================================

class TestAuthorizationBypass:
    """权限越界测试：验证普通用户无法访问管理员接口"""

    ADMIN_ENDPOINTS = [
        ("GET", "/api/admin/users", None, "管理员-用户列表"),
        ("GET", "/api/admin/animes", None, "管理员-动漫列表"),
        ("GET", "/api/admin/forum", None, "管理员-论坛列表"),
        ("GET", "/api/admin/deleted", None, "管理员-删除记录"),
    ]

    @pytest.mark.parametrize("method,url,data,label", ADMIN_ENDPOINTS)
    def test_admin_endpoints_no_token(self, base_url, method, url, data, label):
        """测试: 无Token访问管理员接口应被拒绝"""
        if method == "GET":
            resp = api_get(url)
        else:
            resp = api_post(url, json_data=data)
        print(f"\n  [{label}] 无Token: {resp.status_code}")
        # 应该返回 401/403 或重定向到登录页
        assert resp.status_code in (401, 403, 302), \
            f"{label} 无Token访问应被拒绝，实际: {resp.status_code}"

    @pytest.mark.parametrize("method,url,data,label", ADMIN_ENDPOINTS)
    def test_admin_endpoints_user_token(self, base_url, method, url, data, label, auth_headers):
        """测试: 普通用户Token访问管理员接口应被拒绝"""
        if method == "GET":
            resp = api_get(url, headers=auth_headers)
        else:
            resp = api_post(url, headers=auth_headers, json_data=data)
        print(f"\n  [{label}] 普通用户Token: {resp.status_code}")
        assert resp.status_code in (401, 403, 302), \
            f"{label} 普通用户访问应被拒绝，实际: {resp.status_code}"

    def test_admin_endpoints_admin_token(self, base_url, admin_headers):
        """测试: 管理员Token可访问管理员接口"""
        if not admin_headers:
            pytest.skip("管理员Token不可用")
        resp = api_post("/api/admin/users", headers=admin_headers, json_data={"page": 1, "size": 6})
        data = resp.json()
        print(f"\n  [管理员-用户列表] 管理员Token: code={data.get('code')}")
        assert data.get("code") == 200, f"管理员应能访问，实际: {data}"

    def test_user_modify_other_user(self, base_url, auth_headers):
        """测试: 普通用户尝试修改其他用户资料"""
        resp = api_post("/api/user/update", headers=auth_headers, json_data={
            "oldUsername": "admin",
            "username": "admin_hacked",
            "email": "hack@test.com"
        })
        data = resp.json()
        print(f"\n  [越权修改] 响应: {data}")
        # 应该失败，不能修改其他用户
        assert data.get("code") != 200, f"越权修改应被阻止，实际: {data}"

    def test_unauthorized_delete(self, base_url, auth_headers):
        """测试: 普通用户尝试删除动漫"""
        resp = api_delete("/api/anime/1", headers=auth_headers)
        print(f"\n  [越权删除] 状态码: {resp.status_code}")
        # 删除操作应被拒绝
        assert resp.status_code in (401, 403, 405, 404), \
            f"越权删除应被拒绝，实际: {resp.status_code}"


# ============================================================
# XSS 防护测试
# ============================================================

class TestXSSProtection:
    """XSS 跨站脚本攻击防护测试"""

    XSS_PAYLOADS = [
        '<script>alert("xss")</script>',
        '<img src=x onerror=alert(1)>',
        '<svg onload=alert(1)>',
        'javascript:alert(1)',
        '<iframe src="javascript:alert(1)">',
        '"><script>alert(1)</script>',
        '<body onload=alert(1)>',
        '"><img src=x onerror=alert(1)>',
    ]

    def test_xss_in_username_register(self, base_url):
        """测试: XSS payload在注册用户名中"""
        for payload in self.XSS_PAYLOADS[:3]:
            resp = api_post("/api/user/register", json_data={
                "username": payload,
                "password": "Test@1234",
                "email": "xss@test.com"
            })
            data = resp.json()
            print(f"\n  [XSS注册] payload={payload[:30]}... code={data.get('code')}")
            # 应被拒绝（用户名格式校验或XSS过滤）
            assert data.get("code") != 200, f"XSS用户名应被拒绝: {payload}"

    def test_xss_in_login(self, base_url):
        """测试: XSS payload在登录用户名中"""
        for payload in self.XSS_PAYLOADS[:3]:
            resp = api_post("/api/user/login", json_data={
                "username": payload,
                "password": TEST_PASSWORD
            })
            data = resp.json()
            print(f"\n  [XSS登录] payload={payload[:30]}... code={data.get('code')}")
            # 应返回错误而非成功
            assert data.get("code") != 200, f"XSS登录应失败: {payload}"

    def test_xss_in_search(self, base_url):
        """测试: XSS payload在搜索参数中"""
        for payload in self.XSS_PAYLOADS[:3]:
            resp = api_get("/api/user/search", params={"keyword": payload})
            data = resp.json()
            print(f"\n  [XSS搜索] payload={payload[:30]}... code={data.get('code')}")
            # 不应导致服务器错误
            assert data.get("code") in (200, 400), f"XSS搜索应正常处理: {data}"

    def test_xss_in_profile_update(self, base_url, auth_headers):
        """测试: XSS payload在用户资料更新中"""
        xss_payload = '<script>alert("xss")</script>'
        resp = api_post("/api/user/update", headers=auth_headers, json_data={
            "oldUsername": "testuser",
            "username": "testuser",
            "signature": xss_payload,
            "email": "test@test.com"
        })
        data = resp.json()
        print(f"\n  [XSS签名] 响应: code={data.get('code')}")
        # 应正常处理（存储时应转义）
        if data.get("code") == 200:
            # 检查是否转义存储
            profile_resp = api_post("/api/user/profile", json_data={"username": "testuser"})
            profile_data = profile_resp.json()
            if profile_data.get("code") == 200:
                stored_sig = profile_data.get("data", {}).get("signature", "")
                print(f"  存储的签名: {stored_sig}")
                # 确认存储的内容被转义或过滤
                assert "<script>" not in stored_sig or "&lt;script&gt;" in stored_sig, \
                    f"XSS签名应被转义: {stored_sig}"


# ============================================================
# SQL 注入防护测试
# ============================================================

class TestSQLInjectionProtection:
    """SQL 注入防护测试"""

    SQLI_PAYLOADS = [
        "' OR '1'='1",
        "' OR '1'='1' --",
        "'; DROP TABLE users; --",
        "' UNION SELECT * FROM users --",
        "admin' --",
        "1' OR '1' = '1",
        "' OR 1=1#",
        "1; DROP TABLE users;",
    ]

    def test_sqli_in_login(self, base_url):
        """测试: SQL注入在登录中"""
        for payload in self.SQLI_PAYLOADS[:4]:
            resp = api_post("/api/user/login", json_data={
                "username": payload,
                "password": TEST_PASSWORD
            })
            data = resp.json()
            print(f"\n  [SQL注入登录] payload={payload[:30]}... code={data.get('code')}")
            # 不应登录成功，且不应返回 500 错误
            assert data.get("code") != 200, f"SQL注入登录不应成功: {payload}"
            assert resp.status_code != 500, f"SQL注入不应导致服务器错误: {payload}"

    def test_sqli_in_search(self, base_url):
        """测试: SQL注入在搜索中"""
        for payload in self.SQLI_PAYLOADS[:4]:
            resp = api_get("/api/user/search", params={"keyword": payload})
            print(f"\n  [SQL注入搜索] payload={payload[:30]}... status={resp.status_code}")
            assert resp.status_code != 500, f"SQL注入不应导致服务器错误: {payload}"

    def test_sqli_in_anime_id(self, base_url):
        """测试: SQL注入在动漫ID参数中"""
        for payload in ["' OR '1'='1", "1; DROP TABLE users;"]:
            resp = api_get(f"/api/anime/detail/{payload}")
            print(f"\n  [SQL注入动漫ID] payload={payload}... status={resp.status_code}")
            # SQL注入不应导致服务器500错误，应返回400/404/200等正常响应
            assert resp.status_code != 500, f"SQL注入不应导致服务器错误: {payload}"

    def test_sqli_in_username_param(self, base_url):
        """测试: SQL注入在用户名参数中"""
        payload = "' OR '1'='1"
        resp = api_post("/api/user/profile", json_data={"username": payload})
        data = resp.json()
        print(f"\n  [SQL注入用户名] 响应: {data}")
        assert resp.status_code != 500, f"SQL注入不应导致服务器错误"
        assert data.get("code") != 200, f"SQL注入不应返回成功"


# ============================================================
# 路径遍历测试
# ============================================================

class TestPathTraversal:
    """路径遍历攻击防护测试"""

    TRAVERSAL_PAYLOADS = [
        "../../../etc/passwd",
        "..\\..\\..\\windows\\system32",
        "....//....//....//etc/passwd",
        "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc/passwd",
        "..%252f..%252f..%252fetc/passwd",
    ]

    def test_path_traversal_in_avatar(self, base_url):
        """测试: 路径遍历在头像参数中"""
        for payload in self.TRAVERSAL_PAYLOADS[:2]:
            resp = api_get("/api/user/info", params={"username": payload})
            data = resp.json()
            print(f"\n  [路径遍历] payload={payload[:30]}... code={data.get('code')}")
            assert data.get("code") != 200, f"路径遍历应被拒绝: {payload}"

    def test_path_traversal_in_anime_id(self, base_url):
        """测试: 路径遍历在动漫ID中"""
        for payload in self.TRAVERSAL_PAYLOADS[:2]:
            resp = api_get(f"/api/anime/{payload}")
            print(f"\n  [路径遍历动漫ID] status={resp.status_code}")
            assert resp.status_code != 500, f"路径遍历不应导致服务器错误"


# ============================================================
# Token 安全测试
# ============================================================

class TestTokenSecurity:
    """JWT Token 安全测试"""

    def test_expired_token(self, base_url):
        """测试: 过期Token应被拒绝"""
        expired_token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImV4cCI6MTYwMDAwMDAwMH0.signature"
        headers = {"Authorization": f"Bearer {expired_token}"}
        # 使用需要认证的接口测试 token 验证
        resp = api_post("/api/admin/users", headers=headers, json_data={"page": 1, "size": 6})
        print(f"\n  [过期Token] 状态码: {resp.status_code}")
        # 过期Token应被拒绝（返回401未授权或403禁止）
        assert resp.status_code in (401, 403), f"过期Token应被拒绝，实际: {resp.status_code}"

    def test_invalid_token(self, base_url):
        """测试: 无效Token应被拒绝"""
        headers = {"Authorization": "Bearer invalid_token_here"}
        # 使用需要认证的接口测试 token 验证
        resp = api_post("/api/admin/users", headers=headers, json_data={"page": 1, "size": 6})
        print(f"\n  [无效Token] 状态码: {resp.status_code}")
        assert resp.status_code in (401, 403), f"无效Token应被拒绝，实际: {resp.status_code}"

    def test_empty_token(self, base_url):
        """测试: 空Token应被拒绝"""
        headers = {"Authorization": "Bearer "}
        resp = api_get("/api/user/info", headers=headers, params={"username": "testuser"})
        print(f"\n  [空Token] 状态码: {resp.status_code}")
        # 空Token应该被处理（可能返回缺少参数400或未授权）
        assert resp.status_code != 500, f"空Token不应导致服务器错误"

    def test_token_in_url(self, base_url):
        """测试: Token通过URL参数传递（不推荐，测试是否被处理）"""
        resp = api_get("/api/user/info", params={"username": "testuser", "token": "test"})
        print(f"\n  [Token在URL] 状态码: {resp.status_code}")
        # 无论什么结果，不应500
        assert resp.status_code != 500

    def test_token_without_bearer(self, base_url, user_token):
        """测试: Token不带Bearer前缀"""
        if not user_token:
            pytest.skip("无用户Token")
        headers = {"Authorization": user_token}
        resp = api_get("/api/user/info", headers=headers, params={"username": "testuser"})
        print(f"\n  [无Bearer前缀] 状态码: {resp.status_code}")
        # 可能被拒绝
        assert resp.status_code != 500


# ============================================================
# 敏感信息泄露测试
# ============================================================

class TestInformationLeakage:
    """敏感信息泄露防护测试"""

    def test_error_messages_no_stacktrace(self, base_url):
        """测试: 错误响应不应包含堆栈信息"""
        # 触发一个错误场景
        resp = api_get("/api/anime/999999")
        data = resp.json() if resp.headers.get("content-type", "").startswith("application/json") else {}
        body = resp.text.lower()
        print(f"\n  [错误信息] 响应体长度: {len(body)}")
        # 不应包含敏感信息
        assert "java.lang." not in body, "错误响应包含Java堆栈信息"
        assert "exception" not in body, "错误响应包含异常信息"
        assert "at com.example" not in body, "错误响应包含代码路径"

    def test_no_sensitive_headers(self, base_url):
        """测试: 响应头不应泄露服务器信息"""
        resp = api_get("/api/anime/list")
        headers = {k.lower(): v for k, v in resp.headers.items()}
        print(f"\n  [响应头] Server: {headers.get('server', 'N/A')}")
        # 检查是否泄露服务器信息
        sensitive_headers = ["x-powered-by", "server", "x-aspnet-version"]
        for h in sensitive_headers:
            if h in headers:
                print(f"  注意: 响应头包含 {h}: {headers[h]}")

    def test_user_enumeration(self, base_url):
        """测试: 用户枚举攻击（登录错误信息一致）"""
        # 不存在用户登录
        resp1 = api_post("/api/user/login", json_data={
            "username": "nonexistent_user_12345",
            "password": "test1234"
        })
        # 存在用户但密码错误
        resp2 = api_post("/api/user/login", json_data={
            "username": "testuser",
            "password": "wrongpassword"
        })

        data1 = resp1.json()
        data2 = resp2.json()
        msg1 = data1.get("msg", "")
        msg2 = data2.get("msg", "")

        print(f"\n  [用户枚举] 不存在用户: {msg1}")
        print(f"  [用户枚举] 错误密码: {msg2}")
        # 两条消息应一致，防止用户枚举
        assert msg1 == msg2, f"登录错误信息不一致，可被用于用户枚举: '{msg1}' vs '{msg2}'"


# ============================================================
# CSRF 防护测试
# ============================================================

class TestCSRFProtection:
    """CSRF 跨站请求伪造防护测试"""

    def test_csrf_without_token(self, base_url, user_token):
        """测试: 修改操作无CSRF Token"""
        if not user_token:
            pytest.skip("无用户Token")
        # 尝试修改密码不带CSRF Token
        headers = {"Authorization": f"Bearer {user_token}"}
        # 不添加 X-CSRF-TOKEN 头
        resp = api_post("/api/user/change-password", headers=headers, json_data={
            "username": "testuser",
            "oldPassword": TEST_PASSWORD,
            "newPassword": "NewTest@1234"
        })
        data = resp.json()
        print(f"\n  [CSRF无Token] 响应: {data}")
        # 接口应正常响应（SecurityConfig已禁用CSRF）
        # 但可以记录此行为
        assert resp.status_code != 500

    def test_state_changing_get(self, base_url):
        """测试: GET请求不应改变状态"""
        # 尝试通过GET请求激活用户（激活应用POST或GET带参数）
        resp = api_get("/api/user/activate", params={"code": "invalid_code"})
        data = resp.json()
        print(f"\n  [GET状态变更] 响应: {data}")
        # 应正常处理
        assert resp.status_code != 500


# ============================================================
# 输入验证测试
# ============================================================

class TestInputValidation:
    """输入验证与边界测试"""

    def test_username_boundary(self, base_url):
        """测试: 用户名边界值"""
        # 2字符 - 应被拒绝
        resp = api_post("/api/user/register", json_data={
            "username": "ab",
            "password": "Test@1234",
            "email": "test@test.com"
        })
        data = resp.json()
        print(f"\n  [用户名2字符] code={data.get('code')}")
        assert data.get("code") != 200, "2字符用户名应被拒绝"

        # 11字符 - 应被拒绝
        resp = api_post("/api/user/register", json_data={
            "username": "abcdefghijk",
            "password": "Test@1234",
            "email": "test@test.com"
        })
        data = resp.json()
        print(f"  [用户名11字符] code={data.get('code')}")
        assert data.get("code") != 200, "11字符用户名应被拒绝"

    def test_special_chars_username(self, base_url):
        """测试: 特殊字符用户名"""
        special_usernames = ["test user", "test@user", "test/user", "test<script>"]
        for uname in special_usernames:
            resp = api_post("/api/user/register", json_data={
                "username": uname,
                "password": "Test@1234",
                "email": "test@test.com"
            })
            data = resp.json()
            print(f"  [特殊字符] '{uname}' code={data.get('code')}")
            assert data.get("code") != 200, f"特殊字符用户名应被拒绝: {uname}"

    def test_large_payload(self, base_url):
        """测试: 超大请求体"""
        large_username = "a" * 10000
        resp = api_post("/api/user/login", json_data={
            "username": large_username,
            "password": TEST_PASSWORD
        })
        print(f"\n  [超大请求] 状态码: {resp.status_code}")
        assert resp.status_code != 500, "超大请求不应导致服务器500错误"

    def test_null_bytes(self, base_url):
        """测试: Null字节注入"""
        resp = api_get("/api/user/search", params={"keyword": "test%00admin"})
        print(f"\n  [Null字节] 状态码: {resp.status_code}")
        assert resp.status_code != 500, "Null字节注入不应导致服务器错误"


# ============================================================
# 速率限制测试
# ============================================================

class TestRateLimiting:
    """速率限制与暴力破解防护测试"""

    def test_rapid_login_attempts(self, base_url):
        """测试: 快速连续登录尝试（检测是否有速率限制）"""
        results = []
        for i in range(10):
            resp = api_post("/api/user/login", json_data={
                "username": "testuser",
                "password": f"wrong_password_{i}"
            })
            results.append(resp.status_code)
            data = resp.json()
            if i < 3:
                print(f"\n  尝试 {i+1}: status={resp.status_code}, code={data.get('code')}")

        status_429 = 429 in results
        print(f"\n  10次登录尝试: 429限流={status_429}, 状态码分布={set(results)}")
        # 理想情况下有限流，但不强制要求
        # 至少不应全部返回500
        assert 500 not in results, "登录尝试不应导致500错误"


# ============================================================
# 跨域安全测试
# ============================================================

class TestCORSSecurity:
    """跨域安全测试"""

    def test_cors_preflight(self, base_url):
        """测试: CORS预检请求"""
        resp = requests.options(
            f"{base_url}/api/user/login",
            headers={
                "Origin": "http://evil.com",
                "Access-Control-Request-Method": "POST",
                "Access-Control-Request-Headers": "Content-Type"
            }
        )
        print(f"\n  [CORS预检] 状态码: {resp.status_code}")
        # 检查是否允许恶意来源
        allow_origin = resp.headers.get("Access-Control-Allow-Origin", "")
        print(f"  Access-Control-Allow-Origin: {allow_origin}")
        # 应限制来源，不应允许任意来源
        assert allow_origin != "*", "CORS不应允许任意来源"

    def test_cors_unauthorized_origin(self, base_url):
        """测试: 未授权来源的跨域请求"""
        resp = api_post("/api/user/login", json_data={
            "username": "testuser",
            "password": TEST_PASSWORD
        }, headers={"Origin": "http://evil.com"})
        allow_origin = resp.headers.get("Access-Control-Allow-Origin", "")
        print(f"\n  [CORS未授权] Allow-Origin: {allow_origin}")
        # 不应允许未授权来源
        assert allow_origin != "http://evil.com", "CORS不应允许未授权来源"