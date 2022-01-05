package com.cureforoptimism.toadzbot.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Croak {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  Long discordUserId;

  String discordId;
}
