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
from xml.etree import ElementTree as ET

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


def parse_junit_xml(xml_path):
    """解析 JUnit XML 测试结果"""
    test_results = []
    if not os.path.exists(xml_path):
        return test_results

    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()

        for testsuite in root.findall("testsuite"):
            suite_name = testsuite.get("name", "")
            for testcase in testsuite.findall("testcase"):
                classname = testcase.get("classname", "")
                name = testcase.get("name", "")
                time = testcase.get("time", "0")

                entry = {
                    "suite": suite_name,
                    "classname": classname,
                    "name": name,
                    "time": time,
                    "status": "passed",
                    "message": "",
                    "traceback": "",
                }

                failure = testcase.find("failure")
                error = testcase.find("error")
                skipped = testcase.find("skipped")

                if failure is not None:
                    msg = failure.get("message", "")
                    tb = failure.text or ""
                    entry["status"] = "failed"
                    entry["message"] = msg
                    entry["traceback"] = tb
                elif error is not None:
                    msg = error.get("message", "")
                    tb = error.text or ""
                    entry["status"] = "error"
                    entry["message"] = msg
                    entry["traceback"] = tb
                elif skipped is not None:
                    msg = skipped.get("message", "")
                    entry["status"] = "skipped"
                    entry["message"] = msg

                test_results.append(entry)
    except ET.ParseError as e:
        print(f"[WARN] 无法解析 JUnit XML: {e}")
    except Exception as e:
        print(f"[WARN] 解析测试结果时出错: {e}")

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

        if status in ("failed", "error"):
            detail_html = f"""
            <tr class="detail-row" id="detail-{i}">
                <td colspan="4">
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
        .name-col {{ max-width: 350px; word-break: break-all; }}
        .class-col {{ max-width: 250px; color: #888; font-size: 13px; }}
        .msg-col {{ max-width: 300px; color: #999; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }}
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

    # 匹配 pytest 的测试结果行
    # 格式: test_file.py::TestClass::test_method PASSED/FAILED/SKIPPED
    result_pattern = re.compile(
        r'(test_\w+\.py)::(\w+)::(\w+)\s+(PASSED|FAILED|SKIPPED|ERROR)'
    )

    for line in output.splitlines():
        m = result_pattern.search(line)
        if m:
            file, cls, method, status = m.groups()
            status_lower = status.lower()
            entry = {"file": file, "class": cls, "method": method, "status": status}
            if status_lower in results:
                results[status_lower].append(entry)

    # 提取失败测试的错误信息
    error_pattern = re.compile(r'FAILED (test_\w+\.py::\w+::\w+).*?\n(.*?)(?=\n_+ |\n\w+\.py::|\Z)', re.DOTALL)
    # 提取 AssertionError
    assertion_pattern = re.compile(r'AssertionError[:\s]*(.*?)(?=\n\w)', re.DOTALL)

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

        # 提取失败用例的详细错误
        for item in results["failed"]:
            print(f"\n    X  {item['file']} -> {item['class']}::{item['method']}")

        # 从原始输出中提取失败原因
        failure_blocks = re.findall(
            r'_{50,}\n(.*?)(?=_{50,}|\Z)',
            output, re.DOTALL
        )
        for block in failure_blocks:
            # 只打印包含 FAILURES 或 ERROR 的块
            if 'FAILURES' in block or 'ERRORS' in block:
                # 提取关键错误信息
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
    """运行 pytest 并解析输出，当 report=True 时自动生成详细 HTML 报告"""
    # 使用 sys.executable -m pytest 确保能找到 pytest
    cmd = [sys.executable, "-m", "pytest"] + test_files + ["-v", "--tb=long", "--color=yes"]
    junit_xml_path = None
    if report:
        os.makedirs("reports", exist_ok=True)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        junit_xml_path = f"reports/results_{timestamp}.xml"
        cmd.extend([f"--junitxml={junit_xml_path}"])
    if parallel:
        cmd.extend(["-n", "auto"])
    if extra_args:
        cmd.extend(extra_args)

    print(f"\n[执行] {' '.join(cmd)}\n")

    # 运行 pytest，捕获输出
    result = subprocess.run(cmd, capture_output=True, text=True)

    # 打印 stdout（实时输出）
    print(result.stdout)
    if result.stderr:
        print(result.stderr)

    # 解析结果并打印摘要
    results = parse_pytest_output(result.stdout)
    print_summary(results, result.stdout)

    # 生成详细 HTML 报告（可展开查看错误详情）
    if report and junit_xml_path and os.path.exists(junit_xml_path):
        test_results = parse_junit_xml(junit_xml_path)
        if test_results:
            report_path = junit_xml_path.replace(".xml", ".html")
            path = generate_detailed_html_report(test_results, report_path)
            abs_path = os.path.abspath(path)
            print(f"\n{'=' * 60}")
            print(f"  HTML 详细报告已生成!")
            print(f"  路径: {abs_path}")
            print(f"  在浏览器中打开: file:///{abs_path.replace(chr(92), '/')}")
            print(f"  点击失败/错误行可展开查看完整错误详情")
            print(f"{'=' * 60}")
        else:
            print(f"\n[WARN] JUnit XML 解析结果为空，跳过 HTML 报告生成")

    return result.returncode


def run_api_tests(report=False, parallel=False):
    """运行 API 测试"""
    files = ["test_api_auth.py", "test_api_anime.py",
             "test_api_forum.py", "test_api_user.py",
             "test_api_file.py", "test_api_admin.py"]
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