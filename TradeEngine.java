import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Aurora Trade Engine - Simulates a simple stock trading engine.
 * Supports placing buy/sell orders, printing market ladders, tracking PnL,
 * and running in a real-time simulation mode.
 */
public class TradeEngine {

    // ====== Static variables ======
    static Scanner scanner = new Scanner(System.in);
    static ArrayList<OrderStock> buy_stock_list = new ArrayList<>();
    static ArrayList<OrderStock> sell_stock_list = new ArrayList<>();
    static HashMap<String, Double> pnlMap = new HashMap<>();
    static int nextId = 1;

    // Metrics tracking
    static long totalTrades = 0;
    static long totalVolume = 0;
    static double totalNotional = 0.0;

    // Real-time order queue
    static BlockingQueue<OrderStock> orderQueue = new LinkedBlockingQueue<>();
    static volatile boolean running = true;

    /**
     * Main entry point for the trade engine.
     * Displays menu options and handles user input.
     */
    public static void main(String[] args) {
        while (true) {
            System.out.println();
            System.out.println("=== Aurora Trade Engine ===");
            System.out.println("1. Enter 1 To Place Order");
            System.out.println("2. Enter 2 To Show Market Ladder");
            System.out.println("3. Enter 3 To Show PnL and Metrics");
            System.out.println("4. Enter 4 To Run Realtime Mode");
            System.out.println("5. Enter 5 To Exit");

            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 1) {
                System.out.print("Buy or Sell ");
                String byse = scanner.nextLine();

                if (byse.equalsIgnoreCase("buy")) {
                    buy_stock(byse);
                } else if (byse.equalsIgnoreCase("sell")) {
                    sell_stock(byse);
                } else {
                    System.out.println("Invalid choice");
                }

            } else if (choice == 2) {
                printMarketLadder(10);
            } else if (choice == 3) {
                printPnL();
                printMetrics();
            } else if (choice == 4) {
                runRealtimeMode();
            } else if (choice == 5) {
                System.out.println("Exiting");
                break;
            } else {
                System.out.println("Invalid choice");
            }
        }
        scanner.close();
    }

    /**
     * Runs the engine in real-time mode with three threads:
     * 1. Engine Thread - Matches buy/sell orders from the queue
     * 2. Feeder Thread - Generates random fake orders
     * 3. UI Thread - Refreshes market ladder and metrics
     */
    private static void runRealtimeMode() {
        running = true;

        // Thread 1: Engine (matches orders from the queue)
        Thread engineThread = new Thread(() -> {
            while (running) {
                try {
                    // Wait for and take the next order from the queue
                    OrderStock order = orderQueue.take();
                    if (order.getType().equalsIgnoreCase("buy")) {
                        buy_stock_from_queue(order);
                    } else {
                        sell_stock_from_queue(order);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // Thread 2: Feeder (generates random orders)
        Thread feederThread = new Thread(() -> {
            Random rand = new Random();
            String[] companies = {"AAPL", "MSFT", "TSLA", "GOOG"};
            String[] traders = {"Alice", "Bob", "Charlie", "Dana"};

            while (running) {
                try {
                    String trader = traders[rand.nextInt(traders.length)];
                    String type = rand.nextBoolean() ? "buy" : "sell";
                    String company = companies[rand.nextInt(companies.length)];
                    int qty = rand.nextInt(50) + 1;
                    double price = 90 + rand.nextInt(21); // Price between 90–110

                    // Create and queue the order
                    OrderStock order = new OrderStock(trader, nextId++, type, company, qty, price);
                    orderQueue.put(order);

                    Thread.sleep(300); // Delay between new orders
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // Thread 3: UI (updates the display)
        Thread uiThread = new Thread(() -> {
            while (running) {
                // Clear terminal screen
                System.out.print("\033[H\033[2J");
                System.out.flush();

                // Show market ladder and metrics
                printMarketLadder(10);
                printMetrics();

                try {
                    Thread.sleep(500); // Refresh every 0.5 sec
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // Start all threads
        engineThread.start();
        feederThread.start();
        uiThread.start();

        // Wait for ENTER to stop
        System.out.println("Press ENTER to stop realtime mode...");
        scanner.nextLine();
        running = false;
    }

    /**
     * Matches a buy order against existing sell orders in the order book.
     * @param buy The buy order to process
     */
    private static void buy_stock_from_queue(OrderStock buy) {
        int i = 0;
        while (buy.getQuantity() > 0 && i < sell_stock_list.size()) {
            OrderStock sell = sell_stock_list.get(i);
            // Match if same company and sell price <= buy price
            if (sell.getCompany().equalsIgnoreCase(buy.getCompany())
                    && sell.getPrice() <= buy.getPrice()) {
                int tradeQty = Math.min(buy.getQuantity(), sell.getQuantity());
                double tradePrice = sell.getPrice();

                // Update quantities
                buy.setQuantity(buy.getQuantity() - tradeQty);
                sell.setQuantity(sell.getQuantity() - tradeQty);

                // Update PnL
                pnlMap.put(buy.getTrader(), pnlMap.getOrDefault(buy.getTrader(), 0.0) - (tradeQty * tradePrice));
                pnlMap.put(sell.getTrader(), pnlMap.getOrDefault(sell.getTrader(), 0.0) + (tradeQty * tradePrice));

                // Update metrics
                totalTrades++;
                totalVolume += tradeQty;
                totalNotional += tradeQty * tradePrice;

                // Remove sell order if fully matched
                if (sell.getQuantity() == 0) {
                    sell_stock_list.remove(i);
                    continue;
                }
            }
            i++;
        }
        if (buy.getQuantity() > 0) insertBuySorted(buy);
    }

    /**
     * Matches a sell order against existing buy orders in the order book.
     * @param sell The sell order to process
     */
    private static void sell_stock_from_queue(OrderStock sell) {
        int i = 0;
        while (sell.getQuantity() > 0 && i < buy_stock_list.size()) {
            OrderStock buy = buy_stock_list.get(i);
            // Match if same company and buy price >= sell price
            if (buy.getCompany().equalsIgnoreCase(sell.getCompany())
                    && buy.getPrice() >= sell.getPrice()) {
                int tradeQty = Math.min(sell.getQuantity(), buy.getQuantity());
                double tradePrice = buy.getPrice();

                // Update quantities
                sell.setQuantity(sell.getQuantity() - tradeQty);
                buy.setQuantity(buy.getQuantity() - tradeQty);

                // Update PnL
                pnlMap.put(buy.getTrader(), pnlMap.getOrDefault(buy.getTrader(), 0.0) - (tradeQty * tradePrice));
                pnlMap.put(sell.getTrader(), pnlMap.getOrDefault(sell.getTrader(), 0.0) + (tradeQty * tradePrice));

                // Update metrics
                totalTrades++;
                totalVolume += tradeQty;
                totalNotional += tradeQty * tradePrice;

                // Remove buy order if fully matched
                if (buy.getQuantity() == 0) {
                    buy_stock_list.remove(i);
                    continue;
                }
            }
            i++;
        }
        if (sell.getQuantity() > 0) insertSellSorted(sell);
    }

    /**
     * Handles manual buy order input from the user.
     * @param type The type of order ("buy")
     */
    public static void buy_stock(String type) {
        System.out.print("Enter Trader Name ");
        String trader_name = scanner.nextLine();
        System.out.print("Enter Company ");
        String company = scanner.nextLine();
        System.out.print("Enter Quantity ");
        int quantity = scanner.nextInt();
        System.out.print("Enter Price $ ");
        double price = scanner.nextDouble();
        scanner.nextLine();

        OrderStock buy = new OrderStock(trader_name, nextId++, type, company, quantity, price);
        buy_stock_from_queue(buy);
    }

    /**
     * Handles manual sell order input from the user.
     * @param type The type of order ("sell")
     */
    public static void sell_stock(String type) {
        System.out.print("Enter Trader Name ");
        String trader_name = scanner.nextLine();
        System.out.print("Enter Company ");
        String company = scanner.nextLine();
        System.out.print("Enter Quantity ");
        int quantity = scanner.nextInt();
        System.out.print("Enter Price $ ");
        double price = scanner.nextDouble();
        scanner.nextLine();

        OrderStock sell = new OrderStock(trader_name, nextId++, type, company, quantity, price);
        sell_stock_from_queue(sell);
    }

    /**
     * Inserts a sell order in price-sorted order.
     * @param sell The sell order to insert
     */
    private static void insertSellSorted(OrderStock sell) {
        int idx = 0;
        while (idx < sell_stock_list.size()
                && sell_stock_list.get(idx).getPrice() <= sell.getPrice()) {
            idx++;
        }
        sell_stock_list.add(idx, sell);
    }

    /**
     * Inserts a buy order in price-sorted order.
     * @param buy The buy order to insert
     */
    private static void insertBuySorted(OrderStock buy) {
        int idx = 0;
        while (idx < buy_stock_list.size()
                && buy_stock_list.get(idx).getPrice() >= buy.getPrice()) {
            idx++;
        }
        buy_stock_list.add(idx, buy);
    }

    /**
     * Prints the current profit and loss for all traders.
     */
    public static void printPnL() {
        if (pnlMap.isEmpty()) {
            System.out.println("No trades executed yet");
        } else {
            for (String trader : pnlMap.keySet()) {
                System.out.printf("%s  $%.2f%n", trader, pnlMap.get(trader));
            }
        }
    }

    /**
     * Prints general market metrics: total trades, volume, and avg price.
     */
    public static void printMetrics() {
        System.out.println();
        System.out.println("Metrics");
        System.out.println("Total trades  " + totalTrades);
        System.out.println("Total volume  " + totalVolume);
        if (totalVolume > 0) {
            double avg = totalNotional / totalVolume;
            System.out.printf("Avg trade price  $%.4f%n", avg);
        } else {
            System.out.println("Avg trade price  N A");
        }
    }

    /**
     * Prints a side-by-side market ladder of buy and sell levels.
     * @param depth Number of levels to display
     */
    public static void printMarketLadder(int depth) {
        ArrayList<String> b = buildBuyLevels();
        ArrayList<String> s = buildSellLevels();
        int maxRows = Math.max(Math.min(depth, b.size()), Math.min(depth, s.size()));
        maxRows = Math.max(maxRows, Math.min(depth, Math.max(b.size(), s.size())));
        String header = String.format("%-16s | %-16s", "BUY", "SELL");
        String line   = "---------------- | ----------------";
        System.out.println(header);
        System.out.println(line);
        for (int i = 0; i < maxRows; i++) {
            String left  = (i < b.size()) ? b.get(i) : "";
            String right = (i < s.size()) ? s.get(i) : "";
            System.out.printf("%-16s | %-16s%n", left, right);
        }
    }

    /**
     * Builds aggregated buy order levels for the market ladder.
     * @return List of formatted buy levels
     */
    private static ArrayList<String> buildBuyLevels() {
        ArrayList<String> levels = new ArrayList<>();
        int i = 0;
        while (i < buy_stock_list.size()) {
            double price = buy_stock_list.get(i).getPrice();
            int qty = 0;
            while (i < buy_stock_list.size() && buy_stock_list.get(i).getPrice() == price) {
                qty += buy_stock_list.get(i).getQuantity();
                i++;
            }
            levels.add(String.format("%4d @ $%.2f", qty, price));
        }
        return levels;
    }

    /**
     * Builds aggregated sell order levels for the market ladder.
     * @return List of formatted sell levels
     */
    private static ArrayList<String> buildSellLevels() {
        ArrayList<String> levels = new ArrayList<>();
        int i = 0;
        while (i < sell_stock_list.size()) {
            double price = sell_stock_list.get(i).getPrice();
            int qty = 0;
            while (i < sell_stock_list.size() && sell_stock_list.get(i).getPrice() == price) {
                qty += sell_stock_list.get(i).getQuantity();
                i++;
            }
            levels.add(String.format("%4d @ $%.2f", qty, price));
        }
        return levels;
    }
}
