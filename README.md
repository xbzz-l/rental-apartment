# 公寓租房系统Demo（三端 + 后端）

基于 [PRD](docs/PRD.md) 实现的轻量租房业务闭环 Demo：**用户端APP → 管理端Web → BI数据看板** 三端联动，共用一套 Spring Boot 后端。

## 项目结构

| 目录 | 说明 | 技术栈 | 端口 |
| --- | --- | --- | --- |
| rental-app | 用户端H5（移动端） | Vue3 + Vant4 + Pinia | 5173 |
| rental-admin | 管理端Web | Vue3 + Element Plus + Pinia | 5174 |
| rental-bi | BI数据看板 | Vue3 + Element Plus + ECharts | 5175 |
| rental-server | 后端服务 | Spring Boot 3 + MyBatis-Plus + MySQL + JWT | 8080 |

> 房源图片存放于后端静态资源目录 `rental-server/src/main/resources/static/images/apartments/`（Spring Boot 默认静态映射对外提供，访问路径形如 `/images/apartments/apt9.jpg`）；三端开发服务器均已配置 `/images` 代理指向 8080，故数据库中的图片字段统一存相对路径。

## 环境要求

- JDK 17+（Maven 已内置在 rental-server/.mvn 中，通过 mvnw.cmd 调用，**无需安装 Maven**）
- Node.js 18+、npm
- MySQL 8.0（本机已配置**便携版**：`D:\AI-code\mysql-8.0.29`，启动脚本自动拉起；迁移到其他电脑时，请安装 MySQL 或将便携版放到该路径，并确认 [application.yml](rental-server/src/main/resources/application.yml) 的账号密码 root/root）

## 启动方式

### 方式一：一键启动（Windows，推荐）

双击项目根目录的 **`start-all.bat`**，脚本会自动：

1. 启动便携版 MySQL 8（3306 端口，已在运行则跳过，最多等待 30 秒）；
2. 启动后端（8080）、用户端（5173）、管理端（5174）、BI看板（5175）共五个独立窗口；
3. 自动打开三个浏览器页面。

### 方式二：手动启动

1. **启动 MySQL**，确认连接配置（默认 root/root），可在 [application.yml](rental-server/src/main/resources/application.yml) 中修改；
2. **启动后端**（Windows 用 mvnw.cmd，Mac/Linux 安装 Maven 后用 mvn）：
   ```bash
   cd rental-server
   mvnw.cmd spring-boot:run
   ```
   首次启动会自动建库建表并写入初始数据（依赖走阿里云镜像）；接口文档见 http://localhost:8080/doc.html
3. **启动三端前端**（分别执行）：
   ```bash
   cd rental-app && npm install && npm run dev     # 用户端 http://localhost:5173
   cd rental-admin && npm install && npm run dev   # 管理端 http://localhost:5174
   cd rental-bi && npm install && npm run dev      # BI看板 http://localhost:5175
   ```

### 方式三：Docker 一键部署（服务器推荐）

只需安装 Docker（含 Compose v2），**不需要**在服务器上装 JDK / Node / MySQL。

```bash
cp .env.example .env          # 按需改端口、数据库密码、管理员密码
docker compose up -d --build  # 首次构建约 5-15 分钟（拉依赖）
docker compose ps             # 查看五个容器状态
```

启动后访问：

| 端 | 地址（默认端口，可在 `.env` 改） |
| --- | --- |
| 用户端 H5 | http://服务器IP:8081 |
| 管理端 | http://服务器IP:8082 |
| BI 看板 | http://服务器IP:8083 |
| 接口文档 | http://服务器IP:8081/doc.html（由 nginx 转发） |

**编排结构**

```
浏览器 ──> nginx(app/admin/bi 三个容器) ──/api、/images──> server ──> mysql
          仅这三个端口对外暴露              不映射端口      不映射端口
```

- **后端与数据库不暴露到宿主机**：只有三个前端端口对外，`/api` 与 `/images` 由各容器的 nginx 同源反代（因此也不存在跨域问题）。
- **`server.address` 会被覆盖为 `0.0.0.0`**：`application.yml` 里为本地开发锁的是 `127.0.0.1`，容器内必须监听所有网卡才能被 nginx 访问；宿主机侧仍无端口映射。
- **数据持久化**：MySQL 数据在具名卷 `<项目名>-mysql-data`，`docker compose down` 不会丢数据，`down -v` 才会删。
- **建表与初始数据**：后端启动时自动执行 `schema.sql` 并播种 18 套房源与近 5 个月历史成交，空库即可直接跑。
- **多服务器/多套部署**：每台机器各自 `up -d` 即可；同一台机器要跑多套，改 `.env` 里的 `COMPOSE_PROJECT_NAME` 与三个端口；多台机器共用一个数据库，改 `DB_HOST` / `DB_PORT`。

常用运维命令：

```bash
docker compose logs -f server     # 跟踪后端日志
docker compose restart server     # 改完后端配置后重启
docker compose up -d --build app  # 只重新构建用户端
docker compose down               # 停止（保留数据卷）
```

> 提示：`.env` 含数据库密码，已被 `.gitignore` 排除，不要提交到仓库。生产环境务必改掉默认的 `MYSQL_ROOT_PASSWORD` 与 `ADMIN_PASSWORD`。

## 默认账号

| 端 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 管理端Web | admin | admin123 | 内置管理员（配置在 `application.yml` 的 `app.admin`，不存于 user 表） |
| 用户端APP | user | user123 | 普通用户（也可自行注册） |
| 用户端APP | zhangwei / lina / wangqiang / liumin / chenchen | 123456 | 演示客户账号（历史成交记录的归属用户） |
| BI看板 | admin | admin123 | 与管理端共用内置管理员账号 |

> 管理员账号不属于 C 端用户体系：它不写入 `user` 表，因此不会出现在用户端的注册/登录体系与管理端的「用户列表」中。

## 主要接口一览

| 模块 | 接口 | 说明 |
| --- | --- | --- |
| 认证 | POST /api/auth/register、/api/auth/login | 注册 / 登录（返回JWT） |
| 用户 | GET/PUT /api/user/info | 查询 / 修改个人信息（需登录） |
| 房源 | GET /api/apartments、GET /api/apartments/{id} | 房源列表（筛选）/ 详情（公开） |
| 房源管理 | POST/PUT/DELETE /api/admin/apartments | 新增 / 编辑 / 删除房源（需管理员） |
| 预约 | POST /api/bookings、GET /api/bookings/my、PUT /api/bookings/{id}/cancel | 提交预约 / 我的租房记录 / 取消预约（需登录） |
| 用户管理 | GET /api/admin/users、GET /api/admin/users/{id}/bookings、DELETE /api/admin/users/{id} | 用户列表 / 用户租房记录 / 删除用户（需管理员） |
| 统计 | GET /api/admin/stats/overview、GET /api/admin/stats/bookings、GET /api/admin/stats/bi | 管理端统计 / 预约明细下钻 / BI看板数据（均需管理员） |

## 核心业务规则（Demo 简化）

- 初始数据采用**幂等播种**（重复启动只补齐缺失部分，不覆盖已有数据）：18 套房源（8 套普通 + 10 套豪宅）、6 个账号（user + 5 个演示客户）、近 5 个月的「已成交」历史记录；**当月不产生任何数据**，由用户实际操作累积；
- 用户端需先登录后看房（默认 user/user123，可自行注册新账号）；
- 用户提交预约即生成租房记录，对应房源状态由「可租」变更为「已租」；
- 用户可在「我的 - 我的租房记录」中取消「已预约」记录（二次确认），取消后房源恢复「可租」；同一房源若还有其他有效预约则不释放；
- 管理员可在「用户管理」中删除不再使用的账号，其名下租房记录一并删除，占用的房源同步释放；
- 管理端「业务统计」卡片支持点击下钻：用户/房源类跳转到对应列表并自动带上筛选，预约量类弹出明细列表；
- BI 看板以「房源规模 + 营业额」为核心：当月营业额 = 该月未取消预约记录所对应房源的月租金合计，卡片同时给出当日营业额与环比上月，图表展示近 6 个月营业额对比；
- 无支付与审核环节，`booking.status` 保留扩展能力（已预约/已成交/已取消）；统计口径中的预约量均不含「已取消」记录。
