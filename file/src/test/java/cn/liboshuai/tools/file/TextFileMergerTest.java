package cn.liboshuai.tools.file;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class TextFileMergerTest {

    @Test
    void testMergeFiles(@TempDir Path tempDir) throws IOException {
        // 1. 准备测试环境
        Path rootDir = tempDir.resolve("docs");
        Files.createDirectories(rootDir);

        // 创建一个 Markdown 文件
        Path file1 = rootDir.resolve("Notes.md");
        Files.writeString(file1, "# My Notes\nRemember this.");

        // 创建一个 Properties 文件
        Path file2 = rootDir.resolve("config.properties");
        Files.writeString(file2, "key=value");

        // 创建一个不应该被包含的图片文件模拟
        Path file3 = rootDir.resolve("image.png");
        Files.writeString(file3, "BINARY_DATA");

        Path output = tempDir.resolve("output.txt");

        // 2. 执行合并
        TextFileMerger merger = new TextFileMerger();
        merger.mergeFiles(rootDir.toString(), output.toString(), ".md", ".properties");

        // 3. 验证结果
        Assertions.assertTrue(Files.exists(output), "输出文件应该存在");

        List<String> lines = Files.readAllLines(output);
        String content = String.join("\n", lines);

        // 验证包含指定扩展名的内容
        Assertions.assertTrue(content.contains("FILE PATH: Notes.md"));
        Assertions.assertTrue(content.contains("# My Notes"));

        Assertions.assertTrue(content.contains("FILE PATH: config.properties"));
        Assertions.assertTrue(content.contains("key=value"));

        // 验证未包含其他文件
        Assertions.assertFalse(content.contains("FILE PATH: image.png"));
    }
}