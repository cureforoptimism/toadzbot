package com.cureforoptimism.toadzbot.repository;

import com.cureforoptimism.toadzbot.domain.ToadzSale;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToadzSaleRepository extends JpaRepository<ToadzSale, Long> {
  ToadzSale findFirstByPostedIsTrueOrderByBlockTimestampDesc();

  boolean existsByTxAndTokenId(String tx, Integer tokenId);

  List<ToadzSale> findByBlockTimestampIsAfterAndPostedIsFalseOrderByBlockTimestampAsc(
      Date blockTimestamp);
}
