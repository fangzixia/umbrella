package com.runa.smarter.umbrella.integration.graph;

import com.runa.smarter.umbrella.graph.BusinessErrorMessage;

/** WDS 导航树拉取或解析失败。 */
public class IntegrationGraphException extends RuntimeException {

    public IntegrationGraphException(String message) {
        super(message);
    }

    public IntegrationGraphException(String message, Throwable cause) {
        super(message, cause);
    }

    public static IntegrationGraphException because(String reason, String remedy) {
        return new IntegrationGraphException(BusinessErrorMessage.format(reason, remedy));
    }

    public static IntegrationGraphException because(String reason, String remedy, Throwable cause) {
        return new IntegrationGraphException(BusinessErrorMessage.format(reason, remedy), cause);
    }

}
