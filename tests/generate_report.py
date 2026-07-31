"""
自定义 HTML 测试报告生成器
生成可直接在浏览器中查看的详细测试报告，每个用例可以展开查看错误详情
"""
import json
import os
import subprocess
import sys
import re
from datetime import datetime
from xml.etree import ElementTree as ET


def run_pytest_and_get_results(test_files, parallel=False):
    """运行 pytest 并生成 JUnit XML 报告，然后解析结果"""
    xml_path = "reports/results.xml"
    os.makedirs("reports", exist_ok=True)

    cmd = [
        "pytest", *test_files,
        "-v", "--tb=long",
        f"--junitxml={xml_path}"
    ]
    if parallel:
        cmd.extend(["-n", "auto"])

    print(f"[执行] {' '.join(cmd)}\n")
    result = subprocess.run(cmd, capture_output=True, text=True)
    print(result.stdout)
    if result.stderr:
        print(result.stderr)

    # 解析 JUnit XML
    test_results = []
    if os.path.exists(xml_path):
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

        if status == "failed" or status == "error":
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