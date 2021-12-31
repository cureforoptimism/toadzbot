package com.cureforoptimism.toadzbot.discord.listener;

import com.cureforoptimism.toadzbot.discord.command.ToadzCommand;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class ToadzCommandListener {
  private final Collection<ToadzCommand> commands;

  public ToadzCommandListener(ApplicationContext applicationContext) {
    commands = applicationContext.getBeansOfType(ToadzCommand.class).values();
  }

  public void handle(MessageCreateEvent event) {
    try {
      String message = event.getMessage().getContent().toLowerCase();
      if (message.toLowerCase().startsWith("croak")) {
        message = "!croak";
      } else if (!message.startsWith("!")) {
        return;
      }

      // Trim leading !
      String[] parts = message.split(" ");
      if (parts.length > 0) {
        String commandName = parts[0].substring(1);

        Flux.fromIterable(commands)
            .filter(command -> command.getName().equals(commandName))
            .next()
            .flatMap(
                command -> {
                  // Verify that this is a message in a server and not a DM (for now)
                  Message msg = event.getMessage();
                  if (msg.getGuildId().isEmpty()) {
                    return Mono.empty();
                  }

                  return command.handle(event);
                })
            .block();
      }
    } catch (Exception ex) {
      log.error("Error received in listener loop. Will resume.", ex);
    }
  }
}
