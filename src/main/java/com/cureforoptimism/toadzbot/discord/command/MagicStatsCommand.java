package com.cureforoptimism.toadzbot.discord.command;

import com.cureforoptimism.toadzbot.application.DiscordBot;
import com.cureforoptimism.toadzbot.domain.MarketPrice;
import com.cureforoptimism.toadzbot.service.MarketPriceMessageSubscriber;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateFields.Footer;
import discord4j.core.spec.EmbedCreateSpec;
import java.text.NumberFormat;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Component
public class MagicStatsCommand implements ToadzCommand {
  private final DiscordBot discordBot;
  private final MarketPriceMessageSubscriber marketPriceMessageSubscriber;

  @Override
  public String getName() {
    return "magicstats";
  }

  @Override
  public String getDescription() {
    return "shows detailed information about $MAGIC";
  }

  @Override
  public String getUsage() {
    return null;
  }

  @Override
  public Mono<Message> handle(MessageCreateEvent event) {
    MarketPrice marketPrice = marketPriceMessageSubscriber.getLastMarketPlace();

    return event
        .getMessage()
        .getChannel()
        .flatMap(
            c ->
                c.createMessage(
                    EmbedCreateSpec.builder()
                        .title("MAGIC - $" + discordBot.getCurrentPrice())
                        .description(
                            "MC Rank: #"
                                + marketPrice.getMarketCapRank()
                                + "\n"
                                + "Market cap: $"
                                + NumberFormat.getIntegerInstance()
                                    .format(marketPrice.getMarketCap())
                                + "\n"
                                + "24 hour volume: $"
                                + NumberFormat.getIntegerInstance()
                                    .format(discordBot.getCurrentVolume24h())
                                + "\n"
                                + "In circulation: "
                                + NumberFormat.getIntegerInstance()
                                    .format(marketPrice.getCirculatingSupply())
                                + " MAGIC\n"
                                + "Total supply: "
                                + NumberFormat.getIntegerInstance()
                                    .format(marketPrice.getTotalSupply())
                                + " MAGIC\n"
                                + "Max supply: "
                                + NumberFormat.getIntegerInstance()
                                    .format(marketPrice.getMaxSupply())
                                + " MAGIC")
                        .addField(
                            "Current Prices",
                            "USD: `"
                                + discordBot.getCurrentPrice()
                                + "`\n"
                                + "ETH: `"
                                + String.format("`%.6f`", marketPrice.getPriceInEth())
                                + "`\n"
                                + "BTC: `"
                                + String.format("`%.8f`", marketPrice.getPriceInBtc())
                                + "`\n",
                            true)
                        .addField(
                            "Price Changes",
                            "1h: `"
                                + String.format("`%.2f%%`", discordBot.getCurrentChange1h())
                                + "`\n"
                                + "24h: `"
                                + String.format("`%.2f%%`", discordBot.getCurrentChange())
                                + "`\n"
                                + "7d: `"
                                + String.format(
                                    "`%.2f%%`", marketPrice.getPriceChangePercentage7d())
                                + "`\n"
                                + "1m: `"
                                + String.format(
                                    "`%.2f%%`", marketPrice.getPriceChangePercentage30d())
                                + "`\n",
                            true)
                        .thumbnail(
                            "https://assets.coingecko.com/coins/images/18623/large/Magic.png?1635755672")
                        .footer(
                            Footer.of(
                                "Powered by Defined.fi",
                                "https://miro.medium.com/fit/c/262/262/1*GQz1T6gRmenQRhTS-aWiLA.png"))
                        .timestamp(Instant.now())
                        .build()));
  }
}
