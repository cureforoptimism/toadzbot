package com.cureforoptimism.toadzbot.repository;

import com.cureforoptimism.toadzbot.domain.Toad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToadRepository extends JpaRepository<Toad, Long> {}
