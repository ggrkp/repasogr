package com.gingy.repaso.gr.repository;

import com.gingy.repaso.gr.domain.DictionaryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntry, Long> {

    List<DictionaryEntry> findByGreekKeyOrderByIdAsc(String greekKey);

    List<DictionaryEntry> findBySpanishKeyOrderByIdAsc(String spanishKey);

    Optional<DictionaryEntry> findFirstByGreekKeyAndSpanishKeyOrderByIdAsc(String greekKey, String spanishKey);

    @Modifying
    @Query("delete from DictionaryEntry e where e.source = :source")
    int deleteBySource(@Param("source") String source);
}
