package com.gingy.repaso.gr.repository;

import com.gingy.repaso.gr.domain.OwnWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OwnWordRepository extends JpaRepository<OwnWord, Long> {

    Optional<OwnWord> findFirstByGreekKeyAndSpanishKeyOrderByIdAsc(String greekKey, String spanishKey);

    List<OwnWord> findByGreekKeyOrSpanishKeyOrderByIdAsc(String greekKey, String spanishKey);

    List<OwnWord> findAllByOrderBySpanishKeyAscGreekKeyAsc();

    /** Words whose normalized Greek or Spanish text contains the pattern (e.g. "%casa%"). */
    @Query("select w from OwnWord w where w.greekKey like :pattern or w.spanishKey like :pattern "
            + "order by w.spanishKey asc, w.greekKey asc")
    List<OwnWord> findContaining(@Param("pattern") String pattern);

    boolean existsByGreekKeyAndSpanishKeyAndIdNot(String greekKey, String spanishKey, Long id);
}
