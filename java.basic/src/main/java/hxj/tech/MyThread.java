package hxj.tech;

/**
 * ClassName:MyThread
 * Package:hxj.tech
 * Description:
 *
 * @Author AlbertZhao
 * @Create 1/14/2024 7:54 PM
 * @Version 1.0
 */
public class MyThread extends Thread{
    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            System.out.println(getName()+":"+i+"线程开启了");
        }
    }
}
