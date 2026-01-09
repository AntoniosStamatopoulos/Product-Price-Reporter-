# Global Commodity Prices Dataset (1960-2023)

## Dataset Overview
Annual commodity price data spanning 64 years with 31 commodities, market headlines, and top commodity rankings.

## Data Structure
- **Format**: CSV file
- **Rows**: 64 (years 1960-2023)
- **Columns**: 34 total

## Data Dictionary

### Core Fields
| Column | Data Type | Unit | Description |
|--------|-----------|------|-------------|
| year | Integer | - | Year (1960-2023) |

### Energy Commodities
| Column | Unit | Description |
|--------|------|-------------|
| Crude oil, average | USD/barrel | Annual average crude oil price |
| Natural gas avg | USD/mmBtu | Average natural gas price |

### Agricultural Products
| Column | Unit | Description |
|--------|------|-------------|
| Cocoa | USD/kg | Cocoa beans price |
| Coffee, Arabica | USD/kg | Arabica coffee beans |
| Tea, avg 3 auctions | USD/kg | Tea average from 3 major auctions |
| Coconut oil | USD/mt | Coconut oil price per metric ton |
| Groundnut oil | USD/mt | Peanut oil price |
| Palm oil | USD/mt | Palm oil price |
| Soybeans | USD/mt | Soybean commodity price |
| Soybean oil | USD/mt | Processed soybean oil |

### Food Staples
| Column | Unit | Description |
|--------|------|-------------|
| Rice, Thai 5% | USD/mt | Thai rice, 5% broken |
| Wheat, US HRW | USD/mt | US Hard Red Winter wheat |
| Banana, US | USD/kg | US banana import price |
| Orange | USD/kg | Orange commodity price |

### Livestock & Protein
| Column | Unit | Description |
|--------|------|-------------|
| Beef | USD/kg | Beef commodity price |
| Chicken | USD/kg | Chicken commodity price |

### Industrial Agricultural
| Column | Unit | Description |
|--------|------|-------------|
| Sugar, world | USD/kg | World sugar price |
| Cotton, A Index | USD/kg | Cotton A Index price |
| Rubber, RSS3 | USD/kg | Ribbed Smoked Sheet 3 rubber |

### Fertilizers
| Column | Unit | Description |
|--------|------|-------------|
| Phosphate rock | USD/mt | Phosphate rock price |
| Potassium chloride | USD/mt | Potash fertilizer price |

### Metals & Minerals
| Column | Unit | Description |
|--------|------|-------------|
| Aluminum | USD/mt | Aluminum commodity price |
| Iron ore, cfr spot | USD/dmt | Iron ore, cost & freight spot price |
| Copper | USD/mt | Copper commodity price |
| Lead | USD/mt | Lead metal price |
| Tin | USD/mt | Tin metal price |
| Nickel | USD/mt | Nickel commodity price |
| Zinc | USD/mt | Zinc metal price |

### Precious Metals
| Column | Unit | Description |
|--------|------|-------------|
| Gold | USD/troy oz | Gold spot price |
| Platinum | USD/troy oz | Platinum spot price |
| Silver | USD/troy oz | Silver spot price |

### Contextual Data
| Column | Data Type | Description |
|--------|-----------|-------------|
| Commodity News Headline | List | Top 10 commodities for the year |
| [Last Column] | String | Major commodity market events/headlines |

## Usage Instructions

### Data Loading
```python
import pandas as pd
df = pd.read_csv('commodity_prices_1960_2023.csv')