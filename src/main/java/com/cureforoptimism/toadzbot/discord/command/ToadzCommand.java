package com.cureforoptimism.toadzbot.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

public interface ToadzCommand {
  String getName();

  String getDescription();

  String getUsage();

  Mono<Message> handle(MessageCreateEvent event);
}
