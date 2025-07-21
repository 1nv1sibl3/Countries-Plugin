# Countries Plugin Development Plan

## Project Overview
A comprehensive Minecraft plugin for Paper API 1.20.4+ that allows players to create countries, manage territories, handle economics, diplomacy, and governance systems.

## Architecture & Package Structure

```
xyz.inv1s1bl3.countries/
├── CountriesPlugin.java (Main class)
├── api/
│   ├── CountriesAPI.java
│   └── events/ (Custom events)
├── commands/
│   ├── CountryCommand.java
│   ├── TerritoryCommand.java
│   ├── EconomyCommand.java
│   └── MarketCommand.java
├── config/
│   ├── ConfigManager.java
│   ├── MessagesConfig.java
│   └── PluginConfig.java
├── core/
│   ├── country/
│   │   ├── Country.java
│   │   ├── CountryManager.java
│   │   ├── CitizenRole.java
│   │   └── GovernmentType.java
│   ├── territory/
│   │   ├── Territory.java
│   │   ├── TerritoryManager.java
│   │   ├── TerritoryType.java
│   │   └── ChunkClaimManager.java
│   ├── economy/
│   │   ├── EconomyManager.java
│   │   ├── BankAccount.java
│   │   ├── Transaction.java
│   │   └── TaxSystem.java
│   ├── market/
│   │   ├── MarketManager.java
│   │   ├── MarketListing.java
│   │   └── MarketTransaction.java
│   ├── diplomacy/
│   │   ├── DiplomacyManager.java
│   │   ├── DiplomaticRelation.java
│   │   ├── Alliance.java
│   │   └── Trade.java
│   └── law/
│       ├── LawSystem.java
│       ├── Crime.java
│       ├── Law.java
│       └── Arrest.java
├── gui/
│   ├── GUIManager.java
│   ├── CountryGUI.java
│   ├── TerritoryGUI.java
│   ├── EconomyGUI.java
│   └── MarketGUI.java
├── listeners/
│   ├── PlayerListener.java
│   ├── ChunkListener.java
│   └── GUIListener.java
├── storage/
│   ├── DataManager.java
│   ├── CountryStorage.java
│   ├── TerritoryStorage.java
│   └── EconomyStorage.java
└── utils/
    ├── ChatUtils.java
    ├── ItemUtils.java
    └── LocationUtils.java
```

## Development Phases

### Phase 1: Foundation
1. Setup Maven project structure
2. Create main plugin class with static instance
3. Implement configuration management
4. Setup data storage system with Gson
5. Create basic command framework
6. Implement chat utilities and messaging

### Phase 2: Core Country System
1. Country data model and management
2. Player citizenship system
3. Government types and roles
4. Basic country commands (/country create, join, leave, info)
5. Country GUI dashboard

### Phase 3: Territory Management
1. Chunk-based land claiming system
2. Territory types and protection
3. Territory management commands
4. Visual territory boundaries
5. Territory GUI interface

### Phase 4: Economy System
1. Personal and country bank accounts
2. Tax collection system
3. Salary distribution
4. Transaction logging
5. Economy GUI interface

### Phase 5: Market System
1. Item listing and browsing
2. Secure buying/selling
3. Market search and filters
4. Price history tracking
5. Market GUI interface

### Phase 6: Diplomacy & Trade
1. Inter-country relationships
2. Alliance system
3. Trade agreements
4. Diplomatic commands
5. Diplomacy GUI interface

### Phase 7: Law & Order
1. Legal system with laws
2. Crime tracking
3. Arrest and fine system
4. Law enforcement roles
5. Legal GUI interface

### Phase 8: Advanced Features
1. Country flags and customization
2. Advanced territory features
3. Statistics and analytics
4. API for other plugins
5. Performance optimization

## Technical Specifications

### Data Storage
- JSON files using Gson for serialization
- Async file operations for performance
- Data validation and migration support
- Backup and recovery systems

### GUI System
- Inventory-based GUIs using Paper API
- Pagination for large datasets
- Interactive buttons and navigation
- Real-time data updates

### Economy Integration
- Optional Vault API integration
- Custom currency system
- Transaction security
- Anti-duplication measures

### Performance Considerations
- Async chunk loading/unloading
- Efficient data caching
- Optimized database queries
- Memory management

## Configuration Files

### config.yml
```yaml
# General settings
enable-debug: false
auto-save-interval: 300
max-countries-per-player: 1
max-territories-per-country: 50

# Economy settings
starting-balance: 1000.0
tax-collection-interval: 86400
default-tax-rate: 0.05

# Territory settings
chunk-claim-cost: 100.0
max-chunks-per-territory: 100
allow-wilderness-claims: true

# Market settings
listing-fee: 10.0
transaction-fee: 0.02
max-listings-per-player: 10

# Diplomacy settings
alliance-cost: 5000.0
war-declaration-cost: 10000.0
```

### messages.yml
```yaml
# Country messages
country:
  created: "&aCountry '{country}' has been created!"
  joined: "&aYou have joined {country}!"
  not-found: "&cCountry not found!"
  
# Territory messages
territory:
  claimed: "&aTerritory claimed successfully!"
  protected: "&cThis territory is protected!"
  
# Economy messages
economy:
  insufficient-funds: "&cInsufficient funds!"
  transaction-complete: "&aTransaction completed!"
```

## Command Structure

### /country
- `/country create <name>` - Create a new country
- `/country info [name]` - View country information
- `/country join <name>` - Join a country
- `/country leave` - Leave current country
- `/country invite <player>` - Invite player to country
- `/country kick <player>` - Remove player from country
- `/country promote <player>` - Promote player rank
- `/country demote <player>` - Demote player rank
- `/country gui` - Open country management GUI

### /territory
- `/territory claim <name>` - Claim current chunk
- `/territory unclaim` - Unclaim current chunk
- `/territory info` - View territory information
- `/territory list` - List all territories
- `/territory gui` - Open territory management GUI

### /economy
- `/economy balance [player]` - Check balance
- `/economy pay <player> <amount>` - Send money
- `/economy tax set <rate>` - Set country tax rate
- `/economy salary <role> <amount>` - Set role salary
- `/economy gui` - Open economy management GUI

### /market
- `/market list <item> <price>` - List item for sale
- `/market buy <id>` - Purchase listing
- `/market search [item]` - Search listings
- `/market gui` - Open market GUI

### /diplomacy
- `/diplomacy ally <country>` - Propose alliance
- `/diplomacy enemy <country>` - Declare war
- `/diplomacy neutral <country>` - Set neutral relations
- `/diplomacy trade <country>` - Propose trade agreement
- `/diplomacy gui` - Open diplomacy GUI

## Implementation Priority
1. Foundation and configuration system
2. Country creation and management
3. Territory claiming system
4. Basic economy features
5. GUI interfaces
6. Market system
7. Diplomacy features
8. Law enforcement system
9. Advanced features and optimization