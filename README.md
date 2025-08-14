# SignFinder
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fjiazengp%2Fsignfinder.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Fjiazengp%2Fsignfinder?ref=badge_shield)


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
/findsign <query>             # Text search
/findsign "中文查询"            # Chinese text search (use quotes for non-ASCII)
/findsign regex <pattern>     # Regex search  
/findsign array word1,word2   # Multi-word search (comma-separated)
/findsign preset <name>       # Use preset
/findsign presets            # List saved presets
/findsign clear              # Clear results
/findsign export <format>            # Export last search result to file (TXT/JSON)
```

### Configuration

Access via ModMenu → SignFinder → Config

## Development

```bash
git clone https://github.com/jiazengp/signfinder.git
./gradlew build
```

## License

[GPLv3 License](LICENSE)

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fjiazengp%2Fsignfinder.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Fjiazengp%2Fsignfinder?ref=badge_large)