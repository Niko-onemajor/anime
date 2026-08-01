# Niko动漫

一个基于 Spring Boot + Vue 3 的全栈动漫视频网站，支持动漫浏览、搜索、排行榜、论坛社区、实时聊天、用户社交等功能。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7、Java 21、Spring Data JPA、MySQL 8.0 |
| 前端 | Vue 3.5、TypeScript、Vite 4、Element Plus |
| 认证 | JWT（Access Token + Refresh Token） |
| 实时通信 | WebSocket |
| 测试 | Pytest（198 项自动化测试） |

## 功能模块

- 动漫浏览：列表、详情、集数、分类筛选、年份/字母排序
- 排行榜：周榜、月榜、年榜
- 搜索：动漫搜索 + 用户搜索，支持分类展示
- 论坛：发帖、评论、点赞/点踩、互动
- 消息：实时聊天、会话列表、未读提醒
- 用户：个人主页、关注/粉丝、收藏、评分、观看记录
- 推荐：个性化推荐、热门推荐
- 管理员：用户管理、动漫管理、论坛管理、删除记录恢复

## 项目结构

```
anime/
├── back/anime/                  # Spring Boot 后端
│   └── src/main/java/com/example/anime/
│       ├── controller/          # 控制器层
│       ├── service/             # 业务逻辑层
│       ├── repository/          # 数据访问层
│       ├── model/               # 实体模型
│       ├── config/              # 配置（Security、JWT等）
│       └── utils/               # 工具类
├── front/anime_vue3/            # Vue 3 前端
│   └── src/
│       ├── views/pages/         # 页面组件
│       ├── views/admin/         # 管理后台页面
│       └── views/player/        # 播放器组件
└── tests/                       # 自动化测试
    ├── test_api_*.py            # API 接口测试
    ├── test_security.py         # 安全测试（XSS/SQL注入/越权等）
    ├── test_performance.py      # 性能测试
    └── test_e2e_ui.py           # E2E 端到端测试
```

## 快速开始

### 环境要求

- JDK 21
- Node.js 20
- MySQL 8.0
- Python 3.10+（运行测试）

### 后端启动

1. 创建 MySQL 数据库：

```sql
CREATE DATABASE anime CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行建表脚本 `back/anime/src/main/resources/sql_data/schema.sql`

3. 修改 `application.properties` 中的数据库连接配置

4. 启动后端：

```bash
cd back/anime
mvn spring-boot:run
```

### 前端启动

```bash
cd front/anime_vue3
npm install
npm run dev
```

### 运行测试

```bash
cd tests
pip install -r requirements.txt

# 运行全部测试
python run_tests.py --mode all --report

# 仅运行 API 测试
python run_tests.py --mode api
```

## 测试覆盖

- API 接口测试：认证、动漫、论坛、用户社交、文件上传、管理员
- 安全测试：XSS 防护、SQL 注入防护、权限越界、CSRF、Token 安全、路径遍历
- 性能测试：响应时间、并发请求、持续负载
- E2E 测试：页面加载、导航、登录流程、搜索功能

## 许可证

MIT
