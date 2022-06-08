package com.cureforoptimism.toadzbot;

import com.cureforoptimism.toadzbot.domain.RarityRank;
import com.cureforoptimism.toadzbot.domain.Toad;
import com.cureforoptimism.toadzbot.domain.Trait;
import com.cureforoptimism.toadzbot.repository.ToadRepository;
import com.cureforoptimism.toadzbot.repository.ToadzRarityRankRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.web3j.contracts.eip721.generated.ERC721Metadata;
import org.web3j.tx.exceptions.ContractCallException;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

@Component
@Slf4j
public class Utilities {
  @Autowired
  @Qualifier("erc721Metadata")
  private final ERC721Metadata erc721Metadata;

  @Autowired private final ToadRepository toadRepository;

  @Autowired private final ToadzRarityRankRepository toadzRarityRankRepository;
  private final WebDriver webDriver;

  public Utilities(
      ERC721Metadata erc721Metadata,
      ToadRepository toadRepository,
      ToadzRarityRankRepository toadzRarityRankRepository) {
    this.erc721Metadata = erc721Metadata;
    this.toadRepository = toadRepository;
    this.toadzRarityRankRepository = toadzRarityRankRepository;

    ChromeOptions options = new ChromeOptions();
    options.setExperimentalOption(
        "excludeSwitches", Collections.singletonList("enable-automation"));
    options.addArguments("window-size=600,600");
    options.addArguments("--headless");
    options.addArguments("--no-sandbox");
    webDriver = new ChromeDriver(options);
  }

  void generateRanks() {
    InputStream csv;
    CSVParser parser;

    try {
      csv = new ClassPathResource("collection_toadstoolz.csv").getInputStream();
    } catch (IOException ex) {
      log.error("Unable to read CSV", ex);
      return;
    }

    try {
      parser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(new InputStreamReader(csv));
    } catch (IOException ex) {
      log.error("Unable to parse CSV", ex);
      return;
    }

    parser.stream()
        .forEach(
            r -> {
              final var rank = r.get("nft_rank");
              final var id = r.get("id");
              final var score = r.get("rarity score new");

              toadzRarityRankRepository.save(
                  RarityRank.builder()
                      .toadId(Long.parseLong(id))
                      .rank(Integer.parseInt(rank))
                      .score(Double.parseDouble(score.replace(",", "")))
                      .build());
            });
    log.info("rarities generated and saved");
  }

  public Optional<Toad> getToad(String id) {
    var toadOpt = toadRepository.findById(Long.parseLong(id));
    if (toadOpt.isPresent()) {
      return toadOpt;
    }

    final ObjectMapper objMapper = new ObjectMapper();

    String tokenUriResponse = "";
    while (tokenUriResponse.isEmpty()) {
      try {
        tokenUriResponse = erc721Metadata.tokenURI(new BigInteger(id)).send();
      } catch (ContractCallException ex) {
        if (ex.getMessage().contains("Token does not exist")) {
          return Optional.empty();
        }
      } catch (Exception ex) {
        log.error("Error retrieving toadz image: " + id, ex);
      } finally {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          // meh
        }
      }
    }

    try {
      Base64.Decoder decoder = Base64.getDecoder();

      // Should probably sanity check this stuff first, but I'm going to just assume this will
      // always start this this prefix. YOLO.
      tokenUriResponse = tokenUriResponse.replace("data:application/json;base64,", "");
      tokenUriResponse = new String(decoder.decode(tokenUriResponse));
      log.info(tokenUriResponse);
    } catch (IllegalArgumentException ex) {
      log.error("Unable to base64 decode token image");
    }

    try {
      final var toad =
          objMapper.readValue(tokenUriResponse.getBytes(StandardCharsets.UTF_8), Toad.class);

      toad.setId(Long.parseLong(id));
      for (Trait trait : toad.getTraits()) {
        trait.setToad(toad);
      }

      toadRepository.save(toad);

      return Optional.of(toad);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public BufferedImage getToadzImage(String id) {
    final var toadOpt = getToad(id);
    if (toadOpt.isEmpty()) {
      return null;
    }

    final Path path = Paths.get("img_cache/toadz/", id + ".png");
    if (path.toFile().exists()) {
      // Read
      try {
        ByteArrayInputStream bytes = new ByteArrayInputStream(Files.readAllBytes(path));
        return ImageIO.read(bytes);
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    }

    final var toad = toadOpt.get();
    String image = toad.getImage();

    webDriver.get(image);

    final var screenshot =
        new AShot().shootingStrategy(ShootingStrategies.simple()).takeScreenshot(webDriver);

    try {
      ImageIO.write(screenshot.getImage(), "png", path.toFile());
    } catch (IOException ex) {
      log.error("Unable to copy file", ex);
    }

    log.info("Retrieved toad: " + id);

    return screenshot.getImage();
  }
}
