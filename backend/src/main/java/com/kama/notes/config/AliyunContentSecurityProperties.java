package com.kama.notes.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云内容安全配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.content-security")
public class AliyunContentSecurityProperties {
    private String accessKeyId;
    private String accessKeySecret;
    private String regionId = "cn-shanghai";
    private String endpoint = "green-cip.cn-shanghai.aliyuncs.com";
}
