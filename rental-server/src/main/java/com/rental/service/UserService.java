package com.rental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rental.common.BusinessException;
import com.rental.common.JwtUtil;
import com.rental.dto.LoginDTO;
import com.rental.dto.RegisterDTO;
import com.rental.dto.UserUpdateDTO;
import com.rental.entity.Booking;
import com.rental.entity.User;
import com.rental.mapper.BookingMapper;
import com.rental.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户业务：注册、登录、个人信息维护、用户列表查询
 */
@Service
@RequiredArgsConstructor
public class UserService {

    /** 内置管理员在 JWT 中的用户标识（不落 user 表，用 0 占位） */
    private static final long ADMIN_ID = 0L;

    private final UserMapper userMapper;
    private final BookingMapper bookingMapper;
    private final ApartmentService apartmentService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 内置管理员账号（配置于 application.yml，仅管理端/BI 使用） */
    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    /**
     * 注册新用户（默认角色 user）
     * 昵称统一取账号，真实信息（姓名/年龄/电话/同住人）留待用户在「编辑资料」中完善
     *
     * @param dto 注册参数（username 唯一，password BCrypt 加密存储）
     * @return 注册成功的用户信息
     */
    public User register(RegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getUsername());
        user.setAvatar("");
        user.setRole("user");
        userMapper.insert(user);
        return user;
    }

    /**
     * 登录校验：账号密码匹配后签发 JWT
     * 管理员为内置账号（配置在 application.yml），不存于 user 表，
     * 因此 C 端的用户体系里不存在管理员，管理端/BI 仍可正常登录
     *
     * @param dto 登录参数
     * @return Map 包含 token 与 user 信息
     */
    public Map<String, Object> login(LoginDTO dto) {
        if (adminUsername.equals(dto.getUsername())) {
            if (!adminPassword.equals(dto.getPassword())) {
                throw new BusinessException("账号或密码错误");
            }
            return buildLoginResult(buildBuiltinAdmin());
        }
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        return buildLoginResult(user);
    }

    /** 构造内置管理员对象（仅内存使用，不持久化到 user 表） */
    private User buildBuiltinAdmin() {
        User admin = new User();
        admin.setId(ADMIN_ID);
        admin.setUsername(adminUsername);
        admin.setNickname(adminUsername);
        admin.setRealName("管理员");
        admin.setAvatar("");
        admin.setRole("admin");
        return admin;
    }

    /** 组装登录返回：token + 用户信息 */
    private Map<String, Object> buildLoginResult(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtUtil.generateToken(user.getId(), user.getRole()));
        result.put("user", user);
        return result;
    }

    /**
     * 按ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息，不存在时抛业务异常
     */
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    /**
     * 更新个人信息（真实姓名/年龄/电话/同住人信息/头像）
     * 昵称不参与更新——它固定等于账号
     *
     * @param id  用户ID
     * @param dto 更新参数
     * @return 更新后的用户信息
     */
    public User updateInfo(Long id, UserUpdateDTO dto) {
        User user = getById(id);
        if (dto.getRealName() != null) {
            user.setRealName(dto.getRealName());
        }
        if (dto.getAge() != null) {
            user.setAge(dto.getAge());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getCohabitants() != null) {
            user.setCohabitants(dto.getCohabitants());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        userMapper.updateById(user);
        return getById(id);
    }

    /**
     * 分页查询用户列表（管理端），支持按账号/昵称/真实姓名模糊搜索
     *
     * @param page    页码（从1开始）
     * @param size    每页条数
     * @param keyword 搜索关键词，可为空
     * @return 分页结果
     */
    public IPage<User> page(long page, long size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getRealName, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);
        return userMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 统计注册用户总数（管理端统计）
     *
     * @return 用户总数
     */
    public long count() {
        return userMapper.selectCount(null);
    }

    /**
     * 删除用户（管理端）
     * 用户不再使用时整体移除：其名下租房记录一并删除，
     * 因删除而不再有有效预约的房源同步恢复为「可租」
     *
     * @param id 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        User user = getById(id);
        if ("admin".equals(user.getRole())) {
            throw new BusinessException("管理员账号不可删除");
        }
        // 先记录该用户占用的房源（有效预约），待记录删除后再判断是否需要释放
        Set<Long> occupiedApartmentIds = bookingMapper.selectList(
                        new LambdaQueryWrapper<Booking>().eq(Booking::getUserId, id)).stream()
                .filter(b -> "已预约".equals(b.getStatus()))
                .map(Booking::getApartmentId)
                .collect(Collectors.toSet());

        bookingMapper.delete(new LambdaQueryWrapper<Booking>().eq(Booking::getUserId, id));
        userMapper.deleteById(id);

        occupiedApartmentIds.forEach(apartmentService::releaseIfVacant);
    }
}
