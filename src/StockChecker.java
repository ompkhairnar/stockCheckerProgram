import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONObject;
import java.util.Scanner;

public class StockChecker {
    static void method(int choice, String[] stockSymbols, String API_KEY, HttpClient client, DateTimeFormatter formatter, ArrayList<String> goodstocks, ArrayList<String> badstocks, String timePeriod, String Interval) throws IOException, InterruptedException {
        String URL;
        for (int y = 0; y < stockSymbols.length; y++) {
            double movingpd = 0;
            double currentPrice;
            if (choice == 4) {
                URL = "https://www.alphavantage.co/query?function=" + timePeriod + "&symbol=" + stockSymbols[y] + "&apikey=" + API_KEY;
            } else {
                URL = "https://www.alphavantage.co/query?function=" + timePeriod + "&symbol=" + stockSymbols[y] + "&interval=" + Interval + "&apikey=" + API_KEY;
            }

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(URL)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String responseBody = response.body();
            System.out.println("Response for " + stockSymbols[y] + ": " + responseBody);  // For debugging

            JSONObject jsonResponse = new JSONObject(responseBody);

            // Check if response contains "Time Series (Daily)" data
            if (jsonResponse.has("Time Series (Daily)")) {
                JSONObject timeSeries = jsonResponse.getJSONObject("Time Series (Daily)");

                // Get the latest available date in the time series (which may not be today's date)
                Iterator<String> keys = timeSeries.keys();
                if (keys.hasNext()) {
                    String latestDate = keys.next();  // The most recent available date
                    JSONObject latestData = timeSeries.getJSONObject(latestDate);
                    currentPrice = latestData.getDouble("4. close");

                    // Calculate moving average for the past 4 days
                    for (int x = 1; x <= 4; x++) {
                        LocalDate targetDate = LocalDate.parse(latestDate).minusDays(x);
                        String formattedDate = targetDate.format(formatter);

                        if (timeSeries.has(formattedDate)) {
                            JSONObject dailyData = timeSeries.getJSONObject(formattedDate);
                            movingpd += dailyData.getDouble("4. close");
                        }
                    }

                    // Calculate average over 4 days
                    double movingAverage = movingpd / 4;

                    // Add stock to either goodstocks or badstocks list
                    if (currentPrice >= movingAverage) {
                        goodstocks.add(stockSymbols[y]);
                    } else {
                        badstocks.add(stockSymbols[y]);
                    }
                }
                else {
                    System.out.println("No data available for " + stockSymbols[y]);
                }

            }
            else if (jsonResponse.has("Time Series (1min)")) {
                JSONObject timeSeries = jsonResponse.getJSONObject("Time Series (1min)");
                Iterator<String> keys = timeSeries.keys();

                if (keys.hasNext()) {
                    String latestTime = keys.next();  // The most recent time
                    JSONObject latestData = timeSeries.getJSONObject(latestTime);
                    currentPrice = latestData.getDouble("4. close");

                    // Calculate moving average for the past 25 minutes (5 intervals of 5 minutes each)
                    for (int x = 0; x < 5; x++) {
                        // Parse the current time and subtract intervals of 5 minutes each
                        LocalDateTime targetTime = LocalDateTime.parse(latestTime, formatter).minusMinutes(x * 5);
                        String formattedTime = targetTime.format(formatter);

                        if (timeSeries.has(formattedTime)) {
                            JSONObject intervalData = timeSeries.getJSONObject(formattedTime);
                            movingpd += intervalData.getDouble("4. close");
                        }
                    }

                    // Calculate average over 25 minutes (5 intervals)
                    double movingAverage = movingpd / 5;

                    // Add stock to either goodstocks or badstocks list
                    if (currentPrice >= movingAverage) {
                        goodstocks.add(stockSymbols[y]);
                    } else {
                        badstocks.add(stockSymbols[y]);
                    }
                }
                else {
                    System.out.println("No data available for " + stockSymbols[y]);
                }
            }
            else if (jsonResponse.has("Time Series (5min)")) {
                JSONObject timeSeries = jsonResponse.getJSONObject("Time Series (5min)");

                // Get the latest available date in the time series (may not be the current time)
                Iterator<String> keys = timeSeries.keys();
                if (keys.hasNext()) {
                    String latestTime = keys.next();  // Most recent available time
                    JSONObject latestData = timeSeries.getJSONObject(latestTime);
                    currentPrice = latestData.getDouble("4. close");

                    // Use the correct formatter for "yyyy-MM-dd HH:mm:ss"
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                    // Calculate moving average for the past 25 minutes (5 intervals)
                    for (int x = 0; x < 5; x++) {
                        LocalDateTime targetTime = LocalDateTime.parse(latestTime, dateTimeFormatter).minusMinutes(5 * x);
                        String formattedTime = targetTime.format(dateTimeFormatter);

                        if (timeSeries.has(formattedTime)) {
                            JSONObject intervalData = timeSeries.getJSONObject(formattedTime);
                            movingpd += intervalData.getDouble("4. close");
                        }
                    }

                    // Calculate average over 5 intervals (25 minutes)
                    double movingAverage = movingpd / 5;

                    // Add stock to either goodstocks or badstocks list based on comparison
                    if (currentPrice >= movingAverage) {
                        goodstocks.add(stockSymbols[y]);
                    } else {
                        badstocks.add(stockSymbols[y]);
                    }

                }
                else {
                    System.out.println("No time series data available for " + stockSymbols[y]);
                }


            }
            else if (jsonResponse.has("Time Series (60min)")) {
                JSONObject timeSeries = jsonResponse.getJSONObject("Time Series (60min)");
                Iterator<String> keys = timeSeries.keys();

                // Get the most recent time in the series
                if (keys.hasNext()) {
                    String latestTime = keys.next();  // The most recent time
                    JSONObject latestData = timeSeries.getJSONObject(latestTime);
                    currentPrice = latestData.getDouble("4. close");

                    // Calculate moving average for the past 5 hours (5 intervals of 60 minutes each)
                    for (int x = 0; x < 5; x++) {
                        // Parse the current time and subtract intervals of 60 minutes each
                        LocalDateTime targetTime = LocalDateTime.parse(latestTime, formatter).minusHours(x);
                        String formattedTime = targetTime.format(formatter);

                        if (timeSeries.has(formattedTime)) {
                            JSONObject intervalData = timeSeries.getJSONObject(formattedTime);
                            movingpd += intervalData.getDouble("4. close");
                        }
                    }

                    // Calculate average over 5 hours (300 minutes)
                    double movingAverage = movingpd / 5;

                    // Add stock to either goodstocks or badstocks list
                    if (currentPrice >= movingAverage) {
                        goodstocks.add(stockSymbols[y]);
                    } else {
                        badstocks.add(stockSymbols[y]);
                    }
                } else {
                    System.out.println("No data available for " + stockSymbols[y]);
                }
            }



        }
        System.out.println("Good stocks to consider:");
        for (String stock : goodstocks) {
            System.out.println(stock);
        }

        System.out.println("\nBad stocks to avoid:");
        for (String stock : badstocks) {
            System.out.println(stock);
        }
    }
        public static void main (String[]args) throws IOException, InterruptedException {
            String API_KEY = "JMEIQOIZ2DV3DL0F";
            ArrayList<String> goodstocks = new ArrayList<>();
            ArrayList<String> badstocks = new ArrayList<>();
            String[] stockSymbols = {
                    "AAPL", "MSFT", "GOOGL", "AMZN", "NVDA",
                    "JPM", "GS", "BAC", "WFC", "C",
                    "JNJ", "PFE", "UNH", "MRK", "ABBV",
                    "PG", "KO", "PEP", "UL", "CL",
                    "XOM", "CVX", "COP", "SLB", "OXY"
            };

            HttpClient client = HttpClient.newHttpClient();

            String timePeriod;

            System.out.println("Enter time period for moving average " +
                    "\n1. 5 minute moving average" +
                    "\n2. 25 minute moving average" +
                    "\n3. 5 hour moving average" +
                    "\n4. 5 day moving average:   ");
            Scanner sc = new Scanner(System.in);
            int choice = sc.nextInt();
            DateTimeFormatter formatter;
            switch (choice) {
                case 1:
                    formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    timePeriod = "TIME_SERIES_INTRADAY";
                    String Interval = "1min";
                    method(choice, stockSymbols, API_KEY, client, formatter, goodstocks, badstocks, timePeriod, Interval);
                    break;
                case 2:
                     formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    timePeriod =  "TIME_SERIES_INTRADAY";
                     String Interval2 = "5min";
                    method(choice, stockSymbols, API_KEY, client, formatter, goodstocks, badstocks, timePeriod, Interval2);
                    break;
                case 3:
                     formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    timePeriod =  "TIME_SERIES_INTRADAY";
                    String Interval3 = "60min";
                    method(choice, stockSymbols, API_KEY, client, formatter, goodstocks, badstocks, timePeriod, Interval3);
                    break;
                case 4:
                     formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    timePeriod =  "TIME_SERIES_DAILY";
                    String Interval4 = "notneeded";
                    method(choice, stockSymbols, API_KEY, client, formatter, goodstocks, badstocks, timePeriod, Interval4);
                    break;
                default:
                        break;

            }


        }

}
