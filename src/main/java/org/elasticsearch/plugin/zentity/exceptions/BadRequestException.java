package org.elasticsearch.plugin.zentity.exceptions;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.rest.RestStatus;

public class BadRequestException extends ElasticsearchStatusException {
    public BadRequestException(String message, Throwable cause) {
        super(message, RestStatus.BAD_REQUEST, cause);
    }

    public BadRequestException(String message) {
        this(message, null);
    }
}
