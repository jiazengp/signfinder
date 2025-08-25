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
- Multilingual support: Chinese and English

## Usage

### Commands

```
/findsign <query>                        # Text search
/findsign "中文搜索需要括起来"            # Enclose non-ASCII characters or text with spaces/special symbols in quotes "
/findsign regex <pattern>                # Regex search  
/findsign array word1,word2              # Multi-word search (comma-separated)
/findsign preset <preset name>           # Use preset
/findsign presets                        # List saved presets
/findsign clear                          # Clear results
/findsign export <format (TXT/JSON)>     # Export last search result to file 
```

### Configuration

Access via ModMenu → SignFinder → Config

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
