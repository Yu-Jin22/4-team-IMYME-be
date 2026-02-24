package com.imyme.mine.domain.pvp.dto.response;

import com.imyme.mine.domain.pvp.entity.PvpRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 내 PvP 기록 조회 응답 (4.8)
 */
@Getter
@AllArgsConstructor
@Builder
public class MyRoomsResponse {
    private List<HistoryItem> histories;
    private PageMeta meta;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class HistoryItem {
        private Long historyId;
        private RoomInfo room;
        private CategoryInfo category;
        private KeywordInfo keyword;
        private PvpRole myRole;
        private MyResult myResult;
        private OpponentInfo opponent;
        private Boolean isHidden;
        private LocalDateTime finishedAt;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class RoomInfo {
        private Long id;
        private String name;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class CategoryInfo {
        private Long id;
        private String name;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class KeywordInfo {
        private Long id;
        private String name;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class MyResult {
        private Integer score;
        private Integer level;
        private Boolean isWinner;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class OpponentInfo {
        private Long id;
        private String nickname;
        private String profileImageUrl;
        private Integer level;
        private Integer score;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class PageMeta {
        private Integer size;
        private Boolean hasNext;
        private String nextCursor;
    }
}
