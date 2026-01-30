package com.expense.chat_service.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    private T data;

    private StatusResponse status;

    public BaseResponse(T data, String message, Integer statusCode) {
        this.data = data;
        this.status = new StatusResponse();
        this.status.setCode(statusCode);
        this.status.setMessage(message);
    }

}
