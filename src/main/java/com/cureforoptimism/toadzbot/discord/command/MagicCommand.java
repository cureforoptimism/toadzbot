package com.cureforoptimism.toadzbot.discord.command;

import com.cureforoptimism.toadzbot.application.DiscordBot;
import com.cureforoptimism.toadzbot.service.MarketPriceMessageSubscriber;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Component
public class MagicCommand implements ToadzCommand {
  private final DiscordBot discordBot;
  private final MarketPriceMessageSubscriber marketPriceMessageSubscriber;

  @Override
  public String getName() {
    return "magic";
  }

  @Override
  public String getDescription() {
    return "shows $MAGIC price in USD and ETH";
  }

  @Override
  public String getUsage() {
    return null;
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event) {
    return event
        .getMessage()
        .getChannel()
        .flatMap(
            c ->
                c.createMessage(
                    "MAGIC: $"
                        + discordBot.getCurrentPrice()
                        + " ("
                        + String.format(
                            "`%.6f`",
                            marketPriceMessageSubscriber.getLastMarketPlace().getPriceInEth())
                        + " ETH"
                        + ")"));
  }
}
