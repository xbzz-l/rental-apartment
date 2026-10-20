package com.rental.common;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录鉴权拦截器：
 * - /api/admin/**                需要 admin 角色
 * - /api/user/**、/api/bookings/** 需要登录
 * - 其余接口公开访问；若携带合法 token，则解析出 userId / role 放入请求属性
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行 CORS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        boolean adminPath = uri.startsWith("/api/admin/");
        boolean authPath = uri.startsWith("/api/user/") || uri.startsWith("/api/bookings/");

        Claims claims = parseClaims(request, adminPath || authPath);

        if (adminPath) {
            if (claims == null) {
                throw new BusinessException(401, "请先登录");
            }
            if (!"admin".equals(claims.get("role"))) {
                throw new BusinessException(403, "无权限访问");
            }
        } else if (authPath) {
            if (claims == null) {
                throw new BusinessException(401, "请先登录");
            }
        }

        // 已登录用户：注入 userId 与 role，供控制器通过 @RequestAttribute 获取
        if (claims != null) {
            request.setAttribute("userId", Long.valueOf(claims.getSubject()));
            request.setAttribute("role", claims.get("role"));
        }
        return true;
    }

    /**
     * 从 Authorization 请求头解析 Bearer 令牌
     *
     * @param request    当前请求
     * @param authNeeded 是否为需登录接口：需登录接口令牌非法/过期直接抛业务异常；
     *                   公开接口令牌非法时按未登录处理，避免过期 token 影响匿名访问
     * @return 令牌载荷；无令牌或非法令牌返回 null
     */
    private Claims parseClaims(HttpServletRequest request, boolean authNeeded) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        try {
            return jwtUtil.parse(header.substring(7));
        } catch (Exception e) {
            if (authNeeded) {
                throw new BusinessException(401, "登录已过期，请重新登录");
            }
            return null;
        }
    }
}
