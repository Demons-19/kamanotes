package com.kama.notes.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.kama.notes.mapper.UserMapper;
import com.kama.notes.model.entity.User;
import com.kama.notes.model.enums.redisKey.RedisKey;
import com.kama.notes.model.enums.user.UserBanned;
import com.kama.notes.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.kama.notes.scope.RequestScopeData;
import com.kama.notes.utils.JwtUtil;

@Component
public class TokenInterceptor implements HandlerInterceptor
{
    private static final long USER_SESSION_PROFILE_TTL_SECONDS = 600;

    @Autowired
    private RequestScopeData requestScopeData;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisService redisService;

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        initRequestScope();

        String token = request.getHeader("Authorization");

        if (token == null || token.trim().isEmpty()) {
            return true;
        }

        token = token.replace("Bearer ", "").trim();
        if (token.isEmpty()) {
            return true;
        }

        if (redisService.exists(RedisKey.jwtBlacklist(token))) {
            return true;
        }

        if (!jwtUtil.validateToken(token)) {
            return true;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return true;
        }

        UserSessionProfile profile = getUserSessionProfile(userId);
        if (profile == null) {
            return true;
        }

        requestScopeData.setUserId(userId);
        requestScopeData.setToken(token);
        requestScopeData.setLogin(true);
        requestScopeData.setBanned(profile.banned());

        if (!requestScopeData.isBanned() && jwtUtil.shouldRefreshToken(token)) {
            String refreshedToken = jwtUtil.refreshToken(userId);
            requestScopeData.setRefreshedToken(refreshedToken);
            response.setHeader("X-Refresh-Token", refreshedToken);
            response.setHeader("Access-Control-Expose-Headers", "X-Refresh-Token");
        }

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    private UserSessionProfile getUserSessionProfile(Long userId) {
        String cacheKey = RedisKey.userSessionProfile(userId);
        Object cached = redisService.get(cacheKey);
        if (cached instanceof UserSessionProfile profile) {
            return profile;
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            return null;
        }

        UserSessionProfile profile = new UserSessionProfile(UserBanned.IS_BANNED.equals(user.getIsBanned()));
        redisService.setWithExpiry(cacheKey, profile, USER_SESSION_PROFILE_TTL_SECONDS);
        return profile;
    }

    private void initRequestScope() {
        requestScopeData.setLogin(false);
        requestScopeData.setToken(null);
        requestScopeData.setUserId(null);
        requestScopeData.setBanned(false);
        requestScopeData.setRefreshedToken(null);
    }

    private record UserSessionProfile(boolean banned) {}
}
