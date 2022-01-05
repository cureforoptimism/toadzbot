package com.cureforoptimism.toadzbot.service;

import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.domain.Coins.CoinFullData;
import lombok.Getter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CoinGeckoService implements MagicValueService {
  private final CoinGeckoApiClient client;
  @Getter private CoinFullData coinFullData;

  public CoinGeckoService(CoinGeckoApiClient client) {
    this.client = client;
  }

  @Scheduled(fixedDelay = 30000)
  @Override
  public void refreshMagicPrice() {
    this.coinFullData = client.getCoinById("magic");
  }
}
