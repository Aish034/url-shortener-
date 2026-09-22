package com.schwab.urlshortener.repository;

import com.schwab.urlshortener.model.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByShortCode(String shortCode);

    List<ClickEvent> findByShortCodeOrderByClickedAtDesc(String shortCode);

    @Query("SELECT c FROM ClickEvent c WHERE c.shortCode = :shortCode AND c.clickedAt >= :since ORDER BY c.clickedAt DESC")
    List<ClickEvent> findRecent(@Param("shortCode") String shortCode, @Param("since") Instant since);
}
