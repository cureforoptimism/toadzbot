package com.cureforoptimism.toadzbot.service;

import com.cureforoptimism.toadzbot.Constants;
import com.cureforoptimism.toadzbot.Utilities;
import com.cureforoptimism.toadzbot.application.DiscordBot;
import com.cureforoptimism.toadzbot.domain.ToadzSale;
import com.cureforoptimism.toadzbot.repository.ToadzRarityRankRepository;
import com.cureforoptimism.toadzbot.repository.ToadzSaleRepository;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.tweet.MediaCategory;
import io.github.redouane59.twitter.dto.tweet.TweetParameters;
import io.github.redouane59.twitter.dto.tweet.TweetParameters.Media;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SalesService {
  private final ToadzSaleRepository toadzSaleRepository;
  private final CoinGeckoService coinGeckoService;
  private Date lastPostedBlockTimestamp = null;
  private final DiscordBot discordBot;
  private final Utilities utilities;

  private final TwitterClient twitterClient;

  private final ToadzRarityRankRepository toadzRarityRankRepository;

  @Scheduled(fixedDelay = 30000, initialDelay = 10000)
  public synchronized void postNewSales() {
    if (discordBot.getCurrentPrice() == null) {
      return;
    }

    //    if (System.getenv("PROD") == null) {
    //      return;
    //    }

    if (lastPostedBlockTimestamp == null) {
      ToadzSale lastPostedSale =
          toadzSaleRepository.findFirstByPostedIsTrueOrderByBlockTimestampDesc();

      if (lastPostedSale != null) {
        lastPostedBlockTimestamp =
            toadzSaleRepository
                .findFirstByPostedIsTrueOrderByBlockTimestampDesc()
                .getBlockTimestamp();
      }
    }

    // Can uncomment and replace with recent ID to test new functionality (delete existing tweet
    // first!)
    //        List<ToadzSale> newSales =
    //
    // donkSaleRepository.findById("https://arbiscan.io/tx/0x1de03a7bb555289aeb82091e54f7b58a66e293747acaa9072ffc002bea8e0a0e").stream().toList();

    List<ToadzSale> newSales =
        toadzSaleRepository.findByBlockTimestampIsAfterAndPostedIsFalseOrderByBlockTimestampAsc(
            lastPostedBlockTimestamp);
    if (!newSales.isEmpty()) {
      final Optional<Double> ethMktPriceOpt = coinGeckoService.getEthPrice();
      if (ethMktPriceOpt.isEmpty()) {
        // This will retry once we have an ethereum price
        return;
      }
      final NumberFormat decimalFormatZeroes = new DecimalFormat("#,###.00");
      final NumberFormat decimalFormatOptionalZeroes = new DecimalFormat("0.##");
      Double currentPrice = discordBot.getCurrentPrice();

      List<Long> channelList = new ArrayList<>();
      if (System.getenv("PROD") != null) {
        channelList.add(Constants.CHANNEL_SALES_BOT);
      }

      // Odd; additional channels don't get the image. Maybe need separate file uploads.
      channelList.add(Constants.CHANNEL_TEST_GENERAL);

      for (ToadzSale toadzSale : newSales) {
        final BigDecimal usdPrice =
            toadzSale.getSalePrice().multiply(BigDecimal.valueOf(currentPrice));
        final Double ethPrice = usdPrice.doubleValue() / ethMktPriceOpt.get();
        final String ethValue = decimalFormatOptionalZeroes.format(ethPrice);
        final String usdValue = decimalFormatZeroes.format(usdPrice);

        final int tokenId = toadzSale.getTokenId();

        final var rarityRank = toadzRarityRankRepository.findByToadId((long) tokenId).getRank();

        final var img = utilities.getToadzImage(Integer.toString(tokenId));
        if (img == null) {
          continue;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
          ImageIO.write(img, "png", baos);
        } catch (IOException e) {
          e.printStackTrace();
          return;
        }

        byte[] bytes = baos.toByteArray();
        // TODO: Uncomment if we add twitter
        final var mediaResponse =
            twitterClient.uploadMedia(
                toadzSale.getTokenId() + "_toadstoolz.png", bytes, MediaCategory.TWEET_IMAGE);
        final var media = Media.builder().mediaIds(List.of(mediaResponse.getMediaId())).build();
        TweetParameters tweetParameters =
            TweetParameters.builder()
                .media(media)
                .text(
                    "Toadstoolz #"
                        + tokenId
                        + " (Rarity Rank #"
                        + rarityRank
                        + ")\nSold for\nMAGIC: "
                        + decimalFormatOptionalZeroes.format(toadzSale.getSalePrice())
                        + "\nUSD: $"
                        + usdValue
                        + "\nETH: "
                        + ethValue
                        + "\n\n"
                        + "https://marketplace.treasure.lol/collection/toadstoolz/"
                        + toadzSale.getTokenId()
                        + "\n\n"
                        + "#toadstoolz #treasuredao")
                .build();
        twitterClient.postTweet(tweetParameters);

        final MessageCreateSpec messageCreateSpec =
            MessageCreateSpec.builder()
                .addFile("toadz_" + tokenId + ".png", new ByteArrayInputStream(bytes))
                .addEmbed(
                    EmbedCreateSpec.builder()
                        .description(
                            "**SOLD**\nToadstoolz #"
                                + tokenId
                                + " (Rarity Rank: **#"
                                + rarityRank
                                + "**)")
                        .addField(
                            "MAGIC",
                            decimalFormatOptionalZeroes.format(toadzSale.getSalePrice()),
                            true)
                        .addField("USD", "$" + usdValue, true)
                        .addField("ETH", "Ξ" + ethValue, true)
                        .image("attachment://toadz_" + tokenId + ".png")
                        .timestamp(toadzSale.getBlockTimestamp().toInstant())
                        .build())
                .build();

        discordBot.postMessage(messageCreateSpec, channelList);

        toadzSale.setPosted(true);
        toadzSaleRepository.save(toadzSale);

        log.info("New sale posted for " + toadzSale.getTokenId());

        if (toadzSale.getBlockTimestamp().after(lastPostedBlockTimestamp)) {
          lastPostedBlockTimestamp = toadzSale.getBlockTimestamp();
        }
      }
    }
  }
}
