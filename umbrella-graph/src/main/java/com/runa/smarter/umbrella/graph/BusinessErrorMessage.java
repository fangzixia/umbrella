package com.runa.smarter.umbrella.graph;

/** 业务异常与 API 错误文案：统一「原因 + 解决办法」格式（简短提示类文案可不使用）。 */
public final class BusinessErrorMessage {

    private BusinessErrorMessage() {
    }

    public static String format(String reason, String remedy) {
        return "原因：" + reason + "。解决办法：" + remedy;
    }

}
