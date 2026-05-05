package cn.liboshuai.tools.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class WordFileAnalyzer {

    /**
     * 分析文件中的单词信息
     *
     * @param filePath 文件路径
     * @return 分析结果
     * @throws IOException IO异常
     */
    public static AnalysisResult analyze(Path filePath) throws IOException {
        List<String> words = readWords(filePath);

        int totalWords = words.size();

        // 统计每个单词出现次数，使用 LinkedHashMap 保留原顺序
        Map<String, Integer> wordCountMap = new LinkedHashMap<>();
        for (String word : words) {
            wordCountMap.merge(word, 1, Integer::sum);
        }

        int uniqueWords = wordCountMap.size();
        boolean hasDuplicate = totalWords > uniqueWords;

        Map<String, Integer> duplicateDetails = wordCountMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        int duplicateWordTypes = duplicateDetails.size();

        int duplicateOccurrences = duplicateDetails.values()
                .stream()
                .mapToInt(count -> count - 1)
                .sum();

        return new AnalysisResult(
                totalWords,
                uniqueWords,
                hasDuplicate,
                duplicateWordTypes,
                duplicateOccurrences,
                wordCountMap,
                duplicateDetails
        );
    }

    /**
     * 生成去重后的新文件
     *
     * @param sourceFile 原文件
     * @param targetFile 去重后文件
     * @throws IOException IO异常
     */
    public static void writeUniqueWordsToFile(Path sourceFile, Path targetFile) throws IOException {
        List<String> words = readWords(sourceFile);

        // LinkedHashSet 保留插入顺序并去重
        LinkedHashSet<String> uniqueSet = new LinkedHashSet<>(words);

        StringJoiner joiner = new StringJoiner(", ");
        for (String word : uniqueSet) {
            joiner.add(word);
        }

        Files.writeString(targetFile, joiner.toString(), StandardCharsets.UTF_8);
    }

    /**
     * 读取文件中的单词列表
     * 支持逗号、空格、换行、制表符分隔
     *
     * @param filePath 文件路径
     * @return 单词列表
     * @throws IOException IO异常
     */
    private static List<String> readWords(Path filePath) throws IOException {
        String content = Files.readString(filePath, StandardCharsets.UTF_8);

        if (content == null || content.isBlank()) {
            return new ArrayList<>();
        }

        // 按逗号、空白字符分割
        String[] arr = content.split("[,\\s]+");

        List<String> words = new ArrayList<>();
        for (String word : arr) {
            if (word != null) {
                String trimmed = word.trim();
                if (!trimmed.isEmpty()) {
                    words.add(trimmed);
                }
            }
        }
        return words;
    }

    /**
     * 分析结果对象
     */
    public static class AnalysisResult {
        private final int totalWords;
        private final int uniqueWords;
        private final boolean hasDuplicate;
        private final int duplicateWordTypes;
        private final int duplicateOccurrences;
        private final Map<String, Integer> wordCountMap;
        private final Map<String, Integer> duplicateDetails;

        public AnalysisResult(int totalWords,
                              int uniqueWords,
                              boolean hasDuplicate,
                              int duplicateWordTypes,
                              int duplicateOccurrences,
                              Map<String, Integer> wordCountMap,
                              Map<String, Integer> duplicateDetails) {
            this.totalWords = totalWords;
            this.uniqueWords = uniqueWords;
            this.hasDuplicate = hasDuplicate;
            this.duplicateWordTypes = duplicateWordTypes;
            this.duplicateOccurrences = duplicateOccurrences;
            this.wordCountMap = wordCountMap;
            this.duplicateDetails = duplicateDetails;
        }

        public int getTotalWords() {
            return totalWords;
        }

        public int getUniqueWords() {
            return uniqueWords;
        }

        public boolean isHasDuplicate() {
            return hasDuplicate;
        }

        public int getDuplicateWordTypes() {
            return duplicateWordTypes;
        }

        public int getDuplicateOccurrences() {
            return duplicateOccurrences;
        }

        public Map<String, Integer> getWordCountMap() {
            return wordCountMap;
        }

        public Map<String, Integer> getDuplicateDetails() {
            return duplicateDetails;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("总单词数（含重复）: ").append(totalWords).append(System.lineSeparator());
            sb.append("去重后单词数: ").append(uniqueWords).append(System.lineSeparator());
            sb.append("是否存在重复单词: ").append(hasDuplicate ? "是" : "否").append(System.lineSeparator());
            sb.append("重复单词种类数: ").append(duplicateWordTypes).append(System.lineSeparator());
            sb.append("重复出现的总次数: ").append(duplicateOccurrences).append(System.lineSeparator());

            if (duplicateDetails.isEmpty()) {
                sb.append("重复单词详情: 无").append(System.lineSeparator());
            } else {
                sb.append("重复单词详情:").append(System.lineSeparator());
                duplicateDetails.forEach((word, count) ->
                        sb.append("  ").append(word).append(" -> 出现 ").append(count).append(" 次")
                                .append(System.lineSeparator())
                );
            }
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        try {
            Path sourceFile = Path.of("C:/Users/lbs/Desktop/source.txt");
            Path uniqueFile = Path.of("C:/Users/lbs/Desktop/target.txt");
            WordFileAnalyzer.AnalysisResult result = WordFileAnalyzer.analyze(sourceFile);
            System.out.println("========== 文件分析结果 ==========");
            System.out.println(result);
            WordFileAnalyzer.writeUniqueWordsToFile(sourceFile, uniqueFile);
            System.out.println("去重后的文件已生成: " + uniqueFile.toAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}