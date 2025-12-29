package com.yourcompany.supermarket.security;

import com.yourcompany.supermarket.entity.Employee;
import com.yourcompany.supermarket.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    private EmployeeService employeeService;

    public AuthSession login(String username, String password) {
        Employee employee = employeeService.findByName(username);
        if (employee == null || Boolean.FALSE.equals(employee.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在或已禁用");
        }

        String passwordHash = hashPassword(password);
        if (!Objects.equals(passwordHash, employee.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        String token = UUID.randomUUID().toString();
        AuthSession session = new AuthSession(token, employee.getId(), employee.getRole());
        sessions.put(token, session);
        return session;
    }

    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public AuthSession resolveSession(String token) {
        return token == null ? null : sessions.get(token);
    }

    public boolean hasRole(AuthSession session, String[] requiredRoles) {
        if (session == null || requiredRoles == null || requiredRoles.length == 0) {
            return false;
        }
        for (String role : requiredRoles) {
            if (session.getRole().equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }

    public String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encoded);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("无法计算密码哈希", e);
        }
    }

    public static class AuthSession {
        private final String token;
        private final Long employeeId;
        private final String role;

        public AuthSession(String token, Long employeeId, String role) {
            this.token = token;
            this.employeeId = employeeId;
            this.role = role;
        }

        public String getToken() {
            return token;
        }

        public Long getEmployeeId() {
            return employeeId;
        }

        public String getRole() {
            return role;
        }
    }
}
