package com.imyme.mine.domain.knowledge.repository;

import com.imyme.mine.domain.knowledge.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 지식 베이스 리포지토리
 */
@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    // 콘텐츠 해시 존재 여부 확인
    boolean existsByContentHash(String contentHash);

    // 콘텐츠 해시로 지식 조회
    Optional<KnowledgeBase> findByContentHash(String contentHash);

    // 임베딩 미생성 활성 지식 조회
    @Query("SELECT kb FROM KnowledgeBase kb WHERE kb.isActive = true AND kb.embedding IS NULL")
    List<KnowledgeBase> findAllWithoutEmbedding();

    // 키워드 ID로 활성 지식 조회
    @Query("SELECT kb FROM KnowledgeBase kb WHERE kb.keyword.id = :keywordId AND kb.isActive = true")
    List<KnowledgeBase> findByKeywordId(@Param("keywordId") Long keywordId);

    /**
     * 전체 벡터 유사도 검색
     * - 모든 활성 지식에서 유사도 검색
     * - 사용 시나리오: 일반 질의응답, 문서 요약
     */
    @Query(value = """
        SELECT kb.id, kb.keyword_id, kb.content, kb.embedding, kb.content_hash,
               kb.is_active, kb.created_at, kb.updated_at,
               (kb.embedding <=> CAST(:queryEmbedding AS vector)) AS distance
        FROM knowledge_base kb
        WHERE kb.is_active = true
          AND kb.embedding IS NOT NULL
        ORDER BY kb.embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarKnowledgeByVector(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("limit") int limit
    );

    /**
     * 키워드별 벡터 유사도 검색
     * - 특정 키워드에 연관된 지식만 검색
     * - 사용 시나리오: 키워드별 맞춤형 피드백 생성
     */
    @Query(value = """
        SELECT kb.id, kb.keyword_id, kb.content, kb.embedding, kb.content_hash,
               kb.is_active, kb.created_at, kb.updated_at,
               (kb.embedding <=> CAST(:queryEmbedding AS vector)) AS distance
        FROM knowledge_base kb
        WHERE kb.is_active = true
          AND kb.embedding IS NOT NULL
          AND kb.keyword_id = :keywordId
        ORDER BY kb.embedding <=> CAST(:queryEmbedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarKnowledgeByKeyword(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("keywordId") Long keywordId,
        @Param("limit") int limit
    );

    /**
     * 모든 활성 지식 조회 (최신순)
     * - 목적: 관리자 페이지에서 지식 목록 표시
     */
    @Query("SELECT kb FROM KnowledgeBase kb WHERE kb.isActive = true ORDER BY kb.createdAt DESC")
    List<KnowledgeBase> findAllActiveOrderByCreatedAtDesc();

    /**
     * 활성 지식 개수 조회
     * - 목적: 대시보드 통계
     */
    @Query("SELECT COUNT(kb) FROM KnowledgeBase kb WHERE kb.isActive = true")
    long countActiveKnowledge();

    /**
     * 임베딩 미생성 지식 개수 조회
     * - 목적: 배치 작업 모니터링
     */
    @Query("SELECT COUNT(kb) FROM KnowledgeBase kb WHERE kb.isActive = true AND kb.embedding IS NULL")
    long countWithoutEmbedding();
}
