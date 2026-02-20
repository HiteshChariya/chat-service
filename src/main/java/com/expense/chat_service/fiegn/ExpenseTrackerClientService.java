package com.expense.chat_service.fiegn;

import com.expense.chat_service.config.FeignConfig;
import com.expense.chat_service.response.BaseResponse;
import com.expense.chat_service.response.TripMemberResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "expense-tracker",
        url = "${expense.tracker.url}",
        configuration = FeignConfig.class
)
public interface ExpenseTrackerClientService {

    @GetMapping("/shared-trips/{tripId}/members")
    BaseResponse<List<TripMemberResponse>> listTripMembers(@PathVariable("tripId") Long tripId);
}
