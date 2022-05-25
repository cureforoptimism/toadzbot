package com.cureforoptimism.toadzbot.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trait {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Getter
  @JsonProperty("trait_type")
  String traitType;

  @Getter String value;

  @ManyToOne
  @Setter
  @JoinColumn(name = "toad_id")
  Toad toad;
}
