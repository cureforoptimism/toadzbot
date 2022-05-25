package com.cureforoptimism.toadzbot.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Slf4j
@PropertySource({"classpath:application.yml"})
@Service
public class TokenService {
  @Value("${tokens.file}")
  private String file;

  public String getDiscordToken() {
    return System.getenv("PROD") == null
        ? getPropertyValue("discord_access_token_dev")
        : getPropertyValue("discord_access_token");
  }

  private String getPropertyValue(String key) {
    ClassPathResource classPathResource = new ClassPathResource(file);

    try (InputStream input = classPathResource.getInputStream()) {
      Properties properties = new Properties();
      properties.load(input);
      return properties.get(key).toString();
    } catch (IOException ex) {
      log.error("Unable to retrieve token", ex);
    }

    return null;
  }
}
