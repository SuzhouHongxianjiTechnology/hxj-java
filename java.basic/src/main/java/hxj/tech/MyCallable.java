package hxj.tech;

import java.util.concurrent.Callable;

/**
 * ClassName:MyCallable
 * Package:hxj.tech
 * Description:
 *
 * @Author AlbertZhao
 * @Create 1/14/2024 8:02 PM
 * @Version 1.0
 */
public class MyCallable implements Callable<Object> {
    @Override
    public Object call() throws Exception {
        for (int i = 0; i < 10; i++) {
            System.out.println(Thread.currentThread().getName()+":"+i+"线程开启了");
        }

        // 返回值就表示线程运行完毕后的结果
        return null;
    }
}
