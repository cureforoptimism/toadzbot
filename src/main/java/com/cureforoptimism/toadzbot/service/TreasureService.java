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

  @Getter private String supplyMessage;

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
                  new URI(
                      "https://api.thegraph.com/subgraphs/name/vinnytreasure/toadstoolzsubgraph-prod"))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .header("Content-Type", "application/json")
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      JSONObject obj = new JSONObject(response.body()).getJSONObject("data");
      JSONArray items = obj.getJSONArray("items");
      for (int x = 0; x < items.length(); x++) {
        JSONObject item = items.getJSONObject(x);
        String category = item.getString("category");

        if (category.equalsIgnoreCase("tool")) {
          tools.put(item.getString("name"), item.getInt("totalSupply"));
        } else if (category.equalsIgnoreCase("resource")) {
          String name = item.getString("name");

          resources.put(name, item.getInt("totalSupply"));
          totalResources += item.getLong("totalSupply");
        }
      }

      jsonBody =
          "{\"query\":\"{\\n  craftingRecipes(first:1000,where:{status:VALID,recipeType:ADVENTURE}) {\\n    displayName\\n    maxCraftsGlobally\\n    currentCraftsGlobally\\n  }\\n}\",\"variables\":null}";

      request =
          HttpRequest.newBuilder(
                  new URI(
                      "https://api.thegraph.com/subgraphs/name/vinnytreasure/toadstoolzsubgraph-prod"))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .header("Content-Type", "application/json")
              .build();
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      obj = new JSONObject(response.body()).getJSONObject("data");

      JSONArray locations = obj.getJSONArray("craftingRecipes");

      StringBuilder sb = new StringBuilder();
      sb.append("**Adventures**").append("\n");

      for (int x = 0; x < locations.length(); x++) {
        JSONObject location = locations.getJSONObject(x);
        String displayName = location.getString("displayName");
        int maxGlobal = location.getInt("maxCraftsGlobally");
        int currentGlobal = location.getInt("currentCraftsGlobally");
        float percentUsed =
            currentGlobal == 0 ? 0.0f : ((float) currentGlobal / (float) maxGlobal) * 100.0f;

        sb.append(displayName)
            .append(" - ")
            .append(currentGlobal)
            .append(" / ")
            .append(maxGlobal)
            .append(" (")
            .append(String.format("%.2f", percentUsed))
            .append("% quests completed)\n");
      }

      // Add shop listings to held count for tools
      jsonBody =
          "{\"query\":\"\\n    query itemShopListings {\\n  itemShopListings {\\n    id\\n    status\\n    listingStart\\n    listingEnd\\n    totalQuantityAvailable\\n    quantityPurchased\\n    bugzCost\\n    item {\\n      id\\n      name\\n      description\\n      category\\n      readURI\\n    }\\n  }\\n}\\n    \"}";
      request =
          HttpRequest.newBuilder(
                  new URI(
                      "https://api.thegraph.com/subgraphs/name/vinnytreasure/toadstoolzsubgraph-prod"))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .header("Content-Type", "application/json")
              .build();
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      obj = new JSONObject(response.body()).getJSONObject("data");
      JSONArray itemShopListings = obj.getJSONArray("itemShopListings");

      for (int x = 0; x < itemShopListings.length(); x++) {
        JSONObject listing = itemShopListings.getJSONObject(x);
        JSONObject tool = listing.getJSONObject("item");
        String name = tool.getString("name");

        if (tools.containsKey(name)) {
          int totalQuantityAvailable = listing.getInt("totalQuantityAvailable");
          int quantityPurchased = listing.getInt("quantityPurchased");
          tools.put(name, tools.get(name) + totalQuantityAvailable - quantityPurchased);
        }
      }

      // Resource counts
      sb.append("\n").append("**Resources**").append("\n");
      Map<String, Integer> sortedResources =
          resources.entrySet().stream()
              .sorted(Map.Entry.comparingByValue())
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      Map.Entry::getValue,
                      (oldV, newV) -> oldV,
                      LinkedHashMap::new));
      for (final var entry : sortedResources.entrySet()) {
        float percentOfRare =
            entry.getValue() == 0
                ? 0.0f
                : ((float) entry.getValue() / (float) totalResources) * 100.0f;

        sb.append(entry.getKey())
            .append(" - ")
            .append(entry.getValue())
            .append(" (")
            .append(String.format("%.2f", percentOfRare))
            .append("%)")
            .append("\n");
      }

      // Now include axe output
      sb.append("\n").append("**Tools**\n");
      Map<String, Integer> sortedTools =
          tools.entrySet().stream()
              .sorted(Map.Entry.comparingByValue())
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      Map.Entry::getValue,
                      (oldV, newV) -> oldV,
                      LinkedHashMap::new));

      for (final var entry : sortedTools.entrySet()) {
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
        "{\"query\":\"query getActivity($first: Int!, $skip: Int, $includeListings: Boolean!, $includeSales: Boolean!, $includeBids: Boolean!, $listingFilter: Listing_filter, $listingOrderBy: Listing_orderBy, $bidFilter: Bid_filter, $bidOrderBy: Bid_orderBy, $saleFilter: Sale_filter, $saleOrderBy: Sale_orderBy, $orderDirection: OrderDirection) {\\n  listings(\\n    first: $first\\n    where: $listingFilter\\n    orderBy: $listingOrderBy\\n    orderDirection: $orderDirection\\n    skip: $skip\\n  ) @include(if: $includeListings) {\\n    ...ListingFields\\n  }\\n  bids(\\n    first: $first\\n    where: $bidFilter\\n    orderBy: $bidOrderBy\\n    orderDirection: $orderDirection\\n    skip: $skip\\n  ) @include(if: $includeBids) {\\n    ...BidFields\\n  }\\n  sales(\\n    first: $first\\n    where: $saleFilter\\n    orderBy: $saleOrderBy\\n    orderDirection: $orderDirection\\n    skip: $skip\\n  ) @include(if: $includeSales) {\\n    ...SaleFields\\n  }\\n}\\n\\nfragment ListingFields on Listing {\\n  timestamp\\n  id\\n  pricePerItem\\n  quantity\\n  seller {\\n    id\\n  }\\n  token {\\n    id\\n    tokenId\\n  }\\n  collection {\\n    id\\n  }\\n  currency {\\n    id\\n  }\\n  status\\n  expiresAt\\n}\\n\\nfragment BidFields on Bid {\\n  timestamp\\n  id\\n  pricePerItem\\n  quantity\\n  token {\\n    id\\n    tokenId\\n  }\\n  collection {\\n    id\\n  }\\n  currency {\\n    id\\n  }\\n  buyer {\\n    id\\n  }\\n  status\\n  expiresAt\\n  bidType\\n}\\n\\nfragment SaleFields on Sale {\\n  timestamp\\n  id\\n  pricePerItem\\n  quantity\\n  type\\n  seller {\\n    id\\n  }\\n  buyer {\\n    id\\n  }\\n  token {\\n    id\\n    tokenId\\n  }\\n  collection {\\n    id\\n  }\\n  currency {\\n    id\\n  }\\n}\",\"variables\":{\"skip\":0,\"first\":20,\"listingOrderBy\":\"timestamp\",\"saleOrderBy\":\"timestamp\",\"bidOrderBy\":\"timestamp\",\"listingFilter\":{\"collection\":\"0x09cae384c6626102abe47ff50588a1dbe8432174\",\"status_in\":[\"ACTIVE\"]},\"bidFilter\":{\"collection\":\"0x09cae384c6626102abe47ff50588a1dbe8432174\",\"status_in\":[\"ACTIVE\",\"EXPIRED\"]},\"saleFilter\":{\"collection\":\"0x09cae384c6626102abe47ff50588a1dbe8432174\"},\"orderDirection\":\"desc\",\"includeListings\":true,\"includeSales\":true,\"includeBids\":true},\"operationName\":\"getActivity\"}";

    try {
      HttpClient httpClient = HttpClient.newHttpClient();
      HttpRequest request =
          HttpRequest.newBuilder(
                  new URI(
                      "https://api.thegraph.com/subgraphs/name/vinnytreasure/treasuremarketplace-fast-prod"))
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .header("Content-Type", "application/json")
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      JSONObject obj = new JSONObject(response.body()).getJSONObject("data");
      JSONArray listings = obj.getJSONArray("sales");

      MathContext mc = new MathContext(10, RoundingMode.HALF_UP);

      for (int x = 0; x < listings.length(); x++) {
        final var listing = listings.getJSONObject(x);
        if(listing.getString("type").equalsIgnoreCase("bid")) {
          continue;
        }

        String id = listing.getString("id");
        String transactionId = "https://arbiscan.io/tx/" + id.split("-")[3];
        int tokenId = listing.getJSONObject("token").getInt("tokenId");

        if (!toadzSaleRepository.existsByTxAndTokenId(transactionId, tokenId)) {
          BigDecimal pricePerItem = new BigDecimal(listing.getBigInteger("pricePerItem"), 18, mc);
          Date blockTimeStamp = new Date(listing.getLong("timestamp") * 1000);

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
