package hxj.tech.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 作用在方法上，在运行时生效
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface MyAnnotaion {
    // 注解的参数：参数类型 参数名()
    // 如果加上 default 则可以不写值，如果没有默认值则必须在注解使用的地方用它。
    // 如果只有一个参数，建议使用 value 命名，value 在调用的时候可以省略不写。
    String name() default "AlbertZhao";
    int age();
    String[] schools() default {"苏州大学","清华大学"};
}
