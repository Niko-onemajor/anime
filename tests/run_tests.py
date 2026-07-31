"""
测试运行入口脚本
支持多种测试模式: API 测试、E2E 测试、全量测试

用法:
  python run_tests.py --list          查看每个 py 文件对应的测试内容
  python run_tests.py --mode api      运行 API 测试
  python run_tests.py --mode e2e      运行 E2E UI 测试
  python run_tests.py --mode all      运行全部测试
  python run_tests.py --mode api --report   生成 HTML 报告
"""
import subprocess
import sys
import os
import argparse
import re
import json
from datetime import datetime

# ============================================================
# 测试文件 → 测试内容 映射表
# ============================================================
TEST_FILE_MAP = {
    "test_api_auth.py": {
        "模块": "认证模块",
        "测试内容": [
            "登录成功/失败/空用户名",
            "注册弱密码/短密码被拒绝",
            "获取用户信息/公开资料",
            "修改密码/Token刷新",
            "更新用户资料/通过ID查用户",
            "无Token访问受保护接口",
        ]
    },
    "test_api_anime.py": {
        "模块": "动漫模块",
        "测试内容": [
            "动漫列表/分页列表",
            "动漫详情/不存在的动漫",
            "周榜/月榜/年榜",
            "搜索动漫/空关键词搜索",
            "按年份/字母筛选",
            "动漫评论列表/添加评论/回复/按作者查评论",
            "动漫评论点赞/点踩",
            "按评分排序/按观看次数热门/观看次数查询",
            "集数列表/获取特定集数",
            "热门推荐/个性化推荐",
        ]
    },
    "test_api_forum.py": {
        "模块": "论坛模块",
        "测试内容": [
            "帖子列表/创建帖子/空标题拒绝",
            "帖子详情/搜索帖子",
            "点赞帖子/点踩帖子/按时间排序/按点赞排序",
            "论坛评论列表/添加评论/点赞评论/点踩评论",
            "按作者获取论坛评论",
            "帖子互动状态查询",
        ]
    },
    "test_api_user.py": {
        "模块": "用户/社交模块",
        "测试内容": [
            "关注/取消关注/关注状态/粉丝数/关注数",
            "关注列表/粉丝列表",
            "添加收藏/检查收藏状态/收藏列表/取消收藏",
            "提交评分/用户评分/评分列表",
            "观看记录添加/列表",
            "搜索用户/空关键词搜索",
            "通知列表/未读数/同步通知/标记已读/全部已读",
            "聊天会话列表/发送消息/对话记录/标记已读/未读数",
            "隐私设置更新",
        ]
    },
    "test_api_file.py": {
        "模块": "文件上传/杂项模块",
        "测试内容": [
            "头像上传/封面上传",
            "无文件上传验证",
            "测试数据生成",
            "测试资源URL获取",
        ]
    },
    "test_api_admin.py": {
        "模块": "管理员模块",
        "测试内容": [
            "用户管理: 列表/搜索/添加/更新/删除/重置密码/修改密码",
            "弱密码创建用户被拒绝",
            "动漫管理: 列表/搜索/添加/更新/删除/切换状态",
            "论坛管理: 帖子列表/搜索/排序/更新/删除/评论/评论更新/评论删除",
            "已删除记录: 用户/动漫/集数",
            "恢复记录: 用户/动漫/集数",
            "彻底删除: 用户/动漫/集数",
            "未授权访问: 无Token/普通用户Token",
        ]
    },
    "test_performance.py": {
        "模块": "性能测试",
        "测试内容": [
            "响应时间: 各接口单独响应时间检测",
            "并发请求: 10/15/30并发请求测试",
            "混合并发: 不同接口混合并发",
            "高并发: 30并发请求稳定性",
            "突发负载: 20请求突发吞吐量",
            "持续负载: 3轮迭代持续负载",
            "API稳定性: 20次重复请求/快速连续请求",
            "前端页面加载: HTTP响应时间",
        ]
    },
    "test_security.py": {
        "模块": "安全测试",
        "测试内容": [
            "权限越界: 无Token/普通用户Token访问管理员接口",
            "越权修改: 修改其他用户资料",
            "XSS防护: 注册/登录/搜索/签名中的XSS注入",
            "SQL注入防护: 登录/搜索/参数中的SQL注入",
            "路径遍历: 头像/动漫ID中的路径遍历",
            "Token安全: 过期/无效/空Token/无Bearer前缀",
            "敏感信息泄露: 错误消息堆栈/响应头/用户枚举",
            "CSRF防护: 无CSRFToken/GET状态变更",
            "输入验证: 用户名边界/特殊字符/超大请求/Null字节",
            "速率限制: 10次快速登录尝试",
            "跨域安全: CORS预检/未授权来源",
        ]
    },
    "test_e2e_ui.py": {
        "模块": "E2E UI测试",
        "测试内容": [
            "页面加载: 登录/首页/分类/论坛/消息/个人中心",
            "页面导航: 导航栏跳转各页面",
            "登录流程: 正常登录/错误密码/注册切换",
            "用户搜索: 弹窗打开/搜索输入",
            "动漫详情页: 详情页/播放页加载",
            "用户主页: 页面加载/标签切换",
        ]
    },
}


# ============================================================
# 测试方法 → 中文备注 映射表
# ============================================================
TEST_METHOD_REMARKS = {
    # 认证模块
    "test_login_success": "验证正确用户名密码登录成功，返回Token和用户信息",
    "test_login_wrong_password": "验证错误密码登录失败，返回401错误",
    "test_login_empty_username": "验证空用户名登录被拒绝",
    "test_register_weak_password": "验证弱密码注册被拒绝",
    "test_register_short_password": "验证短密码（<8位）注册被拒绝",
    "test_get_user_info": "验证通过用户名获取用户基本信息",
    "test_get_user_profile": "验证获取用户完整公开资料",
    "test_change_password": "验证修改密码流程（旧密码验证+新密码设置）",
    "test_update_profile": "验证更新用户资料（昵称/邮箱/生日/签名等）",
    "test_get_user_info_by_id": "验证通过用户ID获取用户信息",
    "test_no_token_access": "验证无Token访问受保护接口返回401",
    "test_refresh_token": "验证使用RefreshToken刷新AccessToken",
    # 动漫模块
    "test_get_anime_list": "验证获取动漫列表接口正常返回",
    "test_get_anime_list_page": "验证动漫列表分页功能",
    "test_anime_list_not_empty": "验证动漫列表不为空",
    "test_get_anime_detail": "验证获取动漫详情（标题/简介/评分/集数等）",
    "test_anime_detail_not_found": "验证查询不存在的动漫返回错误",
    "test_weekly_ranking": "验证周榜排名数据",
    "test_monthly_ranking": "验证月榜排名数据",
    "test_yearly_ranking": "验证年榜排名数据",
    "test_search_anime": "验证按关键词搜索动漫",
    "test_search_empty": "验证空关键词搜索处理",
    "test_filter_by_year": "验证按年份筛选动漫",
    "test_filter_by_letter": "验证按首字母筛选动漫",
    "test_get_anime_comments": "验证获取动漫评论列表",
    "test_add_anime_comment": "验证添加动漫评论",
    "test_get_comment_by_author": "验证按作者查询动漫评论",
    "test_get_comment_replies": "验证获取评论回复列表",
    "test_like_anime_comment": "验证点赞动漫评论",
    "test_dislike_anime_comment": "验证点踩动漫评论",
    "test_get_anime_by_rating": "验证按评分排序获取动漫",
    "test_get_popular_anime": "验证获取热门动漫（按观看次数）",
    "test_get_anime_watch_count": "验证获取单个动漫观看次数",
    "test_get_all_watch_counts": "验证获取所有动漫观看次数",
    "test_get_episodes_by_anime": "验证获取动漫的集数列表",
    "test_get_specific_episode": "验证获取特定集数详情",
    "test_get_personalized_recommendations": "验证个性化推荐接口",
    # 论坛模块
    "test_get_posts": "验证获取帖子列表",
    "test_create_post": "验证创建新帖子",
    "test_create_post_empty_title": "验证空标题帖子被拒绝",
    "test_get_post_detail": "验证获取帖子详情",
    "test_search_post": "验证搜索帖子",
    "test_like_post": "验证点赞帖子",
    "test_dislike_post": "验证点踩帖子",
    "test_get_posts_by_time": "验证按时间排序帖子",
    "test_get_posts_by_likes": "验证按点赞数排序帖子",
    "test_get_forum_comments": "验证获取论坛评论列表",
    "test_add_forum_comment": "验证添加论坛评论",
    "test_like_comment": "验证点赞论坛评论",
    "test_dislike_comment": "验证点踩论坛评论",
    "test_get_comment_by_author_forum": "验证按作者获取论坛评论",
    "test_get_post_interaction_status": "验证查询帖子互动状态",
    # 用户/社交模块
    "test_toggle_follow": "验证关注/取消关注切换",
    "test_follow_status": "验证查询关注状态",
    "test_follower_count": "验证获取粉丝数",
    "test_following_count": "验证获取关注数",
    "test_following_list": "验证获取关注列表",
    "test_follower_list": "验证获取粉丝列表",
    "test_add_favorite": "验证添加收藏",
    "test_check_favorite": "验证检查收藏状态",
    "test_list_favorites": "验证获取收藏列表",
    "test_remove_favorite": "验证取消收藏",
    "test_submit_rating": "验证提交评分",
    "test_user_rating": "验证获取用户评分",
    "test_user_rating_list": "验证获取评分列表",
    "test_add_watch_history": "验证添加观看记录",
    "test_list_watch_history": "验证获取观看记录列表",
    "test_search_user": "验证搜索用户（测试用户应被过滤）",
    "test_search_user_empty": "验证空关键词搜索用户",
    "test_get_notifications": "验证获取通知列表",
    "test_unread_count": "验证获取未读通知数",
    "test_sync_notifications": "验证同步通知",
    "test_mark_notification_read": "验证标记单个通知已读",
    "test_mark_all_notifications_read": "验证标记全部通知已读",
    "test_get_conversations": "验证获取聊天会话列表",
    "test_send_message": "验证发送聊天消息",
    "test_get_conversation": "验证获取聊天对话记录",
    "test_mark_chat_read": "验证标记聊天消息已读",
    "test_get_chat_unread_count": "验证获取聊天未读消息数",
    "test_update_privacy": "验证更新隐私设置",
    # 文件上传模块
    "test_upload_avatar": "验证上传头像",
    "test_upload_cover": "验证上传封面",
    "test_upload_no_file": "验证无文件上传被拒绝",
    "test_create_test_data": "验证生成测试数据",
    "test_get_test_resource_url": "验证获取测试资源URL",
    # 管理员模块
    "test_get_users": "验证管理员获取用户列表",
    "test_search_users": "验证管理员搜索用户",
    "test_add_user": "验证管理员添加用户",
    "test_update_user": "验证管理员更新用户信息",
    "test_delete_user": "验证管理员删除用户",
    "test_reset_password": "验证管理员重置用户密码",
    "test_admin_change_password": "验证管理员修改密码",
    "test_add_user_weak_password": "验证弱密码创建用户被拒绝",
    "test_get_animes": "验证管理员获取动漫列表",
    "test_search_animes": "验证管理员搜索动漫",
    "test_add_anime": "验证管理员添加动漫",
    "test_update_anime": "验证管理员更新动漫",
    "test_delete_anime": "验证管理员删除动漫",
    "test_toggle_anime_status": "验证管理员切换动漫状态",
    "test_get_forum_posts": "验证管理员获取论坛帖子列表",
    "test_search_forum_posts": "验证管理员搜索帖子",
    "test_sort_posts": "验证管理员排序帖子",
    "test_update_post": "验证管理员更新帖子",
    "test_delete_post": "验证管理员删除帖子",
    "test_get_forum_comments_admin": "验证管理员获取论坛评论",
    "test_update_comment": "验证管理员更新评论",
    "test_delete_comment": "验证管理员删除评论",
    "test_get_deleted_users": "验证获取已删除用户列表",
    "test_get_deleted_animes": "验证获取已删除动漫列表",
    "test_get_deleted_episodes": "验证获取已删除集数列表",
    "test_restore_user": "验证恢复已删除用户",
    "test_restore_anime": "验证恢复已删除动漫",
    "test_restore_episode": "验证恢复已删除集数",
    "test_hard_delete_user": "验证彻底删除用户",
    "test_hard_delete_anime": "验证彻底删除动漫",
    "test_hard_delete_episode": "验证彻底删除集数",
    "test_no_token_admin": "验证无Token访问管理员接口被拒绝",
    "test_user_token_admin": "验证普通用户Token访问管理员接口被拒绝",
    # 性能测试
    "test_response_time": "检测各接口单独响应时间",
    "test_concurrent_10": "10并发请求测试",
    "test_concurrent_15": "15并发请求测试",
    "test_concurrent_30": "30并发请求测试",
    "test_mixed_concurrent": "不同接口混合并发测试",
    "test_high_concurrent": "30并发请求稳定性测试",
    "test_burst_load": "20请求突发吞吐量测试",
    "test_sustained_load": "3轮迭代持续负载测试",
    "test_api_stability": "20次重复请求稳定性测试",
    "test_frontend_page_load": "前端页面HTTP响应时间测试",
    # 安全测试
    "test_unauthorized_admin_access": "验证无Token访问管理员接口",
    "test_user_token_admin_access": "验证普通用户Token越权访问管理员接口",
    "test_modify_other_profile": "验证越权修改其他用户资料",
    "test_xss_register": "验证注册时的XSS注入防护",
    "test_xss_login": "验证登录时的XSS注入防护",
    "test_xss_search": "验证搜索时的XSS注入防护",
    "test_xss_signature": "验证签名中的XSS注入防护",
    "test_sql_injection_login": "验证登录SQL注入防护",
    "test_sql_injection_search": "验证搜索SQL注入防护",
    "test_sql_injection_param": "验证参数SQL注入防护",
    "test_path_traversal_avatar": "验证头像路径遍历防护",
    "test_path_traversal_anime_id": "验证动漫ID路径遍历防护",
    "test_expired_token": "验证过期Token被拒绝",
    "test_invalid_token": "验证无效Token被拒绝",
    "test_empty_token": "验证空Token被拒绝",
    "test_no_bearer_prefix": "验证无Bearer前缀Token被拒绝",
    "test_error_message_no_stacktrace": "验证错误消息不泄露堆栈信息",
    "test_response_headers_security": "验证响应头安全配置",
    "test_user_enumeration": "验证防用户枚举",
    "test_csrf_no_token": "验证无CSRFToken请求",
    "test_csrf_get_state_change": "验证GET请求不触发状态变更",
    "test_username_boundary": "验证用户名边界值测试",
    "test_special_characters": "验证特殊字符输入处理",
    "test_large_request": "验证超大请求处理",
    "test_null_byte": "验证Null字节注入防护",
    "test_rate_limit": "验证10次快速登录速率限制",
    "test_cors_preflight": "验证CORS预检请求",
    "test_cors_unauthorized_origin": "验证未授权来源CORS请求",
    # E2E UI测试
    "test_page_load_login": "验证登录页面加载",
    "test_page_load_index": "验证首页加载",
    "test_page_load_category": "验证分类页面加载",
    "test_page_load_forum": "验证论坛页面加载",
    "test_page_load_message": "验证消息页面加载",
    "test_page_load_profile": "验证个人中心页面加载",
    "test_navigation": "验证导航栏跳转各页面",
    "test_login_flow_success": "验证正常登录流程",
    "test_login_flow_wrong_password": "验证错误密码登录提示",
    "test_login_flow_register_switch": "验证登录/注册切换",
    "test_search_modal_open": "验证用户搜索弹窗打开",
    "test_search_input": "验证搜索输入功能",
    "test_anime_detail_page": "验证动漫详情页加载",
    "test_anime_player_page": "验证播放页加载",
    "test_user_home_page": "验证用户主页加载",
    "test_user_home_tabs": "验证用户主页标签切换",
}


def get_remark(test_name):
    """根据测试方法名获取中文备注"""
    return TEST_METHOD_REMARKS.get(test_name, "")


def print_test_list():
    """打印测试文件与测试内容的映射表"""
    print("=" * 70)
    print("  测试文件 → 测试内容 对照表")
    print("=" * 70)
    for i, (filename, info) in enumerate(TEST_FILE_MAP.items(), 1):
        print(f"\n  [{i}] {filename}")
        print(f"      模块: {info['模块']}")
        for j, item in enumerate(info['测试内容'], 1):
            print(f"        {j}. {item}")
    print("\n" + "=" * 70)
    print("  运行方式:")
    print("    python run_tests.py --mode api      # 运行 [1]~[5] 全部 API 测试")
    print("    python run_tests.py --mode e2e      # 运行 [6] E2E UI 测试")
    print("    python run_tests.py --mode all      # 运行全部测试")
    print("    pytest test_api_auth.py -v          # 只测认证模块")
    print("=" * 70)


def html_escape(text):
    """HTML 转义"""
    if not text:
        return ""
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
        .replace("'", "&#39;")
    )


def parse_pytest_stdout_for_html(output: str):
    """解析 pytest stdout 输出，提取结构化测试结果（含错误详情）"""
    test_results = []

    # 匹配两种格式:
    # 1. 渐进输出: test_file.py::Class::method <- path STATUS [xx%]
    # 2. 摘要输出: STATUS test_file.py::Class::method - message
    test_pattern_a = re.compile(
        r'(test_\w+\.py)::(\w+)::(\w+).*?\s(PASSED|FAILED|SKIPPED|ERROR)'
    )
    test_pattern_b = re.compile(
        r'(PASSED|FAILED|SKIPPED|ERROR)\s+(test_\w+\.py)::(\w+)::(\w+)'
    )

    # 先收集所有测试用例
    tests_by_name = {}
    for line in output.splitlines():
        # 尝试格式A: file::class::method STATUS
        m = test_pattern_a.search(line)
        if m:
            file, cls, method, status = m.groups()
            full_name = f"{cls}.{method}"
            tests_by_name[full_name] = {
                "suite": file,
                "classname": cls,
                "name": method,
                "status": status.lower(),
                "message": "",
                "traceback": "",
            }
            continue
        # 尝试格式B: STATUS file::class::method
        m = test_pattern_b.search(line)
        if m:
            status, file, cls, method = m.groups()
            full_name = f"{cls}.{method}"
            if full_name not in tests_by_name:
                tests_by_name[full_name] = {
                    "suite": file,
                    "classname": cls,
                    "name": method,
                    "status": status.lower(),
                    "message": "",
                    "traceback": "",
                }

    if not tests_by_name:
        return test_results

    # 解析 FAILURES 部分提取错误详情
    # pytest 的失败详情格式:
    # _______ TestClass.test_method _______
    # ... 错误详情 ...
    failure_pattern = re.compile(
        r'_{10,}\s*\n\s*(\w+\.\w+)\s*_{10,}\s*\n(.*?)(?=_{10,}|={10,}|\Z)',
        re.DOTALL
    )

    failures = {}
    for m in failure_pattern.finditer(output):
        test_name = m.group(1)
        detail = m.group(2).strip()
        failures[test_name] = detail

    # 解析错误部分的摘要行
    error_summary_pattern = re.compile(
        r'(FAILED|ERROR)\s+(test_\w+\.py::\w+::\w+)\s*-\s*(.*?)(?=\n|$)'
    )
    error_summaries = {}
    for m in error_summary_pattern.finditer(output):
        full_path = m.group(2)
        parts = full_path.split("::")
        if len(parts) >= 3:
            test_name = f"{parts[1]}.{parts[2]}"
            error_summaries[test_name] = m.group(3).strip()

    # 将错误详情关联到对应的测试用例
    for test_name, detail in failures.items():
        # 提取错误消息
        error_msg = ""
        tb_lines = []
        in_tb = False
        for line in detail.splitlines():
            stripped = line.strip()
            if stripped.startswith("E "):
                error_msg = stripped[2:] if not error_msg else error_msg
            if stripped.startswith("E ") or "Error" in stripped or "assert" in stripped:
                tb_lines.append(line)
            if stripped and not stripped.startswith("E "):
                if "Error" in stripped or "AssertionError" in stripped:
                    error_msg = stripped

        traceback = detail

        # 匹配到对应的 test_entry
        for key, entry in tests_by_name.items():
            if test_name in key or key in test_name:
                if entry["status"] in ("failed", "error"):
                    entry["message"] = error_msg or detail[:200]
                    entry["traceback"] = traceback or detail
                break

    # 也检查 error_summaries
    for test_name, summary in error_summaries.items():
        for key, entry in tests_by_name.items():
            if test_name in key or key in test_name:
                if entry["status"] in ("failed", "error") and not entry["message"]:
                    entry["message"] = summary
                break

    # 转换为列表
    for entry in tests_by_name.values():
        test_results.append(entry)

    return test_results


def generate_detailed_html_report(test_results, output_path):
    """
    生成可展开查看错误详情的 HTML 测试报告
    点击失败/错误行可展开查看完整错误信息和 traceback
    """
    total = len(test_results)
    passed = sum(1 for t in test_results if t["status"] == "passed")
    failed = sum(1 for t in test_results if t["status"] == "failed")
    errors = sum(1 for t in test_results if t["status"] == "error")
    skipped = sum(1 for t in test_results if t["status"] == "skipped")
    pass_rate = round(passed / total * 100, 1) if total > 0 else 0

    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    # 生成测试用例行
    test_rows = []
    for i, t in enumerate(test_results):
        status_cn = {"passed": "通过", "failed": "失败", "error": "错误", "skipped": "跳过"}
        status = t["status"]
        msg = t["message"]
        tb = t["traceback"]

        msg_short = msg[:200] + "..." if len(msg) > 200 else msg

        # 获取中文备注
        remark = get_remark(t['name'])

        if status in ("failed", "error"):
            detail_html = f"""
            <tr class="detail-row" id="detail-{i}">
                <td colspan="5">
                    <div class="error-detail">
                        <div class="error-message"><strong>错误信息:</strong> {html_escape(msg)}</div>
                        <pre class="traceback">{html_escape(tb)}</pre>
                    </div>
                </td>
            </tr>"""
        else:
            detail_html = ""

        row = f"""
        <tr class="test-row status-{status}" onclick="toggleDetail({i})">
            <td class="status-col"><span class="badge badge-{status}">{status_cn.get(status, status)}</span></td>
            <td class="name-col">{html_escape(t['name'])}</td>
            <td class="class-col">{html_escape(t['classname'])}</td>
            <td class="remark-col">{html_escape(remark) if remark else '-'}</td>
            <td class="msg-col">{html_escape(msg_short) if msg else '-'}</td>
        </tr>
        {detail_html}"""
        test_rows.append(row)

    html = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Niko动漫 - 测试报告</title>
    <style>
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f6fa; color: #333; }}
        .header {{ background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px 40px; }}
        .header h1 {{ font-size: 24px; margin-bottom: 8px; }}
        .header .time {{ font-size: 13px; opacity: 0.85; }}
        .summary {{ display: flex; gap: 20px; padding: 24px 40px; background: white; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }}
        .summary-card {{ flex: 1; text-align: center; padding: 16px; border-radius: 8px; background: #f8f9fa; }}
        .summary-card .num {{ font-size: 32px; font-weight: 700; }}
        .summary-card .label {{ font-size: 13px; color: #666; margin-top: 4px; }}
        .summary-card.passed .num {{ color: #27ae60; }}
        .summary-card.failed .num {{ color: #e74c3c; }}
        .summary-card.error .num {{ color: #e67e22; }}
        .summary-card.skipped .num {{ color: #95a5a6; }}
        .summary-card.rate .num {{ color: #667eea; }}
        .container {{ max-width: 1200px; margin: 24px auto; padding: 0 20px; }}
        table {{ width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }}
        thead {{ background: #f8f9fa; }}
        th {{ padding: 12px 16px; text-align: left; font-size: 13px; color: #666; font-weight: 600; border-bottom: 2px solid #e9ecef; }}
        td {{ padding: 12px 16px; font-size: 14px; border-bottom: 1px solid #f0f0f0; }}
        .test-row {{ cursor: pointer; transition: background 0.15s; }}
        .test-row:hover {{ background: #f8f9ff; }}
        .status-failed {{ background: #fff5f5; }}
        .status-failed:hover {{ background: #ffe8e8; }}
        .status-error {{ background: #fff8f0; }}
        .status-error:hover {{ background: #fff0e0; }}
        .badge {{ display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; }}
        .badge-passed {{ background: #e8f5e9; color: #27ae60; }}
        .badge-failed {{ background: #ffebee; color: #e74c3c; }}
        .badge-error {{ background: #fff3e0; color: #e67e22; }}
        .badge-skipped {{ background: #f0f0f0; color: #95a5a6; }}
        .status-col {{ width: 70px; }}
        .name-col {{ max-width: 250px; word-break: break-all; }}
        .class-col {{ max-width: 200px; color: #888; font-size: 13px; }}
        .remark-col {{ max-width: 300px; color: #555; font-size: 13px; }}
        .msg-col {{ max-width: 250px; color: #999; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }}
        .detail-row {{ display: none; }}
        .detail-row.show {{ display: table-row; }}
        .detail-row td {{ padding: 0; }}
        .error-detail {{ padding: 16px 24px; background: #fff5f5; border-left: 3px solid #e74c3c; }}
        .error-message {{ color: #c0392b; font-size: 14px; margin-bottom: 12px; line-height: 1.6; }}
        .traceback {{ background: #1e1e1e; color: #d4d4d4; padding: 16px; border-radius: 6px; font-size: 12px; line-height: 1.5; overflow-x: auto; white-space: pre-wrap; word-break: break-all; max-height: 400px; overflow-y: auto; }}
        .filter-bar {{ display: flex; gap: 8px; margin-bottom: 16px; }}
        .filter-btn {{ padding: 6px 16px; border: 1px solid #ddd; border-radius: 20px; background: white; cursor: pointer; font-size: 13px; transition: all 0.2s; }}
        .filter-btn:hover {{ border-color: #667eea; color: #667eea; }}
        .filter-btn.active {{ background: #667eea; color: white; border-color: #667eea; }}
        .footer {{ text-align: center; padding: 20px; color: #999; font-size: 12px; }}
        .click-hint {{ font-size: 12px; color: #999; margin-bottom: 12px; }}
    </style>
</head>
<body>
    <div class="header">
        <h1>Niko动漫 - 自动化测试报告</h1>
        <div class="time">生成时间: {now}</div>
    </div>

    <div class="summary">
        <div class="summary-card passed">
            <div class="num">{passed}</div>
            <div class="label">通过</div>
        </div>
        <div class="summary-card failed">
            <div class="num">{failed}</div>
            <div class="label">失败</div>
        </div>
        <div class="summary-card error">
            <div class="num">{errors}</div>
            <div class="label">错误</div>
        </div>
        <div class="summary-card skipped">
            <div class="num">{skipped}</div>
            <div class="label">跳过</div>
        </div>
        <div class="summary-card rate">
            <div class="num">{pass_rate}%</div>
            <div class="label">通过率</div>
        </div>
    </div>

    <div class="container">
        <div class="click-hint">点击失败/错误的行可以展开查看详细错误信息和 traceback</div>
        <div class="filter-bar">
            <button class="filter-btn active" onclick="filterTests('all')">全部 ({total})</button>
            <button class="filter-btn" onclick="filterTests('passed')">通过 ({passed})</button>
            <button class="filter-btn" onclick="filterTests('failed')">失败 ({failed})</button>
            <button class="filter-btn" onclick="filterTests('error')">错误 ({errors})</button>
            <button class="filter-btn" onclick="filterTests('skipped')">跳过 ({skipped})</button>
        </div>

        <table>
            <thead>
                <tr>
                    <th>状态</th>
                    <th>测试用例</th>
                    <th>所属类</th>
                    <th>测试说明</th>
                    <th>备注</th>
                </tr>
            </thead>
            <tbody>
                {''.join(test_rows)}
            </tbody>
        </table>
    </div>

    <div class="footer">Niko动漫 自动化测试报告 · Powered by pytest</div>

    <script>
        function toggleDetail(idx) {{
            var row = document.getElementById('detail-' + idx);
            if (row) {{
                row.classList.toggle('show');
            }}
        }}
        function filterTests(status) {{
            document.querySelectorAll('.filter-btn').forEach(function(btn) {{
                btn.classList.remove('active');
            }});
            event.target.classList.add('active');

            document.querySelectorAll('.test-row').forEach(function(row) {{
                if (status === 'all') {{
                    row.style.display = '';
                }} else if (row.classList.contains('status-' + status)) {{
                    row.style.display = '';
                }} else {{
                    row.style.display = 'none';
                }}
            }});
            // 隐藏所有展开的详情行
            document.querySelectorAll('.detail-row').forEach(function(row) {{
                row.classList.remove('show');
            }});
        }}
        // 默认展开所有失败和错误的详情
        window.addEventListener('load', function() {{
            document.querySelectorAll('.test-row.status-failed, .test-row.status-error').forEach(function(row) {{
                row.click();
            }});
        }});
    </script>
</body>
</html>"""

    os.makedirs(os.path.dirname(output_path) or ".", exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(html)

    return output_path


def parse_pytest_output(output: str) -> dict:
    """解析 pytest 输出，提取每个测试用例的结果"""
    results = {"passed": [], "failed": [], "skipped": [], "error": []}

    # 匹配两种格式
    result_pattern_a = re.compile(
        r'(test_\w+\.py)::(\w+)::(\w+).*?\s(PASSED|FAILED|SKIPPED|ERROR)'
    )
    result_pattern_b = re.compile(
        r'(PASSED|FAILED|SKIPPED|ERROR)\s+(test_\w+\.py)::(\w+)::(\w+)'
    )

    seen = set()
    for line in output.splitlines():
        # 格式A: file::class::method STATUS
        m = result_pattern_a.search(line)
        if m:
            file, cls, method, status = m.groups()
            key = f"{file}::{cls}::{method}"
            if key not in seen:
                seen.add(key)
                status_lower = status.lower()
                entry = {"file": file, "class": cls, "method": method, "status": status}
                if status_lower in results:
                    results[status_lower].append(entry)
            continue
        # 格式B: STATUS file::class::method
        m = result_pattern_b.search(line)
        if m:
            status, file, cls, method = m.groups()
            key = f"{file}::{cls}::{method}"
            if key not in seen:
                seen.add(key)
                status_lower = status.lower()
                entry = {"file": file, "class": cls, "method": method, "status": status}
                if status_lower in results:
                    results[status_lower].append(entry)

    return results


def print_summary(results: dict, output: str):
    """打印测试结果摘要"""
    total = sum(len(v) for v in results.values())
    passed = len(results["passed"])
    failed = len(results["failed"])
    skipped = len(results["skipped"])
    errors = len(results["error"])

    print("\n")
    print("=" * 70)
    print("  测试结果摘要")
    print("=" * 70)
    print(f"  总计: {total}  |  "
          f"通过: {passed}  |  "
          f"失败: {failed}  |  "
          f"跳过: {skipped}  |  "
          f"错误: {errors}")
    print("-" * 70)

    if results["passed"]:
        print(f"\n  [通过] {len(results['passed'])} 个:")
        for item in results["passed"]:
            print(f"    OK  {item['file']} -> {item['class']}::{item['method']}")

    if results["skipped"]:
        print(f"\n  [跳过] {len(results['skipped'])} 个:")
        for item in results["skipped"]:
            print(f"    --  {item['file']} -> {item['class']}::{item['method']}")

    if results["failed"] or results["error"]:
        print(f"\n  [失败] {len(results['failed']) + len(results['error'])} 个 - 详情:")

        for item in results["failed"]:
            print(f"\n    X  {item['file']} -> {item['class']}::{item['method']}")

        # 从原始输出中提取失败原因
        failure_blocks = re.findall(
            r'_{50,}\n(.*?)(?=_{50,}|\Z)',
            output, re.DOTALL
        )
        for block in failure_blocks:
            if 'FAILURES' in block or 'ERRORS' in block:
                lines = block.strip().split('\n')
                for line in lines:
                    line = line.strip()
                    if 'AssertionError' in line or 'Error' in line or 'assert' in line:
                        print(f"      原因: {line}")
                        break

        for item in results["error"]:
            print(f"\n    !! {item['file']} -> {item['class']}::{item['method']}")

    print("\n" + "=" * 70)


def run_pytest(test_files, report=False, parallel=False, extra_args=None):
    """运行 pytest 并解析输出，当 report=True 时自动生成详细 HTML 报告（不生成 XML）"""
    cmd = [sys.executable, "-m", "pytest"] + test_files + ["-v", "--tb=short", "--color=no"]
    if parallel:
        cmd.extend(["-n", "auto"])
    if extra_args:
        cmd.extend(extra_args)

    print(f"\n[执行] {' '.join(cmd)}\n")

    # 运行 pytest，捕获输出
    result = subprocess.run(cmd, capture_output=True, text=True)

    # 合并 stdout 和 stderr（pytest 进度输出可能在 stderr 中）
    combined_output = result.stdout + "\n" + result.stderr

    # 打印完整输出
    print(result.stdout)
    if result.stderr:
        print(result.stderr)

    # 解析结果并打印摘要
    results = parse_pytest_output(combined_output)
    print_summary(results, combined_output)

    # 生成详细 HTML 报告（直接从 stdout 解析，不生成 XML）
    if report:
        os.makedirs("reports", exist_ok=True)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_path = f"reports/report_{timestamp}.html"

        test_results = parse_pytest_stdout_for_html(combined_output)
        if test_results:
            path = generate_detailed_html_report(test_results, report_path)
            abs_path = os.path.abspath(path)
            print(f"\n{'=' * 60}")
            print(f"  HTML 详细报告已生成!")
            print(f"  路径: {abs_path}")
            print(f"  在浏览器中打开: file:///{abs_path.replace(chr(92), '/')}")
            print(f"  点击失败/错误行可展开查看完整错误详情")
            print(f"{'=' * 60}")
        else:
            print(f"\n[WARN] 未解析到测试结果，跳过 HTML 报告生成")

    return result.returncode


def run_api_tests(report=False, parallel=False):
    """运行 API 测试"""
    files = ["test_api_auth.py", "test_api_anime.py",
             "test_api_forum.py", "test_api_user.py",
             "test_api_file.py", "test_api_admin.py",
             "test_performance.py", "test_security.py"]
    return run_pytest(files, report=report, parallel=parallel)


def run_e2e_tests(report=False, headed=False):
    """运行 E2E UI 测试"""
    extra = []
    if headed:
        os.environ["HEADED"] = "1"
    return run_pytest(["test_e2e_ui.py"], report=report, extra_args=["-m", "e2e"])


def run_all_tests(report=False, parallel=False):
    """运行全部测试"""
    return run_pytest(["."], report=report, parallel=parallel)


def check_prerequisites():
    """检查前置条件"""
    import requests

    print("=" * 60)
    print("检查前置条件...")
    print("=" * 60)

    try:
        resp = requests.get("http://localhost:8080/api/anime/list", timeout=5)
        if resp.status_code == 200:
            print("[OK] 后端服务运行中 (http://localhost:8080)")
        else:
            print(f"[WARN] 后端服务响应异常: {resp.status_code}")
    except requests.ConnectionError:
        print("[ERROR] 后端服务未启动! 请先启动 Spring Boot 应用")
        print("        cd back/anime && mvn spring-boot:run")
        return False
    except Exception as e:
        print(f"[ERROR] 检查后端失败: {e}")
        return False

    try:
        resp = requests.get("http://localhost:5173", timeout=5)
        print("[OK] 前端服务运行中 (http://localhost:5173)")
    except requests.ConnectionError:
        print("[WARN] 前端服务未启动 (E2E 测试需要前端)")
    except Exception:
        print("[WARN] 无法检查前端服务")

    print()
    return True


def main():
    parser = argparse.ArgumentParser(
        description="Niko动漫 自动化测试运行器",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  python run_tests.py --list             查看测试文件与测试内容对照表
  python run_tests.py --mode api         运行全部 API 测试
  python run_tests.py --mode api --report 运行 API 测试并生成 HTML 报告
  python run_tests.py --mode e2e         运行 E2E UI 测试
  python run_tests.py --mode all         运行全部测试
        """
    )
    parser.add_argument("--mode", choices=["api", "e2e", "all"],
                        default="api", help="测试模式: api/e2e/all (默认: api)")
    parser.add_argument("--list", action="store_true",
                        help="查看每个 py 文件对应的测试内容（不运行测试）")
    parser.add_argument("--report", action="store_true",
                        help="生成 HTML 测试报告")
    parser.add_argument("--parallel", action="store_true",
                        help="并行运行测试（加速）")
    parser.add_argument("--headed", action="store_true",
                        help="E2E 测试显示浏览器窗口")
    parser.add_argument("--skip-check", action="store_true",
                        help="跳过前置条件检查")

    args = parser.parse_args()

    # --list 模式：只打印对照表
    if args.list:
        print_test_list()
        return

    if not args.skip_check:
        if not check_prerequisites():
            sys.exit(1)

    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    os.makedirs("reports", exist_ok=True)

    print("=" * 60)
    print(f"  开始运行 {args.mode.upper()} 测试...")
    print("=" * 60)

    if args.mode == "api":
        exit_code = run_api_tests(report=args.report, parallel=args.parallel)
    elif args.mode == "e2e":
        exit_code = run_e2e_tests(report=args.report, headed=args.headed)
    else:
        exit_code = run_all_tests(report=args.report, parallel=args.parallel)

    sys.exit(exit_code)


if __name__ == "__main__":
    main()