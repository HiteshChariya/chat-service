package com.expense.chat_service.fiegn;

import com.expense.chat_service.config.FeignConfig;
import com.expense.chat_service.response.BaseResponse;
import com.expense.chat_service.response.UserDataResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "user-service",
        url = "${user.service.url}",
        configuration = FeignConfig.class // configurable
)
public interface UserClientService {

    @GetMapping("/user/by-id/{id}")
    BaseResponse<List<UserDataResponse>> userDetailsById(@PathVariable("id") Long id);

    @GetMapping("/actuator/health")
    Map<String, Object> health();
}
