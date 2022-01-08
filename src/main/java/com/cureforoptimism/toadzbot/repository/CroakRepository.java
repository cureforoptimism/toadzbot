package com.cureforoptimism.toadzbot.repository;

import com.cureforoptimism.toadzbot.domain.Croak;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CroakRepository extends JpaRepository<Croak, Long> {
  Croak findFirstByOrderByIdDesc();

  Optional<Croak> findFirstByDiscordUserIdOrderByCreatedAtDesc(Long discordUserId);
}
