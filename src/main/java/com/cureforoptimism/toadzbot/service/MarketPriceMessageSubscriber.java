package com.cureforoptimism.toadzbot.service;

import com.cureforoptimism.toadzbot.application.DiscordBot;
import com.cureforoptimism.toadzbot.domain.MarketPrice;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketPriceMessageSubscriber {
  private final DiscordBot discordBot;

  @Getter private MarketPrice lastMarketPlace;

  public void handleMessage(MarketPrice marketPrice) {
    lastMarketPlace = marketPrice;
    discordBot.refreshMagicPrice(
        marketPrice.getPrice(),
        marketPrice.getChange(),
        marketPrice.getVolume12h(),
        marketPrice.getVolume24h(),
        marketPrice.getChange1h(),
        marketPrice.getVolume24h(),
        marketPrice.getChange12h(),
        marketPrice.getChange4h(),
        marketPrice.getVolume1h());
  }
}
