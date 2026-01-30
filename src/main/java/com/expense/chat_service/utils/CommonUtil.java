package com.expense.chat_service.utils;

import com.expense.chat_service.response.BaseResponse;
import com.expense.chat_service.response.StatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class CommonUtil {

    public <T> BaseResponse<T> setBaseResponse(T data, int code, String message) {
        StatusResponse statusResponse = new StatusResponse();
        statusResponse.setCode(code);
        statusResponse.setMessage(message);

        BaseResponse<T> baseResponse = new BaseResponse<>();
        baseResponse.setData(data);
        baseResponse.setStatus(statusResponse);
        return baseResponse;
    }

    public Pageable paginationData(int pageNo, int size, String sortBy) {
        Sort.Direction direction = Sort.Direction.DESC;
        Sort sortOrder = Sort.by(direction, sortBy);
        return PageRequest.of(pageNo, size, sortOrder);
    }

}
