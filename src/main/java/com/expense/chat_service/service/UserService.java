package com.expense.chat_service.service;

import com.expense.chat_service.fiegn.UserClientService;
import com.expense.chat_service.response.BaseResponse;
import com.expense.chat_service.response.UserDataResponse;
import com.expense.chat_service.utils.CommonUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final CommonUtil commonUtil;
    private final UserClientService userClientService;

//    @CircuitBreaker(name = "userService", fallbackMethod = "userDetailsByIdFallback")
    public BaseResponse<List<UserDataResponse>> userDetailsById(Long id) {
        log.info("Check user Id : {}", id);
        BaseResponse<List<UserDataResponse>> response = userClientService.userDetailsById(id);
        return response;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "healthFallback")
    public Map<String, Object> health() {
        return userClientService.health();
    }

    /* ================= FALLBACKS ================= */

    private BaseResponse<List<UserDataResponse>> userDetailsFallback(Throwable ex) {
        return commonUtil.setBaseResponse(Collections.emptyList(), HttpStatus.SERVICE_UNAVAILABLE.value(), "User service unavailable. Will retry later.");
    }

    private BaseResponse<UserDataResponse> userDetailsByIdFallback(Throwable ex) {
        return commonUtil.setBaseResponse(null, HttpStatus.SERVICE_UNAVAILABLE.value(), "User service unavailable. Will retry later.");
    }

    private Map<String, Object> healthFallback(Throwable ex) {
        return Map.of(
                "status", "DOWN",
                "service", "email-service",
                "reason", ex.getMessage()
        );
    }

    private BaseResponse<String> emailFallback(
            Map<String, Object> request,
            Throwable ex
    ) {
        return commonUtil.setBaseResponse(null, HttpStatus.SERVICE_UNAVAILABLE.value(), "Email service unavailable. Will retry later.");
    }
}
