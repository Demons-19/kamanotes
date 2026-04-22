package com.kama.notes.client;

import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.TextModerationPlusRequest;
import com.aliyun.green20220302.models.TextModerationPlusResponse;
import com.aliyun.green20220302.models.TextModerationPlusResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.kama.notes.config.AliyunContentSecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云内容安全客户端（文本审核）
 */
@Slf4j
@Component
public class AliyunContentSecurityClient {

    private static final String TEXT_MODERATION_SERVICE = "comment_detection_pro";
    private static final int MAX_SEGMENT_LENGTH = 500;
    private static final int SEGMENT_OVERLAP_LENGTH = 80;

    @Autowired
    private AliyunContentSecurityProperties properties;

    private Client client;

    @PostConstruct
    public void init() {
        if (!hasConfig()) {
            log.warn("阿里云内容安全配置未完整，跳过初始化");
            return;
        }

        Config config = new Config();
        config.setAccessKeyId(properties.getAccessKeyId());
        config.setAccessKeySecret(properties.getAccessKeySecret());
        config.setRegionId(properties.getRegionId());
        config.setEndpoint(properties.getEndpoint());
        config.setReadTimeout(6000);
        config.setConnectTimeout(3000);

        try {
            client = new Client(config);
            log.info("阿里云内容安全客户端初始化完成，region={}, endpoint={}", properties.getRegionId(), properties.getEndpoint());
        } catch (Exception e) {
            log.error("阿里云内容安全客户端初始化失败", e);
        }
    }

    /**
     * 检测文本内容
     *
     * @param content 待检测文本
     * @return true = 通过，false = 违规
     */
    public boolean textPass(String content) {
        if (!hasConfig() || client == null) {
            log.warn("阿里云内容安全未配置，默认放行");
            return true;
        }
        if (content == null || content.isEmpty()) {
            return true;
        }

        try {
            List<String> segments = splitContent(content);
            log.info("阿里云文本审核分段数: {}, 原始长度: {}", segments.size(), content.length());

            for (int i = 0; i < segments.size(); i++) {
                String segment = segments.get(i);
                boolean passed = reviewSingleSegment(segment, i + 1, segments.size());
                if (!passed) {
                    log.warn("阿里云文本审核未通过，segment={}/{}", i + 1, segments.size());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("阿里云文本审核调用失败", e);
            return true;
        }
    }

    private boolean reviewSingleSegment(String content, int index, int total) throws Exception {
        Map<String, Object> serviceParameters = new HashMap<>();
        serviceParameters.put("content", content);

        TextModerationPlusRequest request = new TextModerationPlusRequest();
        request.setService(TEXT_MODERATION_SERVICE);
        request.setServiceParameters(toJson(serviceParameters));

        TextModerationPlusResponse response = client.textModerationPlus(request);
        TextModerationPlusResponseBody body = response.getBody();
        log.info("阿里云文本审核响应(segment={}/{}): {}", index, total, toLogJson(body));

        if (response.getStatusCode() == null || response.getStatusCode() != 200) {
            log.warn("阿里云文本审核 HTTP 状态异常(segment={}/{}): {}", index, total, response.getStatusCode());
            return true;
        }
        if (body == null) {
            return true;
        }
        if (body.getCode() == null || body.getCode() != 200) {
            log.warn("阿里云文本审核业务状态异常(segment={}/{}): code={}, message={}",
                    index, total, body.getCode(), body.getMessage());
            return true;
        }

        TextModerationPlusResponseBody.TextModerationPlusResponseBodyData data = body.getData();
        if (data == null || data.getRiskLevel() == null) {
            return true;
        }

        log.info("阿里云文本审核结果(segment={}/{}), RiskLevel={}", index, total, data.getRiskLevel());
        return "none".equalsIgnoreCase(data.getRiskLevel());
    }

    private List<String> splitContent(String content) {
        List<String> segments = new ArrayList<>();
        if (content.length() <= MAX_SEGMENT_LENGTH) {
            segments.add(content);
            return segments;
        }

        int start = 0;
        int step = MAX_SEGMENT_LENGTH - SEGMENT_OVERLAP_LENGTH;
        while (start < content.length()) {
            int end = Math.min(start + MAX_SEGMENT_LENGTH, content.length());
            segments.add(content.substring(start, end));
            if (end >= content.length()) {
                break;
            }
            start += step;
        }
        return segments;
    }

    private boolean hasConfig() {
        return properties.getAccessKeyId() != null && !properties.getAccessKeyId().isEmpty()
                && properties.getAccessKeySecret() != null && !properties.getAccessKeySecret().isEmpty();
    }

    private String toJson(Map<String, Object> values) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            builder.append('"').append(entry.getKey()).append('"').append(':');
            Object value = entry.getValue();
            if (value == null) {
                builder.append("null");
            } else {
                builder.append('"').append(escapeJson(String.valueOf(value))).append('"');
            }
            first = false;
        }
        builder.append('}');
        return builder.toString();
    }

    private String toLogJson(TextModerationPlusResponseBody body) {
        if (body == null) {
            return "null";
        }
        String riskLevel = body.getData() == null ? null : body.getData().getRiskLevel();
        return String.format("{\"RequestId\":\"%s\",\"Message\":\"%s\",\"Code\":%s,\"RiskLevel\":\"%s\"}",
                body.getRequestId(), body.getMessage(), body.getCode(), riskLevel);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
