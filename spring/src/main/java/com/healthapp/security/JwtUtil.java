package com.healthapp.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthapp.common.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    // 当前使用最小实现的 HS256 JWT，只承载用户编号和过期时间，不引入额外会话状态。
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expire-hours:168}")
    private long expireHours;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Base64.Encoder encoder;
    private Base64.Decoder decoder;

    @PostConstruct
    public void init() {
        // Base64 编解码器在启动时预初始化，避免每次签发和校验 token 都重复创建对象。
        encoder = Base64.getUrlEncoder().withoutPadding();
        decoder = Base64.getUrlDecoder();
    }

    public String generateToken(Long userId) {
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", String.valueOf(userId));
            // 过期时间和签发时间都放进 payload，后续可直接做无状态校验。
            payload.put("exp", Instant.now().plusSeconds(expireHours * 3600).getEpochSecond());
            payload.put("iat", Instant.now().getEpochSecond());
            String headerPart = encodeJson(header);
            String payloadPart = encodeJson(payload);
            String signature = sign(headerPart + "." + payloadPart);
            return headerPart + "." + payloadPart + "." + signature;
        } catch (Exception e) {
            throw new BusinessException(500, "生成 token 失败");
        }
    }

    public Long parseUserId(String token) {
        try {
            if (token == null || token.isBlank()) {
                throw new BusinessException(401, "未登录或登录已过期");
            }
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException(401, "token 格式错误");
            }
            // 先校验签名再解析 payload，避免无效 token 进入后续字段处理逻辑。
            String expected = sign(parts[0] + "." + parts[1]);
            if (!constantTimeEquals(expected, parts[2])) {
                throw new BusinessException(401, "token 无效");
            }
            String payloadJson = new String(decoder.decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<>() {});
            long exp = Long.parseLong(String.valueOf(payload.get("exp")));
            if (Instant.now().getEpochSecond() > exp) {
                throw new BusinessException(401, "登录已过期");
            }
            return Long.parseLong(String.valueOf(payload.get("sub")));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(401, "token 解析失败");
        }
    }

    private String encodeJson(Map<String, Object> data) throws Exception {
        return encoder.encodeToString(objectMapper.writeValueAsBytes(data));
    }

    private String sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return encoder.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        // 使用常量时间比较降低签名校验被时序侧信道利用的风险。
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
