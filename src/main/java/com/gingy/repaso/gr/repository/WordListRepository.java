package com.gingy.repaso.gr.repository;

import com.gingy.repaso.gr.domain.WordList;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WordListRepository extends JpaRepository<WordList, Long> {

    List<WordList> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    @EntityGraph(attributePaths = "items")
    Optional<WordList> findWithItemsById(Long id);
}
