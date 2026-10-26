# 广州大学生租房推荐系统

面向广州高校学生的租房平台，**Spring Boot 3 + Vue 3 前后端分离**。除常规的房源发布、预约看房、
在线签约外，核心是**租房推荐引擎**：基于物品/用户的协同过滤、余弦相似度与校园位置距离，
为每个学生生成个性化房源排序。

项目为本科毕业设计作品。

---

## 功能概览

### 前台（学生 / 房东）

| 模块 | 功能 |
| --- | --- |
| 账号 | 注册、登录、忘记密码、实名/学生认证、信用分 |
| 房源 | 列表、详情、多图、地图定位（高德）、按校区/预算/户型筛选 |
| 推荐 | 个性化推荐、相似房源、热门榜、冷启动推荐、搜索词推荐 |
| 预约 | 租客发起看房预约，房东确认/拒绝 |
| 收藏 | 收藏房源、我的收藏 |
| 订单与签约 | 创建订单、生成 PDF 租赁合同、签署 |
| 租约 | 我的租约、到期提醒（定时任务）、续租/退租 |
| 交易 | 押金与租金流水记录 |
| 社区 | 论坛分区、发帖、回复、敏感词过滤 |
| 互动 | 房源评价、留言、站内信 |
| 其他 | 公告、轮播图、意见反馈 |

### 后台（管理员）

数据看板、用户审核与封禁、房源审核、预约管理、订单管理、租约管理、交易流水、
违规与信用记录、论坛管理、评价管理、公告与轮播图管理、反馈处理、数据统计。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3、Spring Security（仅用于 BCrypt）、MyBatis-Plus、JWT（auth0）、Redis |
| 文档 | Knife4j / OpenAPI 3 |
| 文档导出 | PDFBox（合同生成）、EasyExcel/Apache POI（Excel 导出） |
| 前端 | Vue 3、Vite 4、Vue Router、Pinia + Vuex、Element Plus、ECharts 5、高德地图 JSAPI、xlsx |
| 数据库 | MySQL 8（19 张表） |

## 项目结构

```
.
├── backend/                              # Spring Boot 后端
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd / .mvn/           # Maven Wrapper
│   └── src/main/
│       ├── java/org/example/springboot/
│       │   ├── SpringbootApplication.java
│       │   ├── common/                   # Result / ResultCode
│       │   ├── config/                   # Security、JWT 拦截器、Redis、MyBatis-Plus、Knife4j、Web
│       │   ├── controller/               # 21 个 REST 控制器
│       │   ├── DTO/                      # 统计、改密等传输对象
│       │   ├── entity/                   # 19 个实体
│       │   ├── enumClass/                # 账号状态、订单状态、文件类型、交易类型
│       │   ├── exception/                # 全局异常处理
│       │   ├── mapper/                   # 19 个 Mapper（3 个带 XML）
│       │   ├── scheduled/                # 租约到期定时任务
│       │   ├── service/                  # 24 个服务
│       │   └── util/                     # JWT、合同生成、Excel 导出、敏感词过滤、文件、日期
│       └── resources/
│           ├── application.properties    # 连接信息全部走环境变量
│           ├── mapper/*.xml              # Favorite / Message / UserBehavior
│           └── fonts/simfang.ttf         # 合同 PDF 所需中文字体
├── frontend/                             # Vue 3 前端
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js                    # 开发代理 /api → localhost:1236
│   ├── .env / .env.development / .env.production
│   └── src/
│       ├── main.js / App.vue
│       ├── api/                          # favorite / message / recommendation / user
│       ├── components/                   # 地图容器、轮播、房源列表、后台导航
│       ├── layouts/                      # FrontendLayout / BackendLayout
│       ├── router/index.js
│       ├── store/                        # Pinia app + Vuex user
│       ├── styles/  utils/
│       └── views/
│           ├── frontend/                 # 17 个前台页面
│           ├── backend/                  # 25 个后台页面
│           └── auth/                     # 登录 / 注册 / 统一认证页
├── db/
│   └── init.sql                          # 建库 + 19 张表 + 初始数据（79 KB）
├── README.md
├── 启动说明.md
├── .env.example
└── .gitignore
```

规模：后端 110 个 Java 文件 / 15,420 行；前端 55 个 `.vue` / 11 个 `.js`。

## 快速开始

> 完整步骤与常见问题见 **[启动说明.md](启动说明.md)**，下面是精简版。

### 1. 环境要求

| 组件 | 版本 |
| --- | --- |
| JDK | 17+ |
| Maven | 3.6+（或用自带 `mvnw`） |
| Node.js | 18+ |
| MySQL | 8.0 |
| **Redis** | 6+（**必需**，推荐缓存依赖它） |

### 2. 导入数据库

```bash
mysql -u root -p < db/init.sql
```

脚本会 `DROP DATABASE IF EXISTS kb251018` 再重建，**执行前确认这个库里没有你要保留的数据**。

### 3. 设置环境变量

```powershell
# Windows PowerShell
$env:DB_PASSWORD = "你的 MySQL 密码"
```

```bash
# macOS / Linux
export DB_PASSWORD="你的 MySQL 密码"
```

Redis 用默认 `localhost:6379` 无密码即可；不是的话再设 `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD`。

### 4. 启动后端

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

服务监听 `http://localhost:1236`。接口文档：`http://localhost:1236/doc.html`（Knife4j）。

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:3000`。Vite 已配代理，`/api` 转发到 `1236` 并保留前缀。

## 推荐引擎

核心实现集中在 `service/CollaborativeFilteringService.java`（约 1680 行），另有
`UserBehaviorService` 负责行为采集（浏览/收藏/预约/成交）。

| 能力 | 实现 |
| --- | --- |
| 物品协同过滤 | 由用户交互过的房源出发，用**余弦相似度**找相似房源，按 `相似度 × 行为权重` 累加打分 |
| 用户协同过滤 | 基于共同浏览/收藏计算用户相似度，推荐相似用户偏好的房源 |
| 冷启动 | 新用户无行为时，按校区距离 + 预算匹配 + 热度回退 |
| 校园距离 | 内置广州主要高校坐标表，按经纬度算直线距离参与排序 |
| 行为数据 | `user_behavior` 表记录行为类型与权重，`UserBehaviorMapper.xml` 做聚合 |
| 缓存 | `@Cacheable("recommendations")` 缓存推荐结果，TTL 1 小时（`spring.cache.redis.time-to-live`） |

接口：`/api/recommendation/personalized`、`/similar/{houseId}`、`/hot`、`/cold-start`、
`/search-keywords`、`/preference-based`。

## 其他实现要点

- **合同生成**：`ContractGenerator` 用 PDFBox + 内嵌仿宋字体直接绘版出 PDF，不依赖 Word 模板
- **Excel 导出**：`ExcelExportUtils` 统一格式化导出统计与流水
- **敏感词过滤**：`SensitiveWordFilter` 用于论坛发帖与评价
- **租约到期提醒**：`scheduled/LeaseExpirationTask` 定时扫描到期租约
- **信用分**：`credit_record` 表记录加减分事由，`violation` 表记录违规

## ⚠️ 代码审核结论

搭建仓库时对后端做了逐文件检查，以下是**需要你知道的问题**。README 如实记录，未做修复——
因为项目当前无法端到端运行（见上一节），盲改鉴权有把功能改坏的风险。

### 严重：全站接口没有鉴权

两个配置叠加，导致认证和授权**完全失效**：

**1. JWT 拦截器把自己排除掉了**

`config/WebConfig.java`：

```java
registry.addInterceptor(jwtInterceptor)
        .addPathPatterns("/api/**")     // 拦截 /api/**
        .excludePathPatterns("/api/**") // 又把自己排除掉
        .excludePathPatterns(PUBLIC_PATHS);
```

`addPathPatterns` 与 `excludePathPatterns` 都是 `/api/**`，排除规则优先 →
**拦截器对所有 API 请求都不执行**。

**2. `@PreAuthorize` 全部失效**

后端共 **40 处** `@PreAuthorize("hasRole('ADMIN')")` 之类的注解，分布在 9 个控制器里。
但全项目搜索不到 `@EnableMethodSecurity`（Spring Security 6 必须显式开启才会处理方法级注解），
也没有 `@EnableGlobalMethodSecurity`。**这 40 个注解目前是纯装饰，没有任何效果。**

**3. `SecurityConfig` 放行一切**

```java
.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
.csrf(csrf -> csrf.disable())
```

**结论**：任何人不带 token 直接请求 `/api/order/list`、`/api/user/list` 等接口，
预期都能拿到数据；普通用户调用管理员接口也不会被拦。

**建议修复顺序**（供参考，未实施）：

1. 把 JWT 拦截器的 `excludePathPatterns("/api/**")` 删掉，只保留 `PUBLIC_PATHS`
2. 加 `@EnableMethodSecurity(prePostEnabled = true)` 让 40 个 `@PreAuthorize` 生效
3. 确认 `UserDetailsServiceImpl` 给用户装配了 `ROLE_XXX` 权限，否则 `hasRole` 仍会全部拒绝
4. 逐接口回归，把确实该公开的接口补进 `PUBLIC_PATHS`

### 中等

| 问题 | 位置 | 说明 |
| --- | --- | --- |
| Token 泄漏到日志 | `JwtInterceptor` | 校验失败时 `LOGGER.error(..., " token=" + token)` 把完整 token 写进日志 |
| Token 可从 URL 传 | `JwtInterceptor` / `JwtTokenUtils` | 支持 `?token=xxx`，会进入浏览器历史、代理日志、Referer |
| JWT 有效期硬编码 | `JwtTokenUtils.genToken` | `offsetHour(now, 2)`，2 小时固定，无刷新机制 |
| JWT 密钥即密码哈希 | `JwtTokenUtils` | 用 BCrypt 哈希当 HMAC 密钥；改密码即失效（安全），但密钥不可独立轮换 |
| 明文开发口令 | `application.properties` | `spring.security.user` 与 `knife4j.basic` 都是 `admin/admin`（已改成可配置，但默认值仍在） |
| 全局默认密码 | `application.properties` | `user.defaultPassword=123456`，注册用户初始口令固定 |
| CSRF 关闭 | `SecurityConfig` | 若将来改用 Cookie 会话需重新开启 |

### 轻微

- `springdoc.paths-to-match= /**` 会把内部接口一并暴露到 Swagger
- `result` 被 `User` 实体直接返回时依赖 `@JsonIgnore`，需确认密码字段不会序列化出去
- `files/` 目录（运行时上传的房源图、生成的合同 PDF）未纳入仓库，需程序首次运行时自建

## 数据库

`db/init.sql` 创建 19 张表，与实体一一对应：

```
user  house  house_type  house_comment  favorite  user_behavior
order  lease_record  appointment  transaction  credit_record  violation
forum_category  forum_post  forum_reply  message  feedback
announcement  carousel
```

## 仓库说明

本仓库只收录**项目代码与数据库脚本**，未包含：

| 排除项 | 原因 |
| --- | --- |
| `frontend/node_modules/` | 229 MB 依赖 |
| `backend/target/` | Maven 构建产物 |
| `backend/files/img/`、`backend/files/contract/` | 运行时上传的房源图片与生成的合同 PDF（3 MB） |
| `文档/`（任务书、初稿 docx/pdf）、`演示视频/` | 论文与演示材料，非代码 |

## 许可

本项目仅用于学习与毕业设计交流，不用于商业用途。
