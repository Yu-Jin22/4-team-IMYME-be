package com.imyme.mine.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String message;
    private final ErrorData data;

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorData {
        private final String errorCode;
        private final List<FieldError> errors;
        private final Map<String, Object> meta;

        public ErrorData(String errorCode, List<FieldError> errors, Map<String, Object> meta) {
            this.errorCode = errorCode;
            this.errors = errors == null ? List.of() : List.copyOf(errors);
            this.meta = meta == null ? Map.of() : Map.copyOf(meta);
        }

        public String getErrorCode() {
            return errorCode;
        }

        public List<FieldError> getErrors() {
            return errors;
        }

        public Map<String, Object> getMeta() {
            return meta;
        }
    }

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String reason;
        private final Object value;
    }

    public static ErrorResponse of(String message, String errorCode) {
        return ErrorResponse.builder()
            .message(message)
            .data(ErrorData.builder()
                .errorCode(errorCode)
                .build())
            .build();
    }

    public static ErrorResponse of(String message, String errorCode, Map<String, Object> meta) {
        return ErrorResponse.builder()
            .message(message)
            .data(ErrorData.builder()
                .errorCode(errorCode)
                .meta(meta)
                .build())
            .build();
    }

    public static ErrorResponse of(String message, String errorCode, List<FieldError> errors) {
        return ErrorResponse.builder()
            .message(message)
            .data(ErrorData.builder()
                .errorCode(errorCode)
                .errors(errors)
                .build())
            .build();
    }
}
