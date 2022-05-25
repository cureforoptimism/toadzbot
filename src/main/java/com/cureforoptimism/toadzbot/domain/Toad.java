package com.cureforoptimism.toadzbot.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Toad {
  @Getter @Setter @Id Long id;

  @Setter @Getter public String name;
  @Getter @Lob public String image;

  @Getter
  @OneToMany(fetch = FetchType.EAGER, mappedBy = "toad", cascade = CascadeType.ALL)
  @JsonProperty("attributes")
  Set<Trait> traits;
}
