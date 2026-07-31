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
            "动漫评论列表/添加评论/按作者查评论",
        ]
    },
    "test_api_forum.py": {
        "模块": "论坛模块",
        "测试内容": [
            "帖子列表/创建帖子/空标题拒绝",
            "帖子详情/搜索帖子",
            "点赞帖子/按时间排序/按点赞排序",
            "论坛评论列表/添加评论/点赞评论",
            "按作者获取论坛评论",
        ]
    },
    "test_api_user.py": {
        "模块": "用户/社交模块",
        "测试内容": [
            "关注/取消关注/关注状态/粉丝数/关注数",
            "关注列表/粉丝列表",
            "添加收藏/检查收藏状态/收藏列表",
            "提交评分/用户评分/评分列表",
            "观看记录添加/列表",
            "搜索用户/空关键词搜索",
            "通知列表/未读数/同步通知",
            "聊天会话列表/发送消息",
            "隐私设置更新",
        ]
    },
    "test_api_admin.py": {
        "模块": "管理员模块",
        "测试内容": [
            "用户管理: 列表/搜索/添加/更新/重置密码",
            "弱密码创建用户被拒绝",
            "动漫管理: 列表/搜索/添加",
            "论坛管理: 帖子列表/搜索/评论列表",
            "已删除记录: 用户/动漫",
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
    """运行 pytest 并解析输出"""
    cmd = ["pytest"] + test_files + ["-v", "--tb=long", "--color=yes"]
    if report:
        os.makedirs("reports", exist_ok=True)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_path = f"reports/report_{timestamp}.html"
        cmd.extend([f"--html={report_path}", "--self-contained-html"])
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

    if report:
        print(f"\n[报告] HTML 报告已保存到: {os.path.abspath(report_path)}")

    return result.returncode


def run_api_tests(report=False, parallel=False):
    """运行 API 测试"""
    files = ["test_api_auth.py", "test_api_anime.py",
             "test_api_forum.py", "test_api_user.py",
             "test_api_admin.py"]
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