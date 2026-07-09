# SignFinder

A Minecraft Fabric mod that finds and highlights signs based on content with search capabilities.

This mod is designed to help players locate signs or item frames containing specific content in large multiplayer server markets, making it easy to find what they are looking for even when numerous stalls are present.

## Features

- Smart sign detection with configurable keywords
- Text, regex, and array searches with presets
- ESP-style highlighting with customizable colors
- Pagination and distance-based sorting
- Auto-removal when approaching signs
- Automatic local saving of search results
- Multilingual support: Chinese, English, and Russian

## Requirements

- Minecraft >=26.1.0 <26.3.0
- Fabric Loader >=0.19.0
- Fabric API >=0.152.0+26.1
- Java >=25

## Usage

### Commands

```
/findsign <query>                        # Text search
/findsign regex <pattern>                # Regex search  
/findsign array word1,word2              # Multi-word search (comma-separated)
/findsign preset <preset name>           # Use preset
/findsign preset <preset name> <radius>  # Use preset with radius
/findsign presets                        # List saved presets
/findsign page <number>                  # Go to page
/findsign current                        # Refresh current page
/findsign clear                          # Clear results
/findsign remove <x> <y> <z>            # Remove highlight at position
/findsign color <x> <y> <z>             # Cycle highlight color
/findsign export <TXT/JSON>              # Export last search results
```

### Configuration

Access via ModMenu → SignFinder → Config, or edit `config/signfinder.json`:
- Auto-detection keywords and ignore words
- Search radius and page size
- Highlight colors and ESP styles
- Auto-save mode and export format

## Development

```bash
git clone https://github.com/jiazengp/signfinder.git
./gradlew build
```

## Compatibility

### Known incompatibility:

- [Vulkan](https://modrinth.com/mod/vulkanmod)

### About v1.20.x or lower

This mod will not temporarily support Minecraft versions below 1.21.
If you are on 1.20, consider using [sign-searcher-updated](https://modrinth.com/mod/sign-searcher-updated).

## License

[GPLv3 License](LICENSE)
