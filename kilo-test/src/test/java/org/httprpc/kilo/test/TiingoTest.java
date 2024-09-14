/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.kilo.test;

import org.httprpc.kilo.WebServiceProxy;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;

import static org.httprpc.kilo.util.Collections.*;

public class TiingoTest {
    private static final URI baseURI = URI.create("https://api.tiingo.com");

    public static void main(String[] args) throws IOException {
        var token = System.getProperty("token");

        if (token == null) {
            System.out.println("Token is required.");
            return;
        }

        if (args.length < 1) {
            System.out.println("Ticker is required.");
            return;
        }

        var ticker = args[0];

        // Create service proxy
        var tiingoServiceProxy = WebServiceProxy.of(TiingoServiceProxy.class, baseURI, mapOf(
            entry("Authorization", String.format("Token %s", token))
        ));

        // Get asset details
        var asset = tiingoServiceProxy.getAsset(ticker);

        System.out.println(asset.getTicker());
        System.out.println(asset.getName());
        System.out.println(asset.getExchangeCode());
        System.out.println(asset.getDescription());

        System.out.printf("%s - %s\n", asset.getStartDate(), asset.getEndDate());

        // Get historical pricing
        var endDate = LocalDate.now();
        var startDate = endDate.minusDays(7);

        var historicalPricing = tiingoServiceProxy.getHistoricalPricing(ticker, startDate, endDate);

        for (var assetPricing : historicalPricing) {
            System.out.printf("%s open = %.2f, high = %.2f, low = %.2f, close = %.2f, volume = %d\n",
                assetPricing.getDate(),
                assetPricing.getOpen(),
                assetPricing.getHigh(),
                assetPricing.getLow(),
                assetPricing.getClose(),
                assetPricing.getVolume());
        }
    }
}
