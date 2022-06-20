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
import java.util.stream.Collectors;
import lombok.Getter;
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

  @Getter
  private String supplyMessage;

  @Scheduled(fixedDelay = 60000)
  public synchronized void updateSupplies() {
    // First, get the recipe formulas: which adventure produces which items at which rarity
    String jsonBody =
        "{\"query\":\"{\\n  craftingRecipeOutputs(first:1000){\\n    options {\\n      odds {\\n        baseOdds\\n      }\\n      item{\\n        name        \\n      }\\n    }\\n    recipe {\\n      id\\n      name\\n    }\\n  }\\n}\",\"variables\":null}";

    // This will start with zeroed out entries. We'll populate them in the 2nd query.
    Map<String, Integer> tools = new HashMap<>();
    Map<String, Integer> resources = new HashMap<>();
    long totalResources = 0;

    try {

      HttpClient httpClient = HttpClient.newHttpClient();

      // Next, get each item and its supply
      jsonBody =
          "{\"query\":\"{\\nitems(first:1000) {\\n    id\\n    name\\n    totalSupply\\n  category\\n}  \\n}\",\"variables\":null}";
      HttpRequest request =
          HttpRequest.newBuilder(
                  new URI("https://api.thegraph.com/subgraphs/name/vinnytreasure/toadstoolzsubgraph-prod"))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .header("Content-Type", "application/json")
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      JSONObject obj = new JSONObject(response.body()).getJSONObject("data");
      JSONArray items = obj.getJSONArray("items");
      for(int x = 0; x < items.length(); x++) {
        JSONObject item = items.getJSONObject(x);
        String category = item.getString("category");

        if(category.equalsIgnoreCase("tool")) {
          tools.put(item.getString("name"), item.getInt("totalSupply"));
        } else if(category.equalsIgnoreCase("resource")) {
          String name = item.getString("name");

          if(!name.equalsIgnoreCase("gold wood")) {
            resources.put(name, item.getInt("totalSupply"));
            totalResources += item.getLong("totalSupply");
          }
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

        sb.append(displayName).append(" - ").append(currentGlobal).append(" / ").append(maxGlobal).append(" (").append(String.format("%.2f", percentUsed)).append("% quests completed)\n");
      }

      // Add shop listings to held count for tools
      jsonBody = "{\"query\":\"\\n    query itemShopListings {\\n  itemShopListings {\\n    id\\n    status\\n    listingStart\\n    listingEnd\\n    totalQuantityAvailable\\n    quantityPurchased\\n    bugzCost\\n    item {\\n      id\\n      name\\n      description\\n      category\\n      readURI\\n    }\\n  }\\n}\\n    \"}";
      request =
          HttpRequest.newBuilder(
                  new URI("https://api.thegraph.com/subgraphs/name/vinnytreasure/toadstoolzsubgraph-prod"))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .header("Content-Type", "application/json")
              .build();
      response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      obj = new JSONObject(response.body()).getJSONObject("data");
      JSONArray itemShopListings = obj.getJSONArray("itemShopListings");

      for(int x = 0; x < itemShopListings.length(); x++) {
        JSONObject listing = itemShopListings.getJSONObject(x);
        JSONObject tool = listing.getJSONObject("item");
        String name = tool.getString("name");

        if(tools.containsKey(name)) {
          int totalQuantityAvailable = listing.getInt("totalQuantityAvailable");
          int quantityPurchased = listing.getInt("quantityPurchased");
          tools.put(name, tools.get(name) + totalQuantityAvailable - quantityPurchased);
        }
      }

      // Resource counts
      sb.append("\n").append("**Resources**").append("\n");
      Map<String, Integer> sortedResources = resources.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(
          Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldV, newV) -> oldV, LinkedHashMap::new));
      for(final var entry : sortedResources.entrySet()) {
        float percentOfRare = entry.getValue() == 0 ? 0.0f : ((float)entry.getValue() / (float)totalResources) * 100.0f;

        sb.append(entry.getKey()).append(" - ")
            .append(entry.getValue())
            .append(" (")
            .append(String.format("%.2f", percentOfRare)).append("%)")
            .append("\n");
      }

      // Now include axe output
      sb.append("\n").append("**Tools**\n");
      Map<String, Integer> sortedTools = tools.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(
          Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldV, newV) -> oldV, LinkedHashMap::new));

      for(final var entry : sortedTools.entrySet()) {
        sb.append(entry.getKey()).append(" - ").append(entry.getValue()).append("\n");
      }

      supplyMessage = sb.toString();
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
