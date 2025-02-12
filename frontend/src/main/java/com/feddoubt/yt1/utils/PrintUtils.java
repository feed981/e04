package com.feddoubt.yt1.utils;

public class PrintUtils {

    /**
     * print useful sentence
     * @param args
     */
    public static void main(String[] args) {
        printConstructorInjection("YouTubeController" ,"YVCService");
        printConstructorInjection("YouTubeController" ,"DownloadLimiter");
        printConstructorInjection("YouTubeController" ,"UserUtils");
        printConstructorInjection("YouTubeController" ,"HashUtils");
//        printmq("UserLog");
    }

    // autowired ->> 構造注入 Constructor Injection  只能單個
    // 使用構造函數注入，可以確保 bean 是不可變的，避免了意外的依賴變更。
    public static void printConstructorInjection(String classname ,String bean){
        StringBuilder stringBuilder = new StringBuilder();
        String el = convertFirstUpperToLower(bean);
        stringBuilder.append(String.format("private %s %s;%n",bean ,el));

        stringBuilder.append(String.format("public %s",classname));
        stringBuilder.append(String.format("(%s %s",bean ,el));
//            stringBuilder.append(",");
        stringBuilder.append(String.format(") {%n"));
        stringBuilder.append(String.format("    this.%s = %s;%n",el ,el));

        stringBuilder.append("}");
        System.out.println(stringBuilder.toString());
    }

    public static void printmq(String bean){
        String el = convertFirstUpperToLower(bean);
        String queue = el + "Queue";
        String queues = convertToKebabCase(queue);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("-- configuration file --\n");
        stringBuilder.append("rabbitmq:\n");
        stringBuilder.append(String.format("  %s: %s%n",queues,queue));
        stringBuilder.append("\n");

        stringBuilder.append("-- RabbitMQConfig --\n");
        stringBuilder.append("@Bean\n");
        stringBuilder.append(String.format("public Queue %s() {);%n",queue));
        stringBuilder.append(String.format("    return new Queue(\"%s\", true);%n",queue));
        stringBuilder.append("}\n");
        stringBuilder.append("\n");

        stringBuilder.append("-- Listener --\n");
        stringBuilder.append(String.format("@RabbitListener(queues = \"${rabbitmq.%s}\")%n",queues));
        stringBuilder.append("@Async\n");
        stringBuilder.append(String.format("public void handle%s(%s %s) {%n",bean ,bean ,el));
        stringBuilder.append(String.format("    log.info(\"%s:{}\",%s);%n" ,el,el));
        stringBuilder.append("}\n");
        stringBuilder.append("\n");

        stringBuilder.append("-- send q --\n");
        stringBuilder.append(String.format("rabbitTemplate.convertAndSend(\"%s\", \"your T\");%n",queue));
        stringBuilder.append("\n");

        stringBuilder.append("-- RabbitResponse - send q and catch --\n");
        stringBuilder.append(String.format("rabbitResponse.queueMessageLog(\"%s\", \"your T\");%n",queue));
        System.out.println(stringBuilder.toString());
    }

    // convertAndSend ->> convert-and-send
    public static String convertToKebabCase(String input) {
        // 使用正則表達式找到大寫字母並在它們前面插入 "-"
        String result = input.replaceAll("([a-z])([A-Z])", "$1-$2");
        // 將整個字符串轉為小寫
        return result.toLowerCase();
    }

    // 首字母大寫轉小寫
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
