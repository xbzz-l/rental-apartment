package com.rental.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rental.entity.Apartment;
import com.rental.entity.Booking;
import com.rental.entity.User;
import com.rental.mapper.ApartmentMapper;
import com.rental.mapper.BookingMapper;
import com.rental.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 初始数据初始化（幂等播种，可重复启动）
 * <p>
 * 各维度独立判重，只补齐缺失的部分，不覆盖已有数据：
 * - 账号：user/user123（普通用户）+ 若干演示客户；管理员为内置账号（application.yml 的 app.admin），不落库
 * - 房源：18 套（原 8 套 + 新增 10 套豪宅），按标题判重
 * - 历史预约：近 5 个月的「已成交」记录（用于 BI 看板展示营业额曲线），当月不产生数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserMapper userMapper;
    private final ApartmentMapper apartmentMapper;
    private final BookingMapper bookingMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 内置房源图片前缀：图片存放于后端静态资源 static/images/apartments/，由 Spring Boot 默认静态映射对外提供 */
    private static final String IMG_PREFIX = "/images/apartments/";

    /** 启动时执行：各维度分别判重后补齐，保证幂等 */
    @Override
    public void run(String... args) {
        int users = seedUsers();
        int apartments = seedApartments();
        int bookings = seedHistoryBookings();
        log.info("初始数据检查完成（本次新增：用户 {} 个 / 房源 {} 套 / 历史成交记录 {} 条）",
                users, apartments, bookings);
    }

    // ============================ 用户 ============================

    /** 初始化账号：演示用户 + 若干客户（用于历史成交记录归属） */
    private int seedUsers() {
        int count = 0;
        count += createUserIfAbsent("user", "user123", "王小明", 28, "13800000002",
                "李小红（配偶，13800000003）");
        count += createUserIfAbsent("zhangwei", "123456", "张伟", 32, "13900000001", "独居");
        count += createUserIfAbsent("lina", "123456", "李娜", 26, "13900000002",
                "王芳（室友，13900000009）");
        count += createUserIfAbsent("wangqiang", "123456", "王强", 35, "13900000003",
                "赵敏（配偶，13900000008）");
        count += createUserIfAbsent("liumin", "123456", "刘敏", 29, "13900000004", "独居");
        count += createUserIfAbsent("chenchen", "123456", "陈晨", 31, "13900000005",
                "陈建国（父亲，13900000006）");
        return count;
    }

    /** 按账号判重创建用户：昵称统一等于账号，密码使用 BCrypt 加密 */
    private int createUserIfAbsent(String username, String rawPassword, String realName,
                                   Integer age, String phone, String cohabitants) {
        if (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
            return 0;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname(username);
        user.setRealName(realName);
        user.setAge(age);
        user.setPhone(phone);
        user.setCohabitants(cohabitants);
        user.setAvatar("");
        user.setRole("user");
        userMapper.insert(user);
        return 1;
    }

    // ============================ 房源 ============================

    /** 初始化房源：原 8 套普通房源 + 新增 10 套豪宅，按标题判重 */
    private int seedApartments() {
        int count = 0;

        // ---- 原有 8 套（图片沿用在线占位图） ----
        count += createApartmentIfAbsent("阳光花园·温馨一居", "一室一厅", "45", 2200,
                "空调,冰箱,洗衣机,WiFi", "幸福市阳光区花园路1号", "apt1");
        count += createApartmentIfAbsent("城东公寓·精致两居", "两室一厅", "68", 3200,
                "空调,冰箱,洗衣机,热水器", "幸福市城东区建设大道88号", "apt2");
        count += createApartmentIfAbsent("中心广场·Loft公寓", "Loft", "50", 2800,
                "空调,冰箱,WiFi,电梯", "幸福市中央区广场路6号", "apt3");
        count += createApartmentIfAbsent("学府名苑·三室大宅", "三室两厅", "108", 4500,
                "空调,冰箱,洗衣机,车位", "幸福市文教区学府路12号", "apt4");
        count += createApartmentIfAbsent("滨江豪庭·江景两居", "两室两厅", "85", 4000,
                "空调,冰箱,洗衣机,车位,健身房", "幸福市滨江区江景大道5号", "apt5");
        count += createApartmentIfAbsent("静安里·单身公寓", "开间", "30", 1600,
                "空调,冰箱,WiFi", "幸福市静安区静安里3号", "apt6");
        count += createApartmentIfAbsent("云顶国际·高层一居", "一室一厅", "52", 2600,
                "空调,冰箱,洗衣机,电梯", "幸福市云顶区云顶路9号", "apt7");
        count += createApartmentIfAbsent("绿洲家园·舒适两居", "两室一厅", "75", 3500,
                "空调,冰箱,洗衣机,WiFi,车位", "幸福市绿洲区绿洲街21号", "apt8");

        // ---- 新增 10 套（图片为项目内置静态资源 static/images/apartments/*.jpg） ----
        count += createApartmentIfAbsent("汤臣一品·外滩豪景大平层", "四室两厅", "320", 88000,
                "中央空调,地暖,新风系统,私人电梯,管家服务,车位", "幸福市外滩滨江大道1号 汤臣一品",
                IMG_PREFIX + "apt9.jpg");
        count += createApartmentIfAbsent("汤臣一品·滨江臻藏", "三室两厅", "220", 62000,
                "中央空调,地暖,新风系统,江景阳台,管家服务,车位", "幸福市外滩滨江大道1号 汤臣一品",
                IMG_PREFIX + "apt10.jpg");
        count += createApartmentIfAbsent("华府天地·都市核心", "三室两厅", "180", 45000,
                "中央空调,地暖,智能家居,健身房,车位", "幸福市中央商务区华府路8号",
                IMG_PREFIX + "apt11.jpg");
        count += createApartmentIfAbsent("翠湖天地·御苑", "两室两厅", "150", 38000,
                "中央空调,地暖,新风系统,公园景观,车位", "幸福市翠湖区御苑街6号",
                IMG_PREFIX + "apt12.jpg");
        count += createApartmentIfAbsent("星河湾·奢装大宅", "四室两厅", "260", 52000,
                "中央空调,地暖,私人影音室,双车位,管家服务", "幸福市星河湾大道18号",
                IMG_PREFIX + "apt13.jpg");
        count += createApartmentIfAbsent("仁恒滨江园·江畔名邸", "三室两厅", "190", 36000,
                "中央空调,地暖,新风系统,江景阳台,车位", "幸福市滨江园路22号",
                IMG_PREFIX + "apt14.jpg");
        count += createApartmentIfAbsent("中粮海景壹号·云端", "三室两厅", "200", 42000,
                "中央空调,地暖,全景落地窗,健身房,车位", "幸福市海景壹号大厦",
                IMG_PREFIX + "apt15.jpg");
        count += createApartmentIfAbsent("世茂滨江花园·璀璨", "两室两厅", "130", 28000,
                "中央空调,地暖,新风系统,江景阳台", "幸福市滨江花园路36号",
                IMG_PREFIX + "apt16.jpg");
        count += createApartmentIfAbsent("保利One56·天际公馆", "两室两厅", "145", 33000,
                "中央空调,地暖,智能家居,天际会所,车位", "幸福市天际大道56号",
                IMG_PREFIX + "apt17.jpg");
        count += createApartmentIfAbsent("绿地海珀外滩·尊邸", "三室两厅", "175", 39000,
                "中央空调,地暖,新风系统,外滩景观,管家服务", "幸福市海珀路9号",
                IMG_PREFIX + "apt18.jpg");

        return count;
    }

    /**
     * 按标题判重创建房源
     *
     * @param imgSeed 图片种子：以 "apt" 开头表示内置图片文件名（不含扩展名），否则视为在线占位图种子
     */
    private int createApartmentIfAbsent(String title, String layout, String area, int rent,
                                        String facilities, String address, String imgSeed) {
        if (apartmentMapper.selectCount(
                new LambdaQueryWrapper<Apartment>().eq(Apartment::getTitle, title)) > 0) {
            return 0;
        }
        Apartment apartment = new Apartment();
        apartment.setTitle(title);
        apartment.setImages(buildImages(imgSeed));
        apartment.setLayout(layout);
        apartment.setArea(new BigDecimal(area));
        apartment.setRent(rent);
        apartment.setStatus("可租");
        apartment.setFacilities(facilities);
        apartment.setAddress(address);
        apartment.setCreateTime(LocalDateTime.now());
        apartment.setUpdateTime(LocalDateTime.now());
        apartmentMapper.insert(apartment);
        return 1;
    }

    /**
     * 构造房源图片字段
     *
     * @param imgSeed 图片种子：以 "/" 开头视为内置静态资源完整路径（直接返回），否则视为在线占位图种子
     */
    private String buildImages(String imgSeed) {
        if (imgSeed.startsWith("/")) {
            return imgSeed;
        }
        return "https://picsum.photos/seed/" + imgSeed + "a/800/600,"
                + "https://picsum.photos/seed/" + imgSeed + "b/800/600";
    }

    // ============================ 历史预约 ============================

    /**
     * 播种近 5 个月的「已成交」记录（当月不产生），用于 BI 看板的营业额曲线与房源占比展示。
     * 幂等：一旦历史月份已存在记录，则不再重复播种，避免覆盖用户后续操作产生的数据。
     */
    private int seedHistoryBookings() {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        long historyCount = bookingMapper.selectCount(
                new LambdaQueryWrapper<Booking>().lt(Booking::getCreateTime, monthStart.atStartOfDay()));
        if (historyCount > 0) {
            return 0;
        }

        // monthsAgo：距当前月份的偏移（5 = 五个月前，1 = 上个月）
        // dayOffset：在该月「今天」基础上后移的天数，保证记录落在 BI「近 6 个月」统计窗口内
        // username / apartmentTitle 用于关联真实主键
        List<HistoryBooking> plan = List.of(
                // 五个月前
                new HistoryBooking(5, 0, "zhangwei", "阳光花园·温馨一居"),
                new HistoryBooking(5, 3, "user", "华府天地·都市核心"),
                new HistoryBooking(5, 7, "lina", "世茂滨江花园·璀璨"),
                // 四个月前
                new HistoryBooking(4, 0, "user", "汤臣一品·外滩豪景大平层"),
                new HistoryBooking(4, 4, "wangqiang", "翠湖天地·御苑"),
                new HistoryBooking(4, 8, "liumin", "中心广场·Loft公寓"),
                new HistoryBooking(4, 12, "chenchen", "静安里·单身公寓"),
                // 三个月前
                new HistoryBooking(3, 2, "zhangwei", "汤臣一品·滨江臻藏"),
                new HistoryBooking(3, 9, "user", "中粮海景壹号·云端"),
                // 两个月前
                new HistoryBooking(2, 1, "lina", "汤臣一品·外滩豪景大平层"),
                new HistoryBooking(2, 6, "wangqiang", "华府天地·都市核心"),
                new HistoryBooking(2, 13, "user", "绿地海珀外滩·尊邸"),
                // 上个月
                new HistoryBooking(1, 0, "liumin", "翠湖天地·御苑"),
                new HistoryBooking(1, 5, "chenchen", "世茂滨江花园·璀璨"),
                new HistoryBooking(1, 10, "zhangwei", "阳光花园·温馨一居")
        );

        int inserted = 0;
        for (HistoryBooking item : plan) {
            if (createHistoryBooking(item)) {
                inserted++;
            }
        }

        // 与业务状态保持一致：产生过成交记录的房源置为「已租」
        List<String> rentedTitles = plan.stream().map(HistoryBooking::apartmentTitle).distinct().toList();
        apartmentMapper.update(null, new LambdaUpdateWrapper<Apartment>()
                .in(Apartment::getTitle, rentedTitles)
                .set(Apartment::getStatus, "已租"));

        return inserted;
    }

    /** 按计划写入单条历史成交记录，关联失败时跳过并告警 */
    private boolean createHistoryBooking(HistoryBooking item) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, item.username()));
        Apartment apartment = apartmentMapper.selectOne(
                new LambdaQueryWrapper<Apartment>().eq(Apartment::getTitle, item.apartmentTitle()));
        if (user == null || apartment == null) {
            log.warn("跳过历史记录播种：用户 {} 或房源 {} 不存在", item.username(), item.apartmentTitle());
            return false;
        }
        Booking booking = new Booking();
        booking.setUserId(user.getId());
        booking.setApartmentId(apartment.getId());
        booking.setStatus("已成交");
        // 以「今天往前推 N 个月」为基准再后移若干天，确保记录落在 BI 近 6 个月窗口内
        LocalDate date = LocalDate.now().minusMonths(item.monthsAgo()).plusDays(item.dayOffset());
        booking.setCreateTime(date.atTime(10 + item.dayOffset() % 8, 30));
        bookingMapper.insert(booking);
        return true;
    }

    /** 历史预约播种计划项 */
    private record HistoryBooking(int monthsAgo, int dayOffset, String username, String apartmentTitle) {
    }
}
