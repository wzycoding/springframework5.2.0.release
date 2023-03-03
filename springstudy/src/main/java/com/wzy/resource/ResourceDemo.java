package com.wzy.resource;

import org.springframework.core.io.FileSystemResource;

import java.io.*;

/**
 * 使用可以写入文件的FileSystemResource
 */
public class ResourceDemo {
    public static void main(String[] args) throws IOException {
        FileSystemResource fileSystemResource = new FileSystemResource(
                "/Users/wzy/project/source-studty/springframework5.2.0.release/springstudy/src/main/java/com/wzy/resource/a.txt");
        File file = fileSystemResource.getFile();

        System.out.println(file.length());

        OutputStream outputStream = fileSystemResource.getOutputStream();

        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
        bufferedWriter.write("我爱你中国🇨🇳");

        bufferedWriter.flush();
    }
}
