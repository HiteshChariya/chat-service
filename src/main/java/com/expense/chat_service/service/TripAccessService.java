package com.expense.chat_service.service;

import com.expense.chat_service.fiegn.ExpenseTrackerClientService;
import com.expense.chat_service.request.Principle;
import com.expense.chat_service.response.BaseResponse;
import com.expense.chat_service.response.TripMemberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripAccessService {

    private static final String STATUS_REMOVED = "REMOVED";
    private final ExpenseTrackerClientService expenseTrackerClientService;

    public void requireTripMembership(Long tripId, Principle principle) {
        if (tripId == null || principle == null || principle.getId() == null) {
            throw new AccessDeniedException("You do not have access to this trip chat");
        }

        BaseResponse<List<TripMemberResponse>> response = expenseTrackerClientService.listTripMembers(tripId);
        List<TripMemberResponse> members =
                response != null && response.getData() != null ? response.getData() : Collections.emptyList();

        boolean isMember = members.stream().anyMatch(member ->
                principle.getId().equals(member.getUserId())
                        && !STATUS_REMOVED.equalsIgnoreCase(String.valueOf(member.getStatus()))
        );

        if (!isMember) {
            log.warn("Trip chat access denied. tripId={}, userId={}", tripId, principle.getId());
            throw new AccessDeniedException("You do not have access to this trip chat");
        }
    }
}
