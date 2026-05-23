package pers.kieran.study.kieranaiagent.constant;

/**
 * @author Kieran_Chase
 * @project kieran-ai-agent
 * @date 2026/5/20
 */

import java.io.File;

/**
 * 文件常量
 */
public interface FileConstant {
    /**
     * 文件保存目录
     */
    String FILE_SAVE_DIR=System.getProperty("user.dir")+"/tmp";
    //或者
    //String FILE_SAVE_DIR = System.getProperty("user.dir") + File.separator + "tmp";
}
