package hxj.tech;

/**
 * ClassName:MyRunnable
 * Package:hxj.tech
 * Description:
 *
 * @Author AlbertZhao
 * @Create 1/14/2024 8:00 PM
 * @Version 1.0
 */
public class MyRunnable implements Runnable{
    @Override
    public void run() {
        for (int i = 0; i < 20; i++) {
            // Thread.currentThread()获取当前线程
            System.out.println(Thread.currentThread().getName()+":"+i+"线程开启了");
        }
    }
}
