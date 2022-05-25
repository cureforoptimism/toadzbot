package com.cureforoptimism.toadzbot.discord.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ToadCommand implements ToadzCommand {

  @Override
  public String getName() {
    return "toad";
  }

  @Override
  public String getDescription() {
    return null;
  }

  @Override
  public String getUsage() {
    return null;
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event) {
    String msg = event.getMessage().getContent();
    String[] parts = msg.split(" ");

    //    if (parts.length == 2) {
    //      String tokenId = parts[1];
    //      Optional<EmbedCreateSpec> embed = utilities.getSmolEmbed(tokenId);
    //      return embed
    //          .map(
    //              embedCreateSpec ->
    //                  event.getMessage().getChannel().flatMap(c ->
    // c.createMessage(embedCreateSpec)))
    //          .orElse(Mono.empty());
    //    }
    //
    return Mono.empty();
  }

  //  @Override
  //  public Mono<Void> handle(ChatInputInteractionEvent event) {
  //    log.info("/smol command received");
  //
  //    try {
  //      final var tokenId = event.getOption("id").orElse(null);
  //      if (tokenId == null) {
  //        return null;
  //      }
  //
  //      if (tokenId.getValue().isEmpty()) {
  //        return Mono.empty();
  //      }
  //
  //      final var tokenIdStrOpt = tokenId.getValue().get();
  //
  //      final var embed = utilities.getSmolEmbed(tokenIdStrOpt.getRaw());
  //      if (embed.isEmpty()) {
  //        return null;
  //      }
  //
  //      event.reply().withEmbeds(embed.get()).block();
  //
  //    } catch (Exception ex) {
  //      log.error("Error with smol command: " + ex.getMessage());
  //    }
  //
  //    return Mono.empty();
  //  }
}
