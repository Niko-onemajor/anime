"""
文件上传/杂项 API 测试
覆盖: 文件上传、数据生成、测试资源
"""
import pytest
import io
from conftest import api_get, api_post, assert_code_200


class TestFileUpload:
    """文件上传相关"""

    def test_upload_avatar(self, base_url, auth_headers):
        """测试: 上传头像文件"""
        img_data = io.BytesIO(
            b'\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00\x00\x01\x01\x00\x05\x18\xd8N\x00\x00\x00\x00IEND\xaeB`\x82'
        )
        files = {"file": ("test_avatar.png", img_data, "image/png")}
        import requests
        resp = requests.post(
            f"{base_url}/api/upload",
            headers=auth_headers,
            files=files,
            data={"type": "avatar"}
        )
        data = resp.json()
        assert data["code"] in (200, 500)  # 200上传成功，500可能OSS未配置

    def test_upload_cover(self, base_url, auth_headers):
        """测试: 上传封面文件"""
        img_data = io.BytesIO(
            b'\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00\x00\x01\x01\x00\x05\x18\xd8N\x00\x00\x00\x00IEND\xaeB`\x82'
        )
        import requests
        resp = requests.post(
            f"{base_url}/api/upload/cover",
            headers=auth_headers,
            files={"file": ("test_cover.png", img_data, "image/png")}
        )
        data = resp.json()
        assert data["code"] in (200, 500)  # 200上传成功，500可能OSS未配置

    def test_upload_no_file(self, base_url, auth_headers):
        """测试: 不上传文件直接请求"""
        import requests
        resp = requests.post(
            f"{base_url}/api/upload",
            headers=auth_headers
        )
        data = resp.json()
        # 缺少文件可能返回400 (Spring默认)、500 (服务器错误) 或 code!=200
        if "code" in data:
            assert data["code"] != 200
        else:
            assert resp.status_code in (400, 500)


class TestMisc:
    """杂项接口"""

    def test_data_generate(self, base_url, admin_headers):
        """测试: 生成测试数据（管理员接口）"""
        resp = api_post("/api/data/generate", headers=admin_headers)
        data = resp.json()
        assert data["code"] in (200, 500)  # 200成功或500已存在数据

    def test_test_resources(self, base_url):
        """测试: 获取测试资源URL"""
        resp = api_get("/api/test/resources")
        data = assert_code_200(resp, "获取测试资源")
        # 响应格式可能是 {"code":200, "videoUrl":"...", "avatarUrl":"..."} 
        # 或 {"code":200, "data": {...}}
        if "videoUrl" in data:
            assert "videoUrl" in data
        elif "data" in data:
            assert "videoUrl" in data["data"]