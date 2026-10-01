package com.company.ai.marketplace.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.ai.marketplace.common.BizException;
import com.company.ai.marketplace.common.ErrorCode;
import com.company.ai.marketplace.config.AppProperties;
import com.company.ai.marketplace.dto.LoginResultVO;
import com.company.ai.marketplace.entity.SysUser;
import com.company.ai.marketplace.mapper.SysUserMapper;
import com.company.ai.marketplace.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务测试")
class AuthServiceTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private JwtService jwtService;
    @Mock
    private AppProperties props;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private SysUser mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new SysUser();
        mockUser.setId(1L);
        mockUser.setAccount("admin");
        mockUser.setPasswordHash(BCrypt.hashpw("Welcome@2026"));
        mockUser.setUsername("Administrator");
        mockUser.setRoles("USER,ADMIN");
        mockUser.setEnabled(1);
        mockUser.setMustChangePassword(1);
    }

    @Test
    @DisplayName("账号密码登录成功")
    void loginByPassword_success() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);
        when(jwtService.sign(any())).thenReturn("mock-jwt-token");

        LoginResultVO result = authService.loginByPassword("admin", "Welcome@2026");

        assertNotNull(result);
        assertNotNull(result.getToken());
        assertEquals("mock-jwt-token", result.getToken());
        assertEquals("Administrator", result.getUser().getUsername());
        verify(userMapper).selectOne(any());
        verify(jwtService).sign(any());
    }

    @Test
    @DisplayName("账号不存在时抛 AUTH_BAD_CREDENTIALS")
    void loginByPassword_userNotFound() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> authService.loginByPassword("nobody", "pass"));

        assertEquals(ErrorCode.AUTH_BAD_CREDENTIALS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("密码不匹配时抛 AUTH_BAD_CREDENTIALS")
    void loginByPassword_wrongPassword() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);

        BizException ex = assertThrows(BizException.class,
                () -> authService.loginByPassword("admin", "wrong-password"));

        assertEquals(ErrorCode.AUTH_BAD_CREDENTIALS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("账号已停用时抛 AUTH_USER_DISABLED")
    void loginByPassword_userDisabled() {
        mockUser.setEnabled(0);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);

        BizException ex = assertThrows(BizException.class,
                () -> authService.loginByPassword("admin", "Welcome@2026"));

        assertEquals(ErrorCode.AUTH_USER_DISABLED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("密码哈希为空时抛 AUTH_BAD_CREDENTIALS")
    void loginByPassword_emptyHash() {
        mockUser.setPasswordHash("");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);

        BizException ex = assertThrows(BizException.class,
                () -> authService.loginByPassword("admin", "Welcome@2026"));

        assertEquals(ErrorCode.AUTH_BAD_CREDENTIALS.getCode(), ex.getCode());
    }
}
