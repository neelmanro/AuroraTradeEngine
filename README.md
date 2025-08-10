# AuroraTradeEngine

AuroraTradeEngine is a multi-threaded trading engine simulation that processes live buy and sell orders in real time.  
It features order matching, market ladder visualization, PnL tracking, and key performance metrics, using concurrency with a Java `BlockingQueue` to handle continuous incoming orders.

## Features
- **Real-time Order Matching**: Matches buy and sell orders based on price and availability.
- **Market Ladder Display**: Shows aggregated bid and ask levels for quick market insight.
- **PnL Tracking**: Keeps track of profits and losses for each trader.
- **Live Metrics**: Displays trade count, volume, and average trade price.
- **Concurrency**: Uses a separate feeder and processing threads for smooth performance.

## How it Works
1. **Manual Mode** – Users can enter buy or sell orders through the console.
2. **Realtime Mode** – A simulated order feeder generates random trades which are processed instantly.
3. Orders are matched based on price and company, updating the order book and PnL in real time.

## Tech Stack
- **Language**: Java
- **Data Structures**: ArrayList, HashMap, BlockingQueue
- **Concurrency**: Multi-threading with `LinkedBlockingQueue`
- **Design**: Simulated matching engine for educational and demo purposes

## Getting Started
1. Clone this repository:
   ```bash
   git clone https://github.com/neelmanro/AuroraTradeEngine.git
