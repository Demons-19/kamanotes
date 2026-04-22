package com.kama.notes.task.note;

import com.kama.notes.config.NoteReviewProperties;
import com.kama.notes.service.NoteReviewProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class NoteReviewStreamConsumer implements SmartLifecycle {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private NoteReviewProperties properties;

    @Autowired
    private NoteReviewProcessor processor;

    @Autowired
    @Qualifier("noteReviewExecutor")
    private Executor noteReviewExecutor;

    private volatile boolean running;
    private Thread workerThread;
    private String consumerName;

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        consumerName = buildConsumerName();
        ensureGroup();
        workerThread = new Thread(this::consumeLoop, "note-review-stream-main");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    @Override
    public void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    private void consumeLoop() {
        while (running) {
            try {
                List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                        Consumer.from(properties.getGroup(), consumerName),
                        StreamReadOptions.empty()
                                .block(Duration.ofMillis(properties.getReadBlockMillis()))
                                .count(properties.getBatchSize()),
                        StreamOffset.create(properties.getStreamKey(), ReadOffset.lastConsumed())
                );

                if (records == null || records.isEmpty()) {
                    continue;
                }

                for (MapRecord<String, Object, Object> record : records) {
                    noteReviewExecutor.execute(() -> processor.process(record));
                }
            } catch (QueryTimeoutException e) {
                if (running) {
                    log.debug("笔记审核 Stream 阻塞读取超时，继续等待新消息");
                }
            } catch (Exception e) {
                if (running) {
                    log.error("消费笔记审核 Stream 失败", e);
                    sleepQuietly(1000L);
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${note.review.claim-interval-millis:15000}")
    public void replayHistoryMessages() {
        if (!running) {
            return;
        }
        try {
            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                    Consumer.from(properties.getGroup(), consumerName),
                    StreamReadOptions.empty().count(properties.getReclaimBatchSize()),
                    StreamOffset.create(properties.getStreamKey(), ReadOffset.from("0-0"))
            );
            if (records == null || records.isEmpty()) {
                return;
            }
            for (MapRecord<String, Object, Object> record : records) {
                noteReviewExecutor.execute(() -> processor.process(record));
            }
        } catch (Exception e) {
            log.warn("补偿读取历史审核消息失败", e);
        }
    }

    private void ensureGroup() {
        try {
            stringRedisTemplate.opsForStream().createGroup(properties.getStreamKey(), ReadOffset.latest(), properties.getGroup());
        } catch (RedisSystemException | InvalidDataAccessApiUsageException e) {
            log.info("笔记审核消费组已存在: {}", properties.getGroup());
        } catch (Exception e) {
            stringRedisTemplate.opsForStream().add(properties.getStreamKey(), Collections.singletonMap("init", "0"));
            try {
                stringRedisTemplate.opsForStream().createGroup(properties.getStreamKey(), ReadOffset.latest(), properties.getGroup());
            } catch (Exception ignore) {
                log.info("笔记审核消费组初始化已完成: {}", properties.getGroup());
            }
        }
    }

    private String buildConsumerName() {
        try {
            return properties.getConsumerPrefix() + "-" + InetAddress.getLocalHost().getHostName() + "-" + System.currentTimeMillis();
        } catch (UnknownHostException e) {
            return properties.getConsumerPrefix() + "-local";
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
