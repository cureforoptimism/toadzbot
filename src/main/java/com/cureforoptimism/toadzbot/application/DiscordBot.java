package com.cureforoptimism.toadzbot.application;

import com.cureforoptimism.toadzbot.discord.events.RefreshEvent;
import com.cureforoptimism.toadzbot.discord.listener.ToadzCommandListener;
import com.cureforoptimism.toadzbot.service.TokenService;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.spec.MessageCreateSpec;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DiscordBot implements ApplicationRunner {
  final ApplicationContext context;
  static GatewayDiscordClient client;
  final TokenService tokenService;

  // TODO: This sucks. Makes this suck less with a rational pattern.
  @Getter Double currentPrice;
  @Getter Double currentChange;
  @Getter Double currentChange12h;
  @Getter Double currentChange4h;
  @Getter Double currentChange1h;
  @Getter Double currentVolume24h;
  @Getter Double currentVolume12h;
  @Getter Double currentVolume4h;
  @Getter Double currentVolume1h;

  public DiscordBot(ApplicationContext context, TokenService tokenService) {
    this.context = context;
    this.tokenService = tokenService;
  }

  public void refreshMagicPrice(
      Double price,
      Double usd24HChange,
      Double change12h,
      Double change4h,
      Double change1h,
      Double volume24h,
      Double volume12h,
      Double volume4h,
      Double volume1h) {
    currentPrice = price;
    currentChange = usd24HChange;
    currentChange12h = change12h;
    currentChange4h = change4h;
    currentChange1h = change1h;
    currentVolume24h = volume24h;
    currentVolume12h = volume12h;
    currentVolume4h = volume4h;
    currentVolume1h = volume1h;

    client.getEventDispatcher().publish(new RefreshEvent(null, null));
  }

  @Override
  public void run(ApplicationArguments args) {
    ToadzCommandListener mbotCommandListener = new ToadzCommandListener(context);

    client =
        DiscordClientBuilder.create(tokenService.getDiscordToken())
            .build()
            .gateway()
            .login()
            .block();

    if (client != null) {
      client
          .getEventDispatcher()
          .on(MessageCreateEvent.class)
          .subscribe(mbotCommandListener::handle);

      client
          .on(RefreshEvent.class)
          .subscribe(
              event -> {
                String nickName = ("MAGIC $" + currentPrice);
                String presence = String.format("24h: %.2f%%", currentChange);
                client.getGuilds().toStream().forEach(g -> g.changeSelfNickname(nickName).block());
                client
                    .updatePresence(ClientPresence.online(ClientActivity.watching(presence)))
                    .block();
              });
    }

    log.info("Discord client logged in");
  }

  public void postMessage(MessageCreateSpec messageCreateSpec, List<Long> discordChannelIds) {
    for (Long discordChannelId : discordChannelIds) {
      try {
        final var messages =
            client
                .getChannelById(Snowflake.of(discordChannelId))
                .ofType(MessageChannel.class)
                .flatMap(c -> c.createMessage(messageCreateSpec));

        messages.block();
      } catch (Exception ex) {
        log.warn("Unable to post to channel: " + discordChannelId, ex);
      }
    }
  }
}
