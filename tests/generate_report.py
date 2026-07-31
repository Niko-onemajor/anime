"""
自定义 HTML 测试报告生成器
生成可直接在浏览器中查看的详细测试报告，每个用例可以展开查看错误详情
（不生成 XML 文件，直接从 pytest stdout 解析结果）
"""
import json
import os
import subprocess
import sys
import re
from datetime import datetime


# 测试方法 → 中文备注 映射表
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
}


def get_remark(test_name):
    """根据测试方法名获取中文备注"""
    return TEST_METHOD_REMARKS.get(test_name, "")


def parse_pytest_stdout(output: str):
    """直接从 pytest stdout 解析测试结果（不依赖 XML 文件）"""
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

    # 收集所有测试用例
    tests_by_name = {}
    for line in output.splitlines():
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
    failure_pattern = re.compile(
        r'_{10,}\s*\n\s*(\w+\.\w+)\s*_{10,}\s*\n(.*?)(?=_{10,}|={10,}|\Z)',
        re.DOTALL
    )

    failures = {}
    for m in failure_pattern.finditer(output):
        test_name = m.group(1)
        detail = m.group(2).strip()
        failures[test_name] = detail

    # 解析错误摘要行
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

    # 关联错误详情
    for test_name, detail in failures.items():
        error_msg = ""
        for line in detail.splitlines():
            stripped = line.strip()
            if "AssertionError" in stripped or "Error" in stripped:
                error_msg = stripped
                break
        if not error_msg:
            for line in detail.splitlines():
                stripped = line.strip()
                if stripped.startswith("E "):
                    error_msg = stripped[2:]
                    break

        for key, entry in tests_by_name.items():
            if test_name in key or key in test_name:
                if entry["status"] in ("failed", "error"):
                    entry["message"] = error_msg or detail[:200]
                    entry["traceback"] = detail
                break

    for test_name, summary in error_summaries.items():
        for key, entry in tests_by_name.items():
            if test_name in key or key in test_name:
                if entry["status"] in ("failed", "error") and not entry["message"]:
                    entry["message"] = summary
                break

    for entry in tests_by_name.values():
        test_results.append(entry)

    return test_results


def run_pytest_and_get_results(test_files, parallel=False):
    """运行 pytest 并直接从 stdout 解析结果（不生成 XML 文件）"""
    cmd = [
        sys.executable, "-m", "pytest", *test_files,
        "-v", "--tb=short", "--color=no"
    ]
    if parallel:
        cmd.extend(["-n", "auto"])

    print(f"[执行] {' '.join(cmd)}\n")
    result = subprocess.run(cmd, capture_output=True, text=True)
    print(result.stdout)
    if result.stderr:
        print(result.stderr)

    combined_output = result.stdout + "\n" + result.stderr

    # 直接从 stdout 解析测试结果
    test_results = parse_pytest_stdout(combined_output)
    return test_results, result.stdout


def generate_html_report(test_results, output_path):
    """生成详细的 HTML 测试报告"""
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

        # 截断超长消息
        msg_short = msg[:200] + "..." if len(msg) > 200 else msg

        # 获取中文备注
        remark = get_remark(t['name'])

        if status == "failed" or status == "error":
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
        <div class="click-hint">点击失败/错误的行可以展开查看详细错误信息</div>
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

    with open(output_path, "w", encoding="utf-8") as f:
        f.write(html)

    return output_path


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


def run_and_report(test_files, output_path="reports/test_report.html", parallel=False):
    """运行测试并生成报告"""
    print("=" * 60)
    print("  运行测试并生成 HTML 报告...")
    print("=" * 60)

    results, stdout = run_pytest_and_get_results(test_files, parallel=parallel)

    if not results:
        print("[WARN] 未收集到测试结果，请确认测试文件路径正确")
        return None

    path = generate_html_report(results, output_path)
    abs_path = os.path.abspath(path)
    print(f"\n{'=' * 60}")
    print(f"  HTML 报告已生成!")
    print(f"  路径: {abs_path}")
    print(f"  在浏览器中打开: file:///{abs_path.replace(chr(92), '/')}")
    print(f"{'=' * 60}")

    return path


if __name__ == "__main__":
    files = [
        "test_api_auth.py", "test_api_anime.py",
        "test_api_forum.py", "test_api_user.py",
        "test_api_admin.py"
    ]
    if len(sys.argv) > 1:
        files = sys.argv[1:]

    run_and_report(files)