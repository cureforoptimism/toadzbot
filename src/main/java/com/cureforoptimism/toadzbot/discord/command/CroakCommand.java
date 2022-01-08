package com.cureforoptimism.toadzbot.discord.command;

import com.cureforoptimism.toadzbot.domain.Croak;
import com.cureforoptimism.toadzbot.repository.CroakRepository;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CroakCommand implements ToadzCommand {
  private final CroakRepository croakRepository;
  private final Set<String> suffixes;

  public CroakCommand(CroakRepository croakRepository) {
    this.croakRepository = croakRepository;

    this.suffixes = Set.of("Toadally awesome!", "Keep croakin'", "Run it up the tadpole!");
  }

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
    final var previousCroak =
        croakRepository.findFirstByDiscordUserIdOrderByCreatedAtDesc(
            event.getMessage().getUserData().id().asLong());
    if (previousCroak.isPresent()) {
      Date previousCroakDate = previousCroak.get().getCreatedAt();

      if (previousCroakDate != null
          && previousCroak
              .get()
              .getCreatedAt()
              .after(new Date(System.currentTimeMillis() - 30000L))) {
        return Mono.empty();
      }
    }

    croakRepository.save(
        Croak.builder()
            .discordUserId(event.getMessage().getUserData().id().asLong())
            .discordId(
                event.getMessage().getUserData().username()
                    + "#"
                    + event.getMessage().getUserData().discriminator())
            .createdAt(new Date())
            .build());

    String suffix =
        suffixes.stream().skip(new Random().nextInt(suffixes.size())).findFirst().orElse("");

    return event
        .getMessage()
        .getChannel()
        .flatMap(
            c ->
                c.createMessage(
                    "<:toad:913690221088997376> **"
                        + NumberFormat.getIntegerInstance()
                            .format(croakRepository.findFirstByOrderByIdDesc().getId())
                        + " Croaks**, and counting! "
                        + suffix));
  }
}
