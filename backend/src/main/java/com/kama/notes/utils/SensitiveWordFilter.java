package com.kama.notes.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * DFA 敏感词过滤器
 */
@Slf4j
@Component
public class SensitiveWordFilter {

    @Value("${sensitive-words.path:}")
    private String sensitiveWordsPath;

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    private static class WordNode {
        private final Map<Character, WordNode> nextNodes = new HashMap<>();
        private boolean isEnd = false;
    }

    private final WordNode rootNode = new WordNode();

    @PostConstruct
    public void init() {
        Set<String> defaultWords = new HashSet<>(Arrays.asList(
                "反动", "色情", "赌博", "毒品", "诈骗", "暴力", "恐怖", "台独", "疆独", "藏独",
                "法轮功", "嫖娼", "卖淫", "杀人", "放火", "爆炸", "吸毒", "贩毒"
        ));
        addWords(defaultWords);

        if (StringUtils.hasText(sensitiveWordsPath)) {
            loadExternalWords(sensitiveWordsPath);
        }
    }

    private void loadExternalWords(String path) {
        try {
            int totalWords = 0;
            if (path.startsWith("classpath:")) {
                Resource[] resources = resourcePatternResolver.getResources(path + "/*.txt");
                for (Resource resource : resources) {
                    int count = loadWordsFromStream(resource.getInputStream(), resource.getFilename());
                    totalWords += count;
                    log.info("加载敏感词资源: {}, 数量: {}", resource.getFilename(), count);
                }
            } else {
                Path dir = Paths.get(path);
                if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                    log.warn("敏感词库路径不存在或不是目录: {}", path);
                    return;
                }
                try (Stream<Path> paths = Files.list(dir)) {
                    List<Path> txtFiles = paths.filter(p -> p.toString().endsWith(".txt")).toList();
                    for (Path file : txtFiles) {
                        int count = loadWordsFromStream(Files.newInputStream(file), file.getFileName().toString());
                        totalWords += count;
                        log.info("加载敏感词文件: {}, 数量: {}", file.getFileName(), count);
                    }
                }
            }
            log.info("外部敏感词库加载完成，总计: {} 个词", totalWords);
        } catch (Exception e) {
            log.error("加载外部敏感词库失败: {}", path, e);
        }
    }

    private int loadWordsFromStream(InputStream is, String sourceName) throws IOException {
        byte[] bytes = StreamUtils.copyToByteArray(is);
        List<String> lines = decodeLines(bytes);
        int count = 0;
        for (String line : lines) {
            String word = line.trim();
            if (!word.isEmpty() && !word.startsWith("#")) {
                addWord(word);
                count++;
            }
        }
        return count;
    }

    private List<String> decodeLines(byte[] bytes) {
        // 尝试 UTF-8，如果出现大量替换字符则回退 GBK
        String utf8Content = new String(bytes, StandardCharsets.UTF_8);
        if (!utf8Content.contains("\uFFFD")) {
            return Arrays.asList(utf8Content.split("\r?\n"));
        }
        String gbkContent = new String(bytes, Charset.forName("GBK"));
        return Arrays.asList(gbkContent.split("\r?\n"));
    }

    public void addWords(Set<String> words) {
        if (words == null || words.isEmpty()) {
            return;
        }
        for (String word : words) {
            addWord(word);
        }
    }

    public void addWord(String word) {
        if (word == null || word.isEmpty()) {
            return;
        }
        WordNode node = rootNode;
        for (char c : word.toCharArray()) {
            node = node.nextNodes.computeIfAbsent(c, k -> new WordNode());
        }
        node.isEnd = true;
    }

    public String filter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder(text);
        int i = 0;
        while (i < text.length()) {
            int matchLen = checkSensitiveWord(text, i);
            if (matchLen > 0) {
                for (int j = 0; j < matchLen; j++) {
                    result.setCharAt(i + j, '*');
                }
                i += matchLen;
            } else {
                i++;
            }
        }
        return result.toString();
    }

    private int checkSensitiveWord(String text, int beginIndex) {
        WordNode node = rootNode;
        int matchedLen = 0;
        int maxLen = 0;
        for (int i = beginIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            node = node.nextNodes.get(c);
            if (node == null) {
                break;
            }
            matchedLen++;
            if (node.isEnd) {
                maxLen = matchedLen;
            }
        }
        return maxLen;
    }
}
