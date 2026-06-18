package pers.kieran.study.kieranaiagent.tools;

import cn.hutool.core.io.FileUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import pers.kieran.study.kieranaiagent.constant.FileConstant;

import java.io.File;

/**
 * 文件操作工具类（提供文件读写功能）
 */
public class FileOperationTool {

    private final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/file";

    @Tool(description = "Read content from a text file in the temporary file directory. Do not use this tool to read generated PDF files.")
    public String readFile(@ToolParam(description = "Name of the text file to read, not an absolute path") String fileName) {
        File file = resolveSafeFile(fileName);
        try {
            if (!file.exists() || !file.isFile()) {
                return "Error reading file: File not exist: " + file.getAbsolutePath();
            }
            return FileUtil.readUtf8String(file);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Write content to a text file in the temporary file directory")
    public String writeFile(
            @ToolParam(description = "Name of the file to write, not an absolute path") String fileName,
            @ToolParam(description = "Content to write to the file") String content) {
        File file = resolveSafeFile(fileName);
        try {
            FileUtil.mkdir(FILE_DIR);
            FileUtil.writeUtf8String(content, file);
            return "File written successfully to: " + file.getAbsolutePath();
        } catch (Exception e) {
            return "Error writing to file: " + e.getMessage();
        }
    }

    private File resolveSafeFile(String fileName) {
        String safeFileName = new File(fileName == null ? "" : fileName).getName();
        if (safeFileName.isBlank()) {
            safeFileName = "untitled.txt";
        }
        return new File(FILE_DIR, safeFileName);
    }
}
