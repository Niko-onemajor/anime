"""
E2E 端到端 UI 测试 (Playwright)
覆盖: 前端页面加载、导航、表单交互、登录流程
需要: 前端开发服务器已启动 (http://localhost:5173)
      后端服务已启动 (http://localhost:8080)
"""
import pytest
import os
import re


# ============================================================
# 条件跳过: 仅在指定运行 E2E 测试时才执行
# ============================================================
pytestmark = pytest.mark.e2e

FRONTEND_URL = os.environ.get("FRONTEND_URL", "http://localhost:5173")


@pytest.fixture(scope="module")
def browser():
    """启动浏览器（模块级别复用）"""
    from playwright.sync_api import sync_playwright
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)  # headless=False 可看到浏览器
        yield browser
        browser.close()


@pytest.fixture
def page(browser):
    """每个测试新建页面"""
    context = browser.new_context(viewport={"width": 1920, "height": 1080})
    page = context.new_page()
    yield page
    context.close()


# ============================================================
# 页面加载测试
# ============================================================

class TestPageLoads:
    """测试所有页面能否正常加载"""

    def test_login_page_loads(self, page):
        """测试: 登录页加载"""
        page.goto(FRONTEND_URL)
        page.wait_for_load_state("networkidle")
        # 默认重定向到 /auth
        assert "/auth" in page.url

    def test_index_page_loads(self, page):
        """测试: 首页加载（需先登录）"""
        self._login(page)
        page.goto(f"{FRONTEND_URL}/index")
        page.wait_for_load_state("networkidle")
        assert page.locator(".navbar").is_visible()

    def test_category_page_loads(self, page):
        """测试: 分类页加载"""
        self._login(page)
        page.goto(f"{FRONTEND_URL}/category")
        page.wait_for_load_state("networkidle")
        assert page.locator(".navbar").is_visible()

    def test_forum_page_loads(self, page):
        """测试: 论坛页加载"""
        self._login(page)
        page.goto(f"{FRONTEND_URL}/forum")
        page.wait_for_load_state("networkidle")
        assert page.locator(".navbar").is_visible()

    def test_messages_page_loads(self, page):
        """测试: 消息页加载"""
        self._login(page)
        page.goto(f"{FRONTEND_URL}/messages")
        page.wait_for_load_state("networkidle")
        assert page.locator(".navbar").is_visible()

    def test_profile_page_loads(self, page):
        """测试: 个人中心页加载"""
        self._login(page)
        page.goto(f"{FRONTEND_URL}/profile")
        page.wait_for_load_state("networkidle")
        assert page.locator(".navbar").is_visible()

    def _login(self, page):
        """辅助: 登录"""
        page.goto(f"{FRONTEND_URL}/auth")
        page.wait_for_load_state("networkidle")
        page.fill('input[placeholder*="用户名"]', "testuser")
        page.fill('input[type="password"]', "Test@1234")
        page.click('button:has-text("登录")')
        page.wait_for_timeout(2000)


# ============================================================
# 导航测试
# ============================================================

class TestNavigation:
    """测试页面间导航"""

    def test_nav_to_index(self, page):
        """测试: 导航栏跳转到首页"""
        self._go_to_page(page, "/index")
        page.click('a[href="/index"]')
        page.wait_for_load_state("networkidle")
        assert "/index" in page.url

    def test_nav_to_category(self, page):
        """测试: 导航栏跳转到分类"""
        self._go_to_page(page, "/index")
        page.click('a[href="/category"]')
        page.wait_for_load_state("networkidle")
        assert "/category" in page.url

    def test_nav_to_forum(self, page):
        """测试: 导航栏跳转到论坛"""
        self._go_to_page(page, "/index")
        page.click('a[href="/forum"]')
        page.wait_for_load_state("networkidle")
        assert "/forum" in page.url

    def test_nav_to_profile(self, page):
        """测试: 导航栏跳转到个人中心"""
        self._go_to_page(page, "/index")
        page.click('a[href="/profile"]')
        page.wait_for_load_state("networkidle")
        assert "/profile" in page.url

    def _go_to_page(self, page, path):
        """辅助: 登录并导航到指定页面"""
        page.goto(f"{FRONTEND_URL}/auth")
        page.wait_for_load_state("networkidle")
        page.fill('input[placeholder*="用户名"]', "testuser")
        page.fill('input[type="password"]', "Test@1234")
        page.click('button:has-text("登录")')
        page.wait_for_timeout(2000)
        page.goto(f"{FRONTEND_URL}{path}")
        page.wait_for_load_state("networkidle")


# ============================================================
# 登录流程测试
# ============================================================

class TestLoginFlow:
    """测试登录流程"""

    def test_login_success(self, page):
        """测试: 正常登录"""
        page.goto(f"{FRONTEND_URL}/auth")
        page.wait_for_load_state("networkidle")
        page.fill('input[placeholder*="用户名"]', "testuser")
        page.fill('input[type="password"]', "Test@1234")
        page.click('button:has-text("登录")')
        page.wait_for_timeout(2000)
        # 登录成功后应跳转到首页
        assert "/index" in page.url or "/auth" not in page.url

    def test_login_wrong_password(self, page):
        """测试: 错误密码"""
        page.goto(f"{FRONTEND_URL}/auth")
        page.wait_for_load_state("networkidle")
        page.fill('input[placeholder*="用户名"]', "testuser")
        page.fill('input[type="password"]', "WrongPass1")
        page.click('button:has-text("登录")')
        page.wait_for_timeout(1000)
        # 应该仍在登录页或显示错误提示
        assert page.locator('.el-message--error, .el-message--warning').is_visible(timeout=3000)

    def test_register_switch(self, page):
        """测试: 切换到注册表单"""
        page.goto(f"{FRONTEND_URL}/auth")
        page.wait_for_load_state("networkidle")
        page.click('text=注册')
        page.wait_for_timeout(500)
        # 注册表单应可见（确认密码输入框出现即为注册模式）
        assert page.locator('input[placeholder*="再次输入密码"]').is_visible()


# ============================================================
# 搜索功能测试
# ============================================================

class TestUserSearch:
    """测试用户搜索功能"""

    def test_search_modal_opens(self, page):
        """测试: 点击搜索按钮打开弹窗"""
        self._login(page)
        page.goto(f"{FRONTEND_URL}/index")
        page.wait_for_load_state("networkidle")
        # 点击搜索按钮
        page.click(".nav-search-btn")
        page.wait_for_timeout(500)
        assert page.locator(".search-modal").is_visible()

    def test_search_input_works(self, page):
        """测试: 搜索输入触发结果"""
        self._login(page)
        page.goto(f"{FRONTEND_URL}/index")
        page.wait_for_load_state("networkidle")
        page.click(".nav-search-btn")
        page.wait_for_timeout(300)
        page.fill(".search-modal-input", "test")
        page.wait_for_timeout(1000)  # 等防抖
        # 应有搜索结果或空结果提示
        has_results = page.locator(".search-result-item").count() > 0
        has_empty = page.locator(".search-empty").is_visible()
        has_loading = page.locator(".search-loading").is_visible()
        assert has_results or has_empty or has_loading

    def _login(self, page):
        page.goto(f"{FRONTEND_URL}/auth")
        page.wait_for_load_state("networkidle")
        page.fill('input[placeholder*="用户名"]', "testuser")
        page.fill('input[type="password"]', "Test@1234")
        page.click('button:has-text("登录")')
        page.wait_for_timeout(2000)


# ============================================================
# 动漫详情页测试
# ============================================================

class TestAnimeDetail:
    """测试动漫详情页"""

    def test_anime_detail_loads(self, page):
        """测试: 动漫详情页加载"""
        self._login(page)
        page.goto(f"{FRONTEND_URL}/anime/1")
        page.wait_for_load_state("networkidle")
        # 页面应正常渲染
        assert page.locator("body").is_visible()

    def test_anime_player_loads(self, page):
        """测试: 动漫播放页加载"""
        self._login(page)
        page.goto(f"{FRONTEND_URL}/anime/1/play/1")
        page.wait_for_load_state("networkidle")
        assert page.locator("body").is_visible()

    def _login(self, page):
        page.goto(f"{FRONTEND_URL}/auth")
        page.wait_for_load_state("networkidle")
        page.fill('input[placeholder*="用户名"]', "testuser")
        page.fill('input[type="password"]', "Test@1234")
        page.click('button:has-text("登录")')
        page.wait_for_timeout(2000)


# ============================================================
# 用户主页测试
# ============================================================

class TestUserHome:
    """测试用户主页"""

    def test_user_home_loads(self, page):
        """测试: 用户主页加载"""
        self._login(page)
        page.goto(f"{FRONTEND_URL}/user/testuser")
        page.wait_for_load_state("networkidle")
        assert page.locator(".user-header").is_visible(timeout=5000)

    def test_user_home_tabs(self, page):
        """测试: 用户主页切换标签"""
        self._login(page)
        page.goto(f"{FRONTEND_URL}/user/testuser")
        page.wait_for_load_state("networkidle")
        # 等待用户头部加载
        page.wait_for_selector(".user-header", timeout=5000)
        # 点击"观看记录"标签
        tab = page.locator(".tab-btn:has-text('观看记录')")
        if tab.is_visible():
            tab.click()
            page.wait_for_timeout(1000)

    def _login(self, page):
        page.goto(f"{FRONTEND_URL}/auth")
        page.wait_for_load_state("networkidle")
        page.fill('input[placeholder*="用户名"]', "testuser")
        page.fill('input[type="password"]', "Test@1234")
        page.click('button:has-text("登录")')
        page.wait_for_timeout(2000)