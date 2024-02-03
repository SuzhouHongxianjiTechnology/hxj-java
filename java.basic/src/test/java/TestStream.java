import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * ClassName:TestStream
 * Package:PACKAGE_NAME
 * Description:
 *
 * @Author AlbertZhao
 * @Create 1/14/2024 6:18 PM
 * @Version 1.0
 */
public class TestStream {
    @Test
    void testFileOutStream() throws IOException {
        // 1. 创建字节输出流对象(如果文件不存在，会自动创建)
        // 如果文件存在，会先清空再写入
        var fos = new FileOutputStream("fos.txt");
        // 2. 写数据
        fos.write("hello".getBytes());
        // 3. 释放资源
        // 不释放的话一直占用资源，其他程序无法访问
        fos.close();
    }
}
