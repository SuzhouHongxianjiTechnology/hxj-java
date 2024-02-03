package hxj.tech;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * ClassName:${NAME}
 * Package:hxj.tech
 * Description:
 *
 * @Author AlbertZhao
 * @Create 1/14/2024 6:15 PM
 * @Version 1.0
 *///TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    private static List<String> list;
    public static void main(String[] args) throws NoSuchFieldException, ClassNotFoundException {
        PrintFieldGenericType("hxj.tech.Main","list");
    }

    static void PrintFieldGenericType(String classFullName,String filedName) throws ClassNotFoundException, NoSuchFieldException {
        Field field = Class.forName(classFullName).getDeclaredField(filedName);
        Type genericType = field.getGenericType();
        if (genericType != null && genericType instanceof ParameterizedType) {
            Type actualType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            System.out.println(actualType.getTypeName());
        }
    }
}