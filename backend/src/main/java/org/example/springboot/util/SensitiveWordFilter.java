package org.example.springboot.util;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 敏感词过滤工具类
 * 使用DFA(Deterministic Finite Automaton)算法实现高效的敏感词检测和替换
 */
@Component
public class SensitiveWordFilter {

    /**
     * 敏感词库
     */
    private static final Set<String> SENSITIVE_WORDS = new HashSet<>(Arrays.asList(
            // 政治敏感词
            "反动", "暴力", "恐怖", "色情", "赌博", "毒品",
            // 诈骗相关
            "诈骗", "骗子", "传销", "非法集资",
            // 不文明用语
            "傻逼", "fuck", "shit", "婊", "垃圾", "废物", "白痴", "智障",
            // 广告垃圾信息
            "办证", "代开发票", "贷款", "信用卡套现", "私人侦探", "枪支弹药",
            // 其他不良信息
            "自杀", "杀人", "血腥", "暴恐"
    ));

    /**
     * 替换字符
     */
    private static final char REPLACE_CHAR = '*';

    /**
     * 敏感词树（DFA算法）
     */
    private Map<String, Object> sensitiveWordTree;

    /**
     * 构造函数，初始化敏感词树
     */
    public SensitiveWordFilter() {
        initSensitiveWordTree();
    }

    /**
     * 初始化敏感词树
     */
    @SuppressWarnings("unchecked")
    private void initSensitiveWordTree() {
        sensitiveWordTree = new HashMap<>();

        for (String word : SENSITIVE_WORDS) {
            if (word == null || word.trim().isEmpty()) {
                continue;
            }

            Map<String, Object> currentMap = sensitiveWordTree;
            char[] chars = word.toCharArray();

            for (int i = 0; i < chars.length; i++) {
                String key = String.valueOf(chars[i]);

                // 获取当前字符对应的子节点
                Object wordMap = currentMap.get(key);

                if (wordMap != null) {
                    currentMap = (Map<String, Object>) wordMap;
                } else {
                    // 不存在则创建
                    Map<String, Object> newWordMap = new HashMap<>();
                    currentMap.put(key, newWordMap);
                    currentMap = newWordMap;
                }

                // 最后一个字符，标记为结束
                if (i == chars.length - 1) {
                    currentMap.put("isEnd", true);
                }
            }
        }
    }

    /**
     * 检查文本中是否包含敏感词
     *
     * @param text 待检查的文本
     * @return true-包含敏感词，false-不包含
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            int length = checkSensitiveWord(chars, i);
            if (length > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取文本中的所有敏感词
     *
     * @param text 待检查的文本
     * @return 敏感词列表
     */
    public Set<String> getSensitiveWords(String text) {
        Set<String> sensitiveWords = new HashSet<>();

        if (text == null || text.trim().isEmpty()) {
            return sensitiveWords;
        }

        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            int length = checkSensitiveWord(chars, i);
            if (length > 0) {
                sensitiveWords.add(new String(chars, i, length));
                i += length - 1; // 跳过已检测的敏感词
            }
        }

        return sensitiveWords;
    }

    /**
     * 替换文本中的敏感词
     *
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public String filterSensitiveWord(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        char[] chars = text.toCharArray();
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < chars.length; i++) {
            int length = checkSensitiveWord(chars, i);

            if (length > 0) {
                // 发现敏感词，用替换字符替换
                for (int j = 0; j < length; j++) {
                    result.append(REPLACE_CHAR);
                }
                i += length - 1; // 跳过已替换的字符
            } else {
                result.append(chars[i]);
            }
        }

        return result.toString();
    }

    /**
     * 检查是否存在敏感词，并返回敏感词长度
     *
     * @param chars 字符数组
     * @param index 开始检查的位置
     * @return 敏感词长度，0表示不存在
     */
    @SuppressWarnings("unchecked")
    private int checkSensitiveWord(char[] chars, int index) {
        Map<String, Object> currentMap = sensitiveWordTree;
        int matchLength = 0;

        for (int i = index; i < chars.length; i++) {
            String key = String.valueOf(chars[i]);

            // 获取当前字符对应的子节点
            currentMap = (Map<String, Object>) currentMap.get(key);

            if (currentMap == null) {
                // 没有匹配到
                break;
            } else {
                matchLength++;

                // 检查是否已经到达敏感词的结尾
                if (Boolean.TRUE.equals(currentMap.get("isEnd"))) {
                    return matchLength;
                }
            }
        }

        return 0;
    }

    /**
     * 添加敏感词到词库
     *
     * @param word 敏感词
     */
    @SuppressWarnings("unchecked")
    public void addSensitiveWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }

        SENSITIVE_WORDS.add(word);

        Map<String, Object> currentMap = sensitiveWordTree;
        char[] chars = word.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            String key = String.valueOf(chars[i]);

            Object wordMap = currentMap.get(key);

            if (wordMap != null) {
                currentMap = (Map<String, Object>) wordMap;
            } else {
                Map<String, Object> newWordMap = new HashMap<>();
                currentMap.put(key, newWordMap);
                currentMap = newWordMap;
            }

            if (i == chars.length - 1) {
                currentMap.put("isEnd", true);
            }
        }
    }
}
