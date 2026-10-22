package com.runa.smarter.umbrella.integration.graph.wds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WdsApiResponse<T>(int code, String message, T data, boolean success) {

}
