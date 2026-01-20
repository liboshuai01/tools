package cn.liboshuai.tools.file;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用文本文件合并工具
 * <p>
 * 功能：将指定目录下的多个文本文件（如代码、Markdown、配置、日志等）合并为一个带标记的大文本文件。
 * 用途：生成用于 LLM (如 Gemini/ChatGPT) 上下文分析的聚合文件，或用于文档归档。
 * </p>
 */
@Slf4j
public class TextFileMerger {

    public static void main(String[] args) {
        TextFileMerger merger = new TextFileMerger();

        // 示例路径
        String targetDir = "src/main/resources";
        String outputFileName = "merged_resources.txt";

        // 演示：同时合并 Markdown文档, XML配置, 和普通文本
        merger.mergeFiles(targetDir, outputFileName,
                ".md",
                ".xml",
                ".txt",
                ".properties",
                ".json");
    }

    // 生成的文件分隔符模板，Gemini 对这种格式识别度很高
    private static final String HEADER_TEMPLATE = "\n\n" +
            "// =======================================================\n" +
            "// FILE PATH: %s\n" +
            "// =======================================================\n\n";

    /**
     * 合并文件
     *
     * @param directoryPathStr 需要扫描的根目录路径
     * @param outputFileStr    输出文件的路径 (例如: project_context.txt)
     * @param extensions       需要合并的文件后缀 (例如: .java, .md, .txt)
     */
    public void mergeFiles(String directoryPathStr, String outputFileStr, String... extensions) {
        Path rootDir = Paths.get(directoryPathStr);
        Path outputFile = Paths.get(outputFileStr);

        if (!Files.exists(rootDir) || !Files.isDirectory(rootDir)) {
            log.error("目标路径不存在或不是目录: {}", directoryPathStr);
            return;
        }

        log.info("开始扫描目录: {}", directoryPathStr);

        List<Path> targetFiles = new ArrayList<>();

        try {
            // 1. 扫描所有文件
            Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.toString();
                    for (String ext : extensions) {
                        if (fileName.endsWith(ext)) {
                            // 避免把输出文件自己也读进去了（如果输出文件也在源目录里）
                            if (!file.equals(outputFile)) {
                                targetFiles.add(file);
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            log.info("找到 {} 个符合条件的文本文件，准备合并...", targetFiles.size());

            // 2. 写入合并文件
            try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                // 写入文件头，明确告知这是一个通用文本合并文件
                writer.write("CONTEXT_INFO: This file contains merged text files for analysis.\n");
                writer.write("Please refer to the file paths below to understand the structure.\n");
                writer.write("Total Files: " + targetFiles.size() + "\n");

                for (Path path : targetFiles) {
                    // 计算相对路径
                    String relativePath = rootDir.relativize(path).toString();
                    // 统一路径分隔符，方便跨平台阅读
                    relativePath = relativePath.replace("\\", "/");

                    // 写入分隔头
                    writer.write(String.format(HEADER_TEMPLATE, relativePath));

                    // 写入文件内容
                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                    for (String line : lines) {
                        writer.write(line);
                        writer.newLine();
                    }

                    log.debug("已合并: {}", relativePath);
                }
            }

            log.info("合并完成！聚合文件已生成: {}", outputFile.toAbsolutePath());

        } catch (IOException e) {
            log.error("文件处理过程中发生 IO 异常", e);
        }
    }
}