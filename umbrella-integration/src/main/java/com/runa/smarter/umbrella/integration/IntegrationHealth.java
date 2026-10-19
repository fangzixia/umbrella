package com.runa.smarter.umbrella.integration;

/** {@link IntegrationGateway#checkHealth()} 的探测结果摘要。 */
public record IntegrationHealth(boolean databaseAvailable, boolean redisAvailable, String message) {

}
