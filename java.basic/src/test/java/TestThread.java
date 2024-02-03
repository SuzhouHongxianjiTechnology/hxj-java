import hxj.tech.MyCallable;
import hxj.tech.MyRunnable;
import hxj.tech.MyThread;
import org.junit.jupiter.api.Test;

import java.util.concurrent.FutureTask;

/**
 * ClassName:TestThread
 * Package:PACKAGE_NAME
 * Description:
 *
 * @Author AlbertZhao
 * @Create 1/14/2024 7:56 PM
 * @Version 1.0
 */
public class TestThread {
    @Test
    void testMyThread() {
        var myThread = new MyThread();
        myThread.start();
    }

    @Test
    void testMyRunnable() {
        var myRunnable = new MyRunnable();
        var thread = new Thread(myRunnable);
        thread.start();
    }

    @Test
    void testMyCallable() {
         var myCallable = new MyCallable();
         var futureTask = new FutureTask<>(myCallable);
         var thread = new Thread(futureTask);
         thread.start();
    }
}
