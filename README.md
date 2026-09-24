# 健康管理系统

包含用户端 App、管理端 PC 后台和 Spring Boot 后端。项目已按答辩目标整理，只保留登录注册、健康看板、饮食记录、健康档案、心率记录、健康计划、计划打卡、文章收藏、个人资料、基础设置和后台管理等核心功能。

## 项目结构

```text
health
├─ healthapp/              # 用户端 uni-app / Vue 3 项目
├─ adminPC/                # 管理端 Vue 3 + Vite 项目
├─ spring/                 # Spring Boot + MyBatis 后端
├─ health_db.sql           # 数据库脚本备份
└─ README.md
```

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 用户端 | uni-app、Vue 3、本地缓存、原生 Android 心率插件 |
| 管理端 | Vue 3、Vue Router、Vite、通用 CRUD 页面 |
| 后端 | Spring Boot 3、MyBatis、MySQL、JWT、BCrypt |
| 数据库 | MySQL、utf8mb4 |

## 核心功能

### 用户端

- 登录、注册、会话恢复、退出登录、修改密码。
- 首页健康看板，展示健康档案、饮食、心率、计划、步数等摘要。
- 饮食记录查询、新增、删除，并支持食物营养估算。
- 健康档案查询、保存，自动计算 BMI、BMR。
- 心率记录查询、手动保存、原生 App 摄像头测心率结果保存。
- 健康计划列表、创建、详情、删除。
- 计划打卡、防重复打卡、计划进度更新。
- 文章收藏、取消收藏、我的收藏。
- 个人资料查看、修改、头像上传与清除。
- 基础设置、目标设置、隐私设置。

### 管理端

- 管理员登录。
- 后台统计总览。
- 用户管理。
- 饮食记录管理。
- 健康档案管理。
- 心率记录管理。
- 用户计划管理。
- 打卡记录管理。
- 收藏管理。
- 用户设置管理。

## 后端数据库

默认数据库名为 `health_db`，后端连接配置位于：

```text
spring/src/main/resources/application.yml
```

默认连接信息：

```yaml
server:
  port: 3030

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/health_db
    username: root
    password: 123456
```

推荐先执行表结构脚本：

```text
spring/src/main/resources/schema.sql
```

也可以根据需要使用根目录的 `health_db.sql` 恢复已有数据。

可通过环境变量覆盖数据库配置：

```powershell
$env:DB_URL="jdbc:mysql://127.0.0.1:3306/health_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="123456"
$env:APP_DATA_DIR="./data"
$env:APP_AVATAR_DIR="./uploads/avatar"
```

## 启动方式

### 1. 启动后端

```powershell
cd spring
mvn spring-boot:run
```

后端默认地址：

```text
http://127.0.0.1:3030
```

### 2. 启动管理端

```powershell
cd adminPC
npm install
npm run dev
```

管理端默认通过 Vite 启动，接口地址默认读取：

```text
http://127.0.0.1:3030
```

如需修改接口地址，可以设置：

```powershell
$env:VITE_API_BASE_URL="http://127.0.0.1:3030"
```

### 3. 启动用户端

用户端为 uni-app 项目，建议使用 HBuilderX 打开 `healthapp` 目录运行到 App 或浏览器。

用户端接口地址配置：

```text
healthapp/config/api.config.js
```

当前默认值：

```js
BASE_URL: 'http://127.0.0.1:3030'
```

摄像头测心率以原生 App 插件分支为准，浏览器摄像头采样入口已移除。

## 测试与检查

### 用户端检查

```powershell
cd healthapp
npm run check:all
```

该命令会依次检查：

- JS 语法。
- Vue SFC 编译。
- `pages.json` 路由与页面文件一致性。
- PPG 心率算法基础稳定性。

### 管理端构建

```powershell
cd adminPC
npm run build
```

### 后端测试

```powershell
cd spring
mvn test
```

当前后端没有单独的 `src/test` 测试用例，`mvn test` 主要用于验证依赖、资源和 Java 编译链路。

## 主要接口

### 用户端接口

| 功能 | 接口 |
| --- | --- |
| 注册 | `POST /api/auth/register` |
| 登录 | `POST /api/auth/login` |
| 会话恢复 | `GET /api/auth/session` |
| 退出登录 | `POST /api/auth/logout` |
| 首页看板 | `GET /api/dashboard` |
| 饮食记录 | `GET /api/diet`、`POST /api/diet`、`DELETE /api/diet/{id}` |
| 健康档案 | `GET /api/health/record`、`POST /api/health/record` |
| 心率记录 | `GET /api/heart`、`POST /api/heart` |
| 健康计划 | `GET /api/plans`、`POST /api/plans`、`GET /api/plans/{id}`、`DELETE /api/plans/{id}` |
| 计划打卡 | `POST /api/plans/{id}/checkin` |
| 收藏文章 | `GET /api/article-favorite`、`POST /api/article-favorite`、`DELETE /api/article-favorite/{id}` |
| 个人资料 | `GET /api/profile`、`PUT /api/profile` |
| 头像 | `POST /api/profile/avatar`、`DELETE /api/profile/avatar` |
| 用户设置 | `GET /api/settings`、`PUT /api/settings` |
| 修改密码 | `PUT /api/user/password` |

### 管理端接口

| 功能 | 接口 |
| --- | --- |
| 管理员登录 | `POST /api/admin/login` |
| 后台统计 | `GET /api/admin/stats` |
| 用户管理 | `/api/admin/users` |
| 饮食记录管理 | `/api/admin/diet-records` |
| 健康档案管理 | `/api/admin/health-records` |
| 心率记录管理 | `/api/admin/heart-records` |
| 用户计划管理 | `/api/admin/user-plans` |
| 打卡记录管理 | `/api/admin/plan-checkins` |
| 收藏管理 | `/api/admin/article-favorites` |
| 用户设置管理 | `/api/admin/user-settings` |

## 数据文件

后端保留两个运行所需 JSON 数据文件：

```text
spring/data/food-nutrition.json
spring/data/plan-templates.json
```

用户端也保留默认食物库兜底文件：

```text
healthapp/static/data/food-nutrition.default.json
```

这些文件用于饮食营养估算和健康计划模板，不建议删除。

