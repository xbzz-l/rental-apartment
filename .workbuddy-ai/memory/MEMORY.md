# 项目长期约定

## 用户身份字段规则（2026-10-14 确立）

- **昵称统一等于账号**：`user.nickname` 是只读镜像字段，注册时由后端设为 `username`，前端不提供任何修改入口。
- **展示层回退链固定为 `realName || username`**，不要再引入 `nickname`——历史遗留昵称会污染展示（曾导致 `user` 账号显示为 "xbzz"）。
- **注册只收 账号 + 密码 + 确认密码**；确认密码是纯前端校验（vant rule validator），后端 `RegisterDTO` 不含 `passwordConfirm`。
- **真实信息**（`real_name` / `age` / `phone` / `cohabitants`）在「我的 - 编辑资料」中填写，是租房登记用资料。
- `schema.sql` 每次启动执行，含幂等自愈语句（加列用 `information_schema` 判断 + `PREPARE/EXECUTE`；昵称归一用无条件 UPDATE）。

## 用户端（rental-app）布局约定（2026-10-14 确立）

- **手机框 + 内部滚动**：`body { overflow: hidden }`，`#app { height: 100%; overflow: hidden }`，滚动由 `.app-scroll` 承担（App.vue 里包裹 router-view），`van-tabbar` 是 `.app-scroll` 的兄弟节点。
- **桌面端 `#app` 必须带 `transform: translateZ(0)`**：这让 `#app` 成为内部 `position: fixed` 元素的包含块，nav-bar / tabbar / 下拉筛选 / 底部弹层 / 遮罩会自动收进 430px 手机框。**不要再给 Vant 组件写 `left: 50%; margin-left: -215px` 之类的手工居中覆写。**
- 页面根元素用 `min-height: 100%`（不是 `100vh`）。

## 账号体系约定（2026-10-14 确立）

- **`user` 表只存 C 端普通用户，不含管理员**。`schema.sql` 每次启动 `DELETE FROM user WHERE role='admin'`。
- **管理员是内置账号**：`application.yml` 的 `app.admin.username / password`，由 `UserService.login` 直接构造内存对象（id=0，role=admin）签发 JWT，不查库。改密码改配置即可。
- **用户端拒绝管理员登录**：`rental-app/src/store/user.js` 的 login 里校验 `role === 'admin'` 就抛错。
- 管理端 / BI 前端不调用 `/api/user/info`，只把登录响应的 user 存 localStorage —— 所以内置管理员没有数据库行也没问题。

## 静态资源与初始数据约定（2026-10-14 确立）

- **房源图片放后端静态资源**：`rental-server/src/main/resources/static/images/apartments/*.jpg`（AI 生成后压缩为 1200×800 JPG，单张约 160-240KB）。数据库 `apartment.images` 存**相对路径** `/images/apartments/aptN.jpg`，三端 `vite.config.js` 均已加 `/images` 代理指向 8080；不要改回 `http://localhost:8080/...` 绝对地址。
- **DataSeeder 是幂等播种器**：按维度分别判重（用户按 username、房源按 title、历史预约按「本月之前是否已有记录」），重复启动只补齐缺失部分，不覆盖已有数据。
- **初始数据规模**：18 套房源（8 普通 + 10 豪宅）、6 个账号（user + zhangwei/lina/wangqiang/liumin/chenchen）、近 5 个月「已成交」历史记录；**当月不产生数据**（留给用户自行演示）。
- **历史预约日期必须用「今天减 N 个月再加偏移天数」计算**：BI 近 6 个月 SQL 条件是 `create_time >= DATE_SUB(CURDATE(), INTERVAL 5 MONTH)`，写死月中日期会落到窗口外被漏掉（曾导致 5 月营业额少算 2200）。
- **原 8 套房源仍用 picsum 外链占位图**，新增 10 套用内置图片；`buildImages()` 以「是否以 `/` 开头」区分两种模式。

## Docker 部署约定（2026-10-19 确立）

- **编排入口是根目录 `docker-compose.yml`**，五个服务 mysql/server/app/admin/bi；三端前端由 nginx 托管静态产物并同源反代 `/api`、`/images`，所以**不要在前端写死后端地址**，`baseURL: '/api'` 是唯一正确写法。
- **只对外暴露三个前端端口**（`.env` 的 APP_PORT/ADMIN_PORT/BI_PORT，默认 8081/8082/8083）；后端 8080、MySQL 3306 不映射宿主机。
- **`application.yml` 的 `server.address: 127.0.0.1` 是本地开发用的**，容器里必须用 `SERVER_ADDRESS=0.0.0.0` 覆盖，否则 nginx 反代 502。改这一项时别直接删 yml 里的配置。
- **密码类配置全部走环境变量**（`MYSQL_ROOT_PASSWORD`、`JWT_SECRET`、`ADMIN_PASSWORD`），`.env` 已进 `.gitignore`，只提交 `.env.example`。
- **三端共用 `docker/web.Dockerfile`**，靠 `--build-arg APP_DIR=rental-app|rental-admin|rental-bi` 区分；不要为每端复制一份 Dockerfile。
- 时区三处必须一致（容器 TZ、JDBC serverTimezone、MySQL default-time-zone），否则 BI 按月的营业额统计会跨月错位。

## 环境注意事项

- 沙箱会注入 `SERVER__PORT=32212`，本地跑后端需 `unset SERVER__PORT SERVER__HOST` 或加 `--server.port=8080`。用户自己双击 `start-all.bat` 不受影响。
- 便携版 MySQL 在 `D:\AI-code\mysql-8.0.29`，库 `rental_db`，账号 root/root。
- 前端三端 Vite dev server：app 5173 / admin 5174 / bi 5175，均支持 HMR，改 `.vue` 无需重启。

## 验证方式

无头浏览器截图：playwright-core（装在 `C:\Users\23655\.workbuddy-ai\binaries\node\workspace`）+ 本机 Edge `C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe`。桌面 1440x900、移动 430x932。
