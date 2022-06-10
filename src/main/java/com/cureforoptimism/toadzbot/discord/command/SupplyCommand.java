package com.cureforoptimism.toadzbot.discord.command;

import com.cureforoptimism.toadzbot.service.TreasureService;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class SupplyCommand implements ToadzCommand {
  private final TreasureService treasureService;

  @Override
  public String getName() {
    return "supply";
  }

  @Override
  public String getDescription() {
    return "Show the supply of axes and adventures";
  }

  @Override
  public String getUsage() {
    return null;
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event) {
    final var msg = treasureService.getSupplyMessage();
    if(msg != null) {
      event.getMessage().getChannel().flatMap(c -> c.createMessage(msg)).block();
    }

    return Mono.empty();
  }
}
