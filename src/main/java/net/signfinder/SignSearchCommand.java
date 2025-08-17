package net.signfinder;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.signfinder.services.SearchQuery.SearchType;
import net.signfinder.commands.core.*;
import net.signfinder.commands.specialized.*;

class SignSearchCommand
{
	public static void register(
		CommandDispatcher<FabricClientCommandSource> dispatcher)
	{
		dispatcher.register(ClientCommandManager
			.literal(CommandConstants.COMMAND_NAME)
			.executes(SearchCommand::executeSearchAll)
			.then(ClientCommandManager
				.argument("query", StringArgumentType.string())
				.executes(
					ctx -> SearchCommand.executeSearch(ctx, null, null, null))
				.then(
					ClientCommandManager
						.argument("radius", IntegerArgumentType.integer(1))
						.executes(ctx -> SearchCommand.executeSearch(ctx,
							IntegerArgumentType.getInteger(ctx, "radius"), null,
							null))
						.then(ClientCommandManager
							.argument("preset_name", StringArgumentType
								.string())
							.executes(ctx -> SearchCommand.executeSearch(
								ctx,
								IntegerArgumentType.getInteger(ctx, "radius"),
								StringArgumentType.getString(ctx,
									"preset_name"),
								null)))))
			
			.then(ClientCommandManager
				.literal(CommandConstants.SUBCOMMAND_REGEX)
				.then(ClientCommandManager
					.argument("pattern", StringArgumentType.string())
					.executes(ctx -> SearchCommand.executeSearch(ctx, null,
						null, SearchType.REGEX))
					.then(ClientCommandManager
						.argument("radius", IntegerArgumentType.integer(1))
						.executes(ctx -> SearchCommand.executeSearch(ctx,
							IntegerArgumentType.getInteger(ctx, "radius"), null,
							SearchType.REGEX))
						.then(ClientCommandManager
							.argument(
								"preset_name", StringArgumentType.string())
							.executes(ctx -> SearchCommand.executeSearch(ctx,
								IntegerArgumentType.getInteger(ctx, "radius"),
								StringArgumentType.getString(ctx,
									"preset_name"),
								SearchType.REGEX))))))
			// 数组搜索
			.then(ClientCommandManager
				.literal(CommandConstants.SUBCOMMAND_ARRAY)
				.then(ClientCommandManager
					.argument("keywords", StringArgumentType.string())
					.executes(ctx -> SearchCommand.executeSearch(ctx, null,
						null, SearchType.ARRAY))
					.then(ClientCommandManager
						.argument("radius", IntegerArgumentType.integer(1))
						.executes(ctx -> SearchCommand.executeSearch(ctx,
							IntegerArgumentType.getInteger(ctx, "radius"), null,
							SearchType.ARRAY))
						.then(ClientCommandManager
							.argument(
								"preset_name", StringArgumentType.string())
							.executes(ctx -> SearchCommand.executeSearch(ctx,
								IntegerArgumentType.getInteger(ctx, "radius"),
								StringArgumentType.getString(ctx,
									"preset_name"),
								SearchType.ARRAY))))))
			// 预设搜索
			.then(ClientCommandManager
				.literal(CommandConstants.SUBCOMMAND_PRESET)
				.then(ClientCommandManager
					.argument("preset_name", StringArgumentType.string())
					.executes(ctx -> SearchCommand.executeSearch(ctx, null,
						null, SearchType.PRESET))
					.then(ClientCommandManager
						.argument("radius", IntegerArgumentType.integer(1))
						.executes(ctx -> SearchCommand.executeSearch(ctx,
							IntegerArgumentType.getInteger(ctx, "radius"), null,
							SearchType.PRESET)))))
			// 分页命令
			.then(ClientCommandManager.literal(CommandConstants.SUBCOMMAND_PAGE)
				.then(ClientCommandManager
					.argument("page_number", IntegerArgumentType.integer(1))
					.executes(PageCommand::executePage)))
			.then(ClientCommandManager
				.literal(CommandConstants.SUBCOMMAND_CURRENT)
				.executes(PageCommand::showCurrentPage))
			// 预设管理
			.then(ClientCommandManager
				.literal(CommandConstants.SUBCOMMAND_PRESETS)
				.executes(PresetCommand::listPresets))
			// 高亮管理
			.then(
				ClientCommandManager.literal(CommandConstants.SUBCOMMAND_REMOVE)
					.then(ClientCommandManager
						.argument("x", IntegerArgumentType.integer())
						.then(ClientCommandManager
							.argument("y", IntegerArgumentType.integer())
							.then(ClientCommandManager
								.argument("z", IntegerArgumentType.integer())
								.executes(HighlightCommand::removeHighlight)))))
			.then(
				ClientCommandManager.literal(CommandConstants.SUBCOMMAND_COLOR)
					.then(ClientCommandManager
						.argument("x", IntegerArgumentType.integer())
						.then(ClientCommandManager
							.argument("y", IntegerArgumentType.integer())
							.then(ClientCommandManager
								.argument("z", IntegerArgumentType.integer())
								.executes(
									HighlightCommand::changeHighlightColor)))))
			.then(
				ClientCommandManager.literal(CommandConstants.SUBCOMMAND_CLEAR)
					.executes(HighlightCommand::clearResults))
			.then(
				ClientCommandManager.literal(CommandConstants.SUBCOMMAND_EXPORT)
					.executes(ctx -> ExportCommand.executeExport(ctx,
						AutoConfig.getConfigHolder(SignFinderConfig.class)
							.getConfig().export_format))
					.then(
						ClientCommandManager
							.argument("format",
								SignExportFormatArgument.exportFormat())
							.executes(
								ctx -> ExportCommand.executeExport(ctx, null)))
			
			));
	}
}
