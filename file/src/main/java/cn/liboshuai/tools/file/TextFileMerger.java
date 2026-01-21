package cn.liboshuai.tools.file;

import cn.liboshuai.tools.file.swing.GuiInput;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        Map<String, String> inputs = GuiInput.askForm("文本合并工具配置",
                new GuiInput.Item("source", "源文件目录", "~/tmp/text/source"),
                new GuiInput.Item("target", "目标存放目录", "~/tmp/text/target"),
                new GuiInput.Item("filename", "合并后的文件名", "code.txt")
        );

        // 从 Map 中取出结果
        String sourceDir = inputs.get("source");
        String targetDir = inputs.get("target");
        String outputFileName = inputs.get("filename");

        // 打印一下确认路径是否解析正确
        log.info("配置确认 -> 源: {}, 目标: {}, 文件: {}", sourceDir, targetDir, outputFileName);

        // 同时合并 Java文件, Markdown文档, XML配置, 和普通文本
        merger.mergeFiles(sourceDir, targetDir, outputFileName,
                ".java",
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
     * @param sourceDirStr      需要扫描的源根目录路径
     * @param targetDirStr      输出文件的目标存储目录
     * @param outputFileNameStr 输出文件的名称 (例如: project_context.txt)
     * @param extensions        需要合并的文件后缀 (例如: .java, .md, .txt)
     */
    public void mergeFiles(String sourceDirStr, String targetDirStr, String outputFileNameStr, String... extensions) {
        Path sourcePath = Paths.get(sourceDirStr);
        Path targetPath = Paths.get(targetDirStr);
        // 拼接完整输出路径：目标目录 + 文件名
        Path outputFile = targetPath.resolve(outputFileNameStr);

        // 校验源目录
        if (!Files.exists(sourcePath) || !Files.isDirectory(sourcePath)) {
            log.error("源路径不存在或不是目录: {}", sourceDirStr);
            return;
        }

        // 校验并创建目标目录
        try {
            if (!Files.exists(targetPath)) {
                Files.createDirectories(targetPath);
                log.info("目标目录不存在，已自动创建: {}", targetPath);
            }
        } catch (IOException e) {
            log.error("无法创建目标目录: {}", targetDirStr, e);
            return;
        }

        log.info("开始扫描源目录: {}", sourceDirStr);

        List<Path> targetFiles = new ArrayList<>();

        try {
            // 1. 扫描所有文件
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String fileName = file.toString();
                    for (String ext : extensions) {
                        if (fileName.endsWith(ext)) {
                            // 避免把输出文件自己也读进去了（如果输出文件被放在了源目录里）
                            if (!file.equals(outputFile)) {
                                targetFiles.add(file);
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            log.info("找到 {} 个符合条件的文本文件，准备合并...", targetFiles.size());

            if (targetFiles.isEmpty()) {
                log.warn("未找到任何符合后缀要求的文件，操作结束。");
                return;
            }

            // 2. 写入合并文件
            try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                // 写入文件头，明确告知这是一个通用文本合并文件
                writer.write("CONTEXT_INFO: This file contains merged text files for analysis.\n");
                writer.write("Please refer to the file paths below to understand the structure.\n");
                writer.write("Total Files: " + targetFiles.size() + "\n");
                writer.write("Source Directory: " + sourcePath.toAbsolutePath() + "\n");

                for (Path path : targetFiles) {
                    // 计算相对路径 (相对于源目录)
                    String relativePath = sourcePath.relativize(path).toString();
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