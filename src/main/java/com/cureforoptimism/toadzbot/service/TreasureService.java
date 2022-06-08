package com.cureforoptimism.toadzbot.service;

import com.cureforoptimism.toadzbot.domain.ToadzSale;
import com.cureforoptimism.toadzbot.repository.ToadzSaleRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TreasureService {
  private final ToadzSaleRepository toadzSaleRepository;


  private final Map<String, TreeMap<Integer, String>> adventureToRecipes = new HashMap<>();

  @Scheduled(fixedDelay = 60000)
  public synchronized void updateSupplies() {
    // First, get the recipe formulas: which adventure produces which items at which rarity
    String jsonBody =
        "{\"query\":\"{\\n  craftingRecipeOutputs(first:1000){\\n    options {\\n      odds {\\n        baseOdds\\n      }\\n      item{\\n        name        \\n      }\\n    }\\n    recipe {\\n      id\\n      name\\n    }\\n  }\\n}\",\"variables\":null}";

    // This will start with zeroed out entries. We'll populate them in the 2nd query.
    Map<String, Integer> tools = new HashMap<>();
    Map<String, Integer> resources = new HashMap<>();

    try {

      HttpClient httpClient = HttpClient.newHttpClient();

      HttpRequest request =
          HttpRequest.newBuilder(
                  new URI("https://api.thegraph.com/subgraphs/name/vinnytreasure/toadstoolzsubgraph-prod"))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .header("Content-Type", "application/json")
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      JSONObject obj = new JSONObject(response.body()).getJSONObject("data");
      JSONArray locationRecipes = obj.getJSONArray("craftingRecipeOutputs");
      for(int x = 0; x < locationRecipes.length(); x++) {
        JSONObject locationRecipe = locationRecipes.getJSONObject(x);
        JSONObject recipe = locationRecipe.getJSONObject("recipe");
        String name = recipe.getString("name");

        if(!adventureToRecipes.containsKey(name)) {
          adventureToRecipes.put(name, new TreeMap<>());
        }

        final var mapEntry = adventureToRecipes.get(name);

        JSONArray options = locationRecipe.getJSONArray("options");
        for(int y = 0; y < options.length(); y++) {
          JSONObject option = options.getJSONObject(y);
          int baseOdds = option.getJSONObject("odds").getInt("baseOdds");
          String itemName = option.getJSONObject("item").getString("name");

          mapEntry.put(baseOdds, itemName);
        }

        adventureToRecipes.put(name, mapEntry);
      }

      // Next, get each item and its supply
      jsonBody =
          "{\"query\":\"{\\nitems(first:1000) {\\n    id\\n    name\\n    totalSupply\\n  category\\n}  \\n}\",\"variables\":null}";
      request =
          HttpRequest.newBuilder(
                  new URI("https://api.thegraph.com/subgraphs/name/vinnytreasure/toadstoolzsubgraph-prod"))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .header("Content-Type", "application/json")
              .build();
      response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      obj = new JSONObject(response.body()).getJSONObject("data");
      JSONArray items = obj.getJSONArray("items");
      for(int x = 0; x < items.length(); x++) {
        JSONObject item = items.getJSONObject(x);
        String category = item.getString("category");

        if(category.equalsIgnoreCase("tool")) {
          tools.put(item.getString("name"), item.getInt("totalSupply"));
        } else if(category.equalsIgnoreCase("resource")) {
          resources.put(item.getString("name"), item.getInt("totalSupply"));
        }
      }

      jsonBody =
          "{\"query\":\"{\\n  craftingRecipes(first:1000,where:{status:VALID,recipeType:ADVENTURE}) {\\n    displayName\\n    maxCraftsGlobally\\n    currentCraftsGlobally\\n  }\\n}\",\"variables\":null}";

      request =
          HttpRequest.newBuilder(
                  new URI("https://api.thegraph.com/subgraphs/name/vinnytreasure/toadstoolzsubgraph-prod"))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .header("Content-Type", "application/json")
              .build();
      response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      obj = new JSONObject(response.body()).getJSONObject("data");

      JSONArray locations = obj.getJSONArray("craftingRecipes");

      StringBuilder sb = new StringBuilder();
      sb.append("**Adventures**").append("\n");

      for(int x = 0; x < locations.length(); x++) {
        JSONObject location = locations.getJSONObject(x);
        String displayName = location.getString("displayName");
        int maxGlobal = location.getInt("maxCraftsGlobally");
        int currentGlobal = location.getInt("currentCraftsGlobally");
        float percentUsed = currentGlobal == 0 ? 0.0f : ((float)currentGlobal / (float)maxGlobal) * 100.0f;

        final var rareResource = adventureToRecipes.get("Adventure: " + displayName).firstEntry().getValue();
        final var totalResources = resources.get(rareResource);
        float percentRareProduced = currentGlobal == 0 ? 0.0f : ((float)totalResources / (float)currentGlobal) * 100.0f;

        sb.append("**").append(displayName).append("**\n")
            .append("Rare resource: ").append(rareResource).append(" - ").append(resources.get(rareResource)).append(" / ").append(currentGlobal).append(" crafted (").append(String.format("%.00f", percentRareProduced)).append("% rare produced)\n")
            .append("Adventures quested: ").append(currentGlobal).append(" / ")
            .append(maxGlobal)
            .append(" (").append(String.format("%.00f", percentUsed)).append("%)")
            .append("\n");
      }

      // Now include axe output
      sb.append("\n").append("**Tools**\n");
      Map<String, Integer> sortedTools = tools.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(
          Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldV, newV) -> oldV, LinkedHashMap::new));

      for(final var entry : sortedTools.entrySet()) {
        sb.append(entry.getKey()).append(" - ").append(entry.getValue()).append("\n");
      }

      log.info(sb.toString());
    } catch (URISyntaxException | IOException | InterruptedException ex) {
      log.error("error updating supplies: " + ex);
    }
  }

  @Scheduled(fixedDelay = 60000)
  public synchronized void updateLatestSales() {
    String jsonBody =
        "{\"query\":\"query getActivity($id: String!, $orderBy: Listing_orderBy!) {\\n  listings(\\n    where: {status: Sold, collection: $id}\\n    orderBy: $orderBy\\n    orderDirection: desc\\n  ) {\\n    ...ListingFields\\n  }\\n}\\n\\nfragment ListingFields on Listing {\\n  blockTimestamp\\n  buyer {\\n    id\\n  }\\n  id\\n  pricePerItem\\n  quantity\\n  seller {\\n    id\\n  }\\n  token {\\n    id\\n    tokenId\\n  }\\n  collection {\\n    id\\n  }\\n  transactionLink\\n}\",\"variables\":{\"id\":\"0x09cae384c6626102abe47ff50588a1dbe8432174\",\"orderBy\":\"blockTimestamp\"},\"operationName\":\"getActivity\"}";
    try {
      HttpClient httpClient = HttpClient.newHttpClient();
      HttpRequest request =
          HttpRequest.newBuilder(
                  new URI("https://api.thegraph.com/subgraphs/name/treasureproject/marketplace"))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .header("Content-Type", "application/json")
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      JSONObject obj = new JSONObject(response.body()).getJSONObject("data");
      JSONArray listings = obj.getJSONArray("listings");

      MathContext mc = new MathContext(10, RoundingMode.HALF_UP);

      for (int x = 0; x < listings.length(); x++) {
        final var listing = listings.getJSONObject(x);

        String transactionId = listing.getString("transactionLink");
        int tokenId = listing.getJSONObject("token").getInt("tokenId");

        if (!toadzSaleRepository.existsByTxAndTokenId(transactionId, tokenId)) {
          BigDecimal pricePerItem = new BigDecimal(listing.getBigInteger("pricePerItem"), 18, mc);
          Date blockTimeStamp = new Date(listing.getLong("blockTimestamp") * 1000);

          toadzSaleRepository.save(
              ToadzSale.builder()
                  .tx(transactionId)
                  .tokenId(tokenId)
                  .salePrice(pricePerItem)
                  .blockTimestamp(blockTimeStamp)
                  .posted(false)
                  .build());
        }
      }
    } catch (Exception ex) {
      log.error("Exception updating latest sales", ex);
    }
  }
}
