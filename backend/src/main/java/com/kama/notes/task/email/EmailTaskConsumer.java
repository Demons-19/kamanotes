package com.kama.notes.task.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kama.notes.model.enums.redisKey.RedisKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class EmailTaskConsumer {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${spring.mail.username}")
    private String from;

    // 每 3 秒轮询一次 redis，查看是否有待发的邮件任务
    @Scheduled(fixedDelay = 3000)
    public void resume() throws JsonProcessingException {
        String emailQueueKey = RedisKey.emailTaskQueue();

        // 从队列中取任务对象
        while (true) {

            // 获取任务对象
            String emailTaskJson = redisTemplate.opsForList().rightPop(emailQueueKey);

            if (emailTaskJson == null) {  // 队列中没有任务对象，退出本次执行
                break;
            }

            // 将 redis 中的 JSON 字符串转成 emailTask 对象
            EmailTask emailTask = objectMapper.readValue(emailTaskJson, EmailTask.class);
            String email = emailTask.getEmail();
            String verificationCode = emailTask.getCode();

            // 根据 emailTask 对象中的信息
            // 填充 SimpleMailMessage 对象，然后使用 JavaMailSender 发送邮件
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(from);
            mailMessage.setTo(email);
            mailMessage.setSubject("卡码笔记- 验证码");
            mailMessage.setText("您的验证码是：" + verificationCode + "，有效期" + 5 + "分钟，请勿泄露给他人。");

            mailSender.send(mailMessage);

            // 保存验证码到 Redis
            // 有效时间为 5 分钟
            redisTemplate.opsForValue().set(RedisKey.registerVerificationCode(email), verificationCode, 5, TimeUnit.MINUTES);
        }
    }
}
