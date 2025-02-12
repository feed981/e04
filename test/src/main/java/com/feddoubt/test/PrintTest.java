package com.feddoubt.test;

public class PrintTest {

    public static void main(String[] args) {
    }

    public static String convertToKebabCase(String input) {
        // 使用正則表達式找到大寫字母並在它們前面插入 "-"
        String result = input.replaceAll("([a-z])([A-Z])", "$1-$2");
        // 將整個字符串轉為小寫
        return result.toLowerCase();
    }


    public static String convertFirstUpperToLower(String input) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                return input.substring(0, i) + Character.toLowerCase(c) + input.substring(i + 1);
            }
        }
        return input;
    }
}
