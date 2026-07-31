"""
测试运行入口脚本
支持多种测试模式: API 测试、E2E 测试、全量测试
"""
import subprocess
import sys
import os
import argparse


def run_api_tests(report=False, parallel=False):
    """运行 API 测试"""
    cmd = ["pytest", "test_api_auth.py", "test_api_anime.py",
           "test_api_forum.py", "test_api_user.py", "test_api_admin.py",
           "-v", "--tb=short"]
    if report:
        cmd.extend(["--html=reports/api_report.html", "--self-contained-html"])
    if parallel:
        cmd.extend(["-n", "auto"])
    print(f"[API测试] 执行: {' '.join(cmd)}")
    return subprocess.run(cmd).returncode


def run_e2e_tests(report=False, headed=False):
    """运行 E2E UI 测试（需要前端和后端都启动）"""
    cmd = ["pytest", "test_e2e_ui.py", "-v", "--tb=short", "-m", "e2e"]
    if report:
        cmd.extend(["--html=reports/e2e_report.html", "--self-contained-html"])
    if headed:
        os.environ["HEADED"] = "1"
    print(f"[E2E测试] 执行: {' '.join(cmd)}")
    return subprocess.run(cmd).returncode


def run_all_tests(report=False, parallel=False):
    """运行全部测试"""
    cmd = ["pytest", "-v", "--tb=short"]
    if report:
        cmd.extend(["--html=reports/full_report.html", "--self-contained-html"])
    if parallel:
        cmd.extend(["-n", "auto"])
    print(f"[全量测试] 执行: {' '.join(cmd)}")
    return subprocess.run(cmd).returncode


def check_prerequisites():
    """检查前置条件"""
    import requests

    print("=" * 60)
    print("检查前置条件...")
    print("=" * 60)

    # 检查后端
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

    # 检查前端
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
    parser = argparse.ArgumentParser(description="Niko动漫 自动化测试运行器")
    parser.add_argument("--mode", choices=["api", "e2e", "all"],
                        default="api", help="测试模式 (默认: api)")
    parser.add_argument("--report", action="store_true",
                        help="生成 HTML 测试报告")
    parser.add_argument("--parallel", action="store_true",
                        help="并行运行测试")
    parser.add_argument("--headed", action="store_true",
                        help="E2E 测试显示浏览器窗口")
    parser.add_argument("--skip-check", action="store_true",
                        help="跳过前置条件检查")

    args = parser.parse_args()

    if not args.skip_check:
        if not check_prerequisites():
            sys.exit(1)

    # 确保在 tests 目录下运行
    os.chdir(os.path.dirname(os.path.abspath(__file__)))

    # 创建报告目录
    os.makedirs("reports", exist_ok=True)

    print("=" * 60)
    print(f"开始运行 {args.mode.upper()} 测试...")
    print("=" * 60)

    if args.mode == "api":
        exit_code = run_api_tests(report=args.report, parallel=args.parallel)
    elif args.mode == "e2e":
        exit_code = run_e2e_tests(report=args.report, headed=args.headed)
    else:
        exit_code = run_all_tests(report=args.report, parallel=args.parallel)

    print("=" * 60)
    if exit_code == 0:
        print("测试全部通过!")
    else:
        print(f"测试失败 (退出码: {exit_code})")
    print("=" * 60)

    sys.exit(exit_code)


if __name__ == "__main__":
    main()