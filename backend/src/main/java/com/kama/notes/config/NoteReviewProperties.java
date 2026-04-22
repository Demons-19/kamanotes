package com.kama.notes.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "note.review")
public class NoteReviewProperties {
    private String streamKey = "stream:note:review";
    private String group = "group:note:review";
    private String consumerPrefix = "note-review-consumer";
    private String retryZSetKey = "zset:note:review:retry";
    private long readBlockMillis = 5000;
    private int batchSize = 10;
    private int maxRetryCount = 5;
    private long baseRetryDelaySeconds = 30;
    private long maxRetryDelaySeconds = 600;
    private long pendingIdleMillis = 60000;
    private long reclaimBatchSize = 20;
    private long claimIntervalMillis = 15000;
    private long mockResultDelayMillis = 0;
}
