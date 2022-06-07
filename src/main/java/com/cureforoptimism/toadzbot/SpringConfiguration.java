package com.cureforoptimism.toadzbot;

import static com.cureforoptimism.toadzbot.Constants.TOADSTOOLZ_CONTRACT_ID;

import com.cureforoptimism.toadzbot.service.TokenService;
import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;
import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.signature.TwitterCredentials;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.web3j.contracts.eip721.generated.ERC721Enumerable;
import org.web3j.contracts.eip721.generated.ERC721Metadata;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

@Slf4j
@Configuration
@EnableScheduling
@EnableTransactionManagement
@RequiredArgsConstructor
public class SpringConfiguration {
  final TokenService tokenService;

  @Bean
  public CoinGeckoApiClient coinGeckoApiClient() {
    return new CoinGeckoApiClientImpl();
  }

  @Bean
  public Web3j web3j() {
    return Web3j.build(new HttpService("https://arb1.arbitrum.io/rpc"));
  }

  @Bean
  public ERC721Enumerable erc721Enumerable() {
    ContractGasProvider contractGasProvider = new DefaultGasProvider();

    try {
      Credentials dummyCredentials = Credentials.create(Keys.createEcKeyPair());
      return ERC721Enumerable.load(
          TOADSTOOLZ_CONTRACT_ID, web3j(), dummyCredentials, contractGasProvider);
    } catch (InvalidAlgorithmParameterException
        | NoSuchAlgorithmException
        | NoSuchProviderException ex) {
      log.error("FATAL: Unable to create ERC721Enumerable");
    }

    return null;
  }

  @Bean
  public ERC721Metadata erc721Metadata() {
    ContractGasProvider contractGasProvider = new DefaultGasProvider();

    try {
      Credentials dummyCredentials = Credentials.create(Keys.createEcKeyPair());
      return ERC721Metadata.load(
          TOADSTOOLZ_CONTRACT_ID, web3j(), dummyCredentials, contractGasProvider);
    } catch (InvalidAlgorithmParameterException
        | NoSuchAlgorithmException
        | NoSuchProviderException ex) {
      log.error("FATAL: Unable to create ERC721Metadata");
    }

    return null;
  }

  @Bean
  public TwitterClient twitterClient() {
    return new TwitterClient(
        TwitterCredentials.builder()
            .accessToken(tokenService.getTwitterApiToken())
            .accessTokenSecret(tokenService.getTwitterApiTokenSecret())
            .bearerToken(tokenService.getTwitterApiBearerToken())
            .apiKey(tokenService.getTwitterApiKey())
            .apiSecretKey(tokenService.getTwitterApiSecret())
            .build());
  }
}
