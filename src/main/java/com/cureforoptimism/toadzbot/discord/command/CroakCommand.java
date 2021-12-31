package com.cureforoptimism.toadzbot.discord.command;

import com.cureforoptimism.toadzbot.domain.Croak;
import com.cureforoptimism.toadzbot.repository.CroakRepository;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class CroakCommand implements ToadzCommand {
  private final CroakRepository croakRepository;

  @Override
  public String getName() {
    return "croak";
  }

  @Override
  public String getDescription() {
    return "CROAK!";
  }

  @Override
  public String getUsage() {
    return null;
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event) {
    croakRepository.save(Croak.builder().discordId(event.getMessage().getUserData().username() + "#" + event.getMessage().getUserData().discriminator()).build());

    return event
        .getMessage()
        .getChannel()
        .flatMap(
            c ->
                c.createMessage(
                    "<:toad:913690221088997376> croak count: "
                        + croakRepository.findFirstByOrderByIdDesc().getId()
                        + "! Keep croakinggg"));
  }
}
