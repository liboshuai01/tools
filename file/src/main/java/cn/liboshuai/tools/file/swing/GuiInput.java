package cn.liboshuai.tools.file.swing;

import javax.swing.*;
import java.awt.*;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 极简参数输入工具 (基于 Swing 弹窗)
 * 支持 Shell 风格路径解析 (~, ${HOME}, ../, ./)
 */
public class GuiInput {

    public static class Item {
        String key;
        String label;
        String defaultValue;

        public Item(String key, String label, String defaultValue) {
            this.key = key;
            this.label = label;
            this.defaultValue = defaultValue;
        }
    }

    public static Map<String, String> askForm(String title, Item... items) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setPreferredSize(new Dimension(550, items.length * 40)); // 稍微加宽一点适应长路径

        Map<String, JTextField> fieldMap = new HashMap<>();

        for (Item item : items) {
            JLabel label = new JLabel(item.label + ":");
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            JTextField textField = new JTextField(item.defaultValue);
            panel.add(label);
            panel.add(textField);
            fieldMap.put(item.key, textField);
        }

        int result = JOptionPane.showConfirmDialog(null, panel, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            Map<String, String> resultMap = new HashMap<>();
            for (Item item : items) {
                JTextField field = fieldMap.get(item.key);
                // 核心：调用增强版路径解析
                String value = resolveLinuxPath(field.getText().trim());
                resultMap.put(item.key, value);
            }
            return resultMap;
        } else {
            System.out.println("用户取消了操作，程序退出。");
            System.exit(0);
            return null;
        }
    }

    public static String ask(String message, String defaultValue) {
        String input = JOptionPane.showInputDialog(null, message, defaultValue);
        if (input == null) {
            System.out.println("用户取消操作。");
            System.exit(0);
        }
        return resolveLinuxPath(input.trim());
    }

    /**
     * 增强版路径解析：支持 Shell 变量及标准 Linux 路径计算
     * 示例:
     * "~/data" -> "/home/lbs/data"
     * "../backup" -> "/home/lbs/project/backup" (假设当前运行在 project 下)
     * "/tmp/a/b/../../c" -> "/tmp/c"
     */
    private static String resolveLinuxPath(String pathStr) {
        if (pathStr == null || pathStr.isBlank()) {
            return pathStr;
        }

        try {
            // 1. 手动处理 Shell 特有的 ~ 和 ${HOME}
            // (Java NIO 不会自动处理这两个符号，所以必须先手动替换)
            String userHome = System.getProperty("user.home");
            if (pathStr.startsWith("~")) {
                // 处理 "~/abc" 和 单独的 "~"
                if (pathStr.length() == 1 || pathStr.charAt(1) == '/') {
                    pathStr = userHome + pathStr.substring(1);
                }
            } else if (pathStr.contains("${HOME}")) {
                pathStr = pathStr.replace("${HOME}", userHome);
            }

            // 2. 利用 Java NIO 进行标准路径规范化
            // Path.of(pathStr) : 创建路径对象
            // .toAbsolutePath(): 如果是相对路径(../ 或 ./), 基于当前工作目录(CWD)解析为绝对路径
            // .normalize()     : 消除冗余元素 (例如把 "a/./b/../c" 变成 "a/c")
            return Path.of(pathStr)
                    .toAbsolutePath()
                    .normalize()
                    .toString();

        } catch (InvalidPathException e) {
            // 如果路径包含非法字符（极其罕见），则原样返回，交给后续文件操作报错
            System.err.println("路径解析警告: " + e.getMessage());
            return pathStr;
        }
    }
}