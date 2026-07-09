# AGENTS.md

This file provides guidance to AI coding agents (Claude Code, Codex, Cursor, etc.)
when working with code in this repository.

## Project Overview

SignFinder is a Minecraft Fabric mod that intelligently finds and highlights signs based on their content. It features configurable automatic detection of container-related signs and a comprehensive search command system supporting text, regex, array, and preset searches with pagination and distance sorting.

## Development Commands

### Build and Setup
- `./gradlew build` - Build the mod JAR file
- `./gradlew genSources` - Generate source mappings for development
- `./gradlew eclipse` - Generate Eclipse project files
- `./gradlew vscode` - Generate VSCode project files (Fabric versions only)

### Code Quality
- `./gradlew spotlessCheck` - Check code formatting and license headers
- `./gradlew spotlessApply` - Apply code formatting fixes

### Testing
- `./gradlew runClient` - Launch Minecraft client with the mod for manual testing

### Publishing (Maintainer Only)
- `./gradlew publishMods` - Publish to CurseForge (requires API key)
- `./gradlew github` - Create GitHub release (requires token)
- `./gradlew uploadBackups` - Upload backup artifacts

## Code Architecture

### Package Structure

The codebase uses the `net.signfinder` package structure:
- `net.signfinder` - Core mod classes and configuration
- `net.signfinder.managers` - Manager classes coordinating different aspects of functionality
- `net.signfinder.services` - Service interfaces and implementations
- `net.signfinder.cache` - Caching infrastructure for performance optimization
- `net.signfinder.search` - Search processing and query handling
- `net.signfinder.detection` - Automatic detection services
- `net.signfinder.commands` - Command system implementation
- `net.signfinder.mixin` - Mixin classes for Minecraft integration
- `net.signfinder.util` - Utility classes
- `net.signfinder.test` - End-to-end test client

### Core Components

**Main Mod Class (`SignFinderMod.java`):**
- Singleton pattern with dependency injection of manager classes
- Manages configuration through AutoConfig/Cloth Config
- Coordinates managers for detection, search results, rendering, and caching
- Central update loop with periodic cache cleanup
- Service-oriented architecture with clean separation of concerns

**Manager Layer:**
- `EntityDetectionManager` - Handles automatic sign/item frame detection
- `SearchResultManager` - Manages search results and highlighting logic
- `HighlightRenderManager` - Coordinates rendering of highlights and tracers
- `ColorManager` - Centralizes color management and theme handling
- `AutoSaveManager` - Handles persistent storage of search results and presets
- `KeyBindingHandler` - Manages keyboard shortcuts and bindings

**Service Layer:**
- `SearchService` - Core search interface with configurable parameters
- `EntitySearchService` - Implementation of entity search functionality
- `SearchQueryProcessor` - Processes and validates search queries
- `CacheService` - Manages cache operations and cleanup
- `AutoDetectionCacheService` - Specialized caching for automatic detection
- `ServiceRegistry` - Dependency injection container with service registration and lookup

**Cache Infrastructure:**
- `LocalDataCacheManager` - Manages local cached data with validation and cleanup
- `SignDataCache` - Caches sign text data for performance
- `PatternCache` - Caches compiled regex patterns to avoid recompilation

**Command System:**
- Modular command architecture with core and specialized command separation
- Core commands: `BaseCommand`, `SearchCommand` with `CommandUtils` and `CommandConstants`
- Specialized commands: `ExportCommand`, `HighlightCommand`, `PageCommand`, `PresetCommand`, `ResultDisplayCommand`
- `SignExportFormatArgument` for command argument handling

### Key Features

**Automatic Detection:**
- Configurable keyword-based detection (disabled by default)
- Case-sensitive search support
- Ignore word filtering
- Performance-optimized chunk scanning

**Manual Search:**
- Text, regex, and array search modes
- Preset system for commonly used searches
- Pagination with configurable page sizes
- Distance-based sorting and smart text previews
- Clickable results with coordinate copying

**Smart Highlighting:**
- Auto-removal when approaching highlighted signs
- Option to clear all highlights when approaching any target
- Separate controls for sign highlighting and tracer lines
- Configurable colors and styles

### Command System

**Available Commands:**
- `/findsign <query>` - Basic text search
- `/findsign regex <pattern>` - Regular expression search
- `/findsign array [keywords]` - Multi-term search (comma-separated)
- `/findsign preset <name>` - Use saved presets
- `/findsign page <number>` - Navigate to specific page
- `/findsign current` - Refresh current page
- `/findsign presets` - List all saved presets
- `/findsign clear` - Clear search results
- `/findsign export <format>` - Export search results to file (TXT/JSON)

**Advanced Features:**
- Player-specific result caching
- Page state persistence across new searches
- Radius parameter support for all search types
- Preset saving during search execution

## Development Standards

### Java Version
- Requires Java 21 (Minecraft 1.21+ requirement)
- Uses modern Java features (records, pattern matching, switch expressions)

### Code Style
- Eclipse formatter configuration in `codestyle/formatter.xml`
- Spotless plugin enforces formatting (no license headers)
- Line ending normalization for Windows

### Dependencies
- Fabric Loader and Fabric API
- Cloth Config for settings GUI (bundled)
- ModMenu for mod configuration access (bundled)
- Security: Forces newer versions of vulnerable dependencies (CVE fixes for jackson-core, commons-lang3, json-smart, nimbus-jose-jwt)
- Uses dependency constraints to override vulnerable transitive dependencies

### Architecture Principles
- Manager-service layered architecture with clear separation of concerns
- Dependency injection through `ServiceRegistry` for loose coupling
- Interface-based service design for testability and modularity
- Utility classes use enum singleton pattern (e.g., `AutoSaveManager.INSTANCE`)
- No hardcoded strings - all user-facing text uses translation keys
- Performance-optimized with configurable update intervals (6000 ticks cache cleanup)
- Memory-efficient caching with player-specific cleanup and periodic maintenance
- Defensive programming with null safety and error handling
- Multi-threaded cache operations using ConcurrentHashMap for thread safety
- Mixin integration for Minecraft client hooks (`ClientPlayerEntityMixin`, `GameRendererMixin`, `WorldRendererMixin`)