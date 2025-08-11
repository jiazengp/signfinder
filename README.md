# SignFinder

A Minecraft Fabric mod that finds and highlights signs based on content with search capabilities.

## Features

- Smart sign detection with configurable keywords
- Text, regex, and array search with presets
- ESP-style highlighting with customizable colors
- Pagination and distance sorting
- Auto-removal when approaching signs

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Download from [Modrinth](https://modrinth.com/mods/signfinder)
3. Place in `mods` folder

## Usage

### Commands
```
/findsign <query>        # Text search
/findsign regex <pattern># Regex search  
/findsign array [words]  # Multi-word search
/findsign preset <name>  # Use preset
/findsign clear          # Clear results
/findsign export         # Export last search result
```

### Configuration
Access via ModMenu → SignFinder → Config

## Compatibility

**Requirements:** Minecraft 1.21.6-beta.3, Java 21+, Fabric Loader 0.16.14+

**Incompatible:** Wurst Client, VulkanMod

## Development

```bash
git clone https://github.com/jiazengp/signfinder.git
./gradlew build
```

## License

[GPLv3 License](LICENSE)