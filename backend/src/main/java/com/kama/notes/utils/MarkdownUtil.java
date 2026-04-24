package com.kama.notes.utils;

import com.vladsch.flexmark.parser.Parser;

public class MarkdownUtil {

    private static final Parser PARSER = Parser.builder().build();

    public static boolean needCollapsed(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return false;
        }
        // 纯文本超过阈值直接判定需要折叠，无需解析 AST
        if (markdown.length() > 250) {
            return true;
        }
        // 短文本才需要解析 AST 检查是否包含图片
        return new MarkdownAST(markdown, PARSER).hasImages();
    }

    public static String extractIntroduction(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        return new MarkdownAST(markdown, PARSER).extractIntroduction(250);
    }
}
