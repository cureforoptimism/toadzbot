package com.cureforoptimism.toadzbot.repository;

import com.cureforoptimism.toadzbot.domain.RarityRank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToadzRarityRankRepository extends JpaRepository<RarityRank, Long> {
  RarityRank findByToadId(Long toadId);
}
