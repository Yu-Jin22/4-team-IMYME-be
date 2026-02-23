package com.imyme.mine.domain.pvp.controller;

import com.imyme.mine.domain.pvp.dto.response.MyRoomsResponse;
import com.imyme.mine.domain.pvp.service.PvpRoomService;
import com.imyme.mine.global.common.response.ApiResponse;
import com.imyme.mine.global.security.UserPrincipal;
import com.imyme.mine.global.security.annotation.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "11. PvP History", description = "PvP 대결 기록 API")
@Slf4j
@RestController
@RequestMapping("/pvp/histories")
@RequiredArgsConstructor
public class PvpHistoryController {

    private final PvpRoomService pvpRoomService;

    /**
     * 4.8 내 PvP 기록 조회
     */
    @Operation(summary = "내 PvP 기록 조회", description = "내 PvP 대결 기록을 커서 페이징으로 조회합니다. 정렬 옵션(finishedAt, score)과 필터링(categoryId, keywordId, includeHidden)을 지원합니다.")
    @SecurityRequirement(name = "JWT")
    @GetMapping
    public ApiResponse<MyRoomsResponse> getMyHistories(
            @CurrentUser UserPrincipal principal,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long keywordId,
            @RequestParam(defaultValue = "false") boolean includeHidden,
            @RequestParam(defaultValue = "finishedAt") String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        log.info("내 PvP 기록 조회: userId={}, categoryId={}, keywordId={}, sort={}",
                principal.getId(), categoryId, keywordId, sort);

        MyRoomsResponse response = pvpRoomService.getMyHistories(
                principal.getId(), categoryId, keywordId, includeHidden, sort, cursor, size);

        return ApiResponse.success(response);
    }
}