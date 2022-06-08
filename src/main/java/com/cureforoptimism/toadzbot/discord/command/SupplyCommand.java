package com.cureforoptimism.toadzbot.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SupplyCommand implements ToadzCommand {

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


    return null;
  }
}
