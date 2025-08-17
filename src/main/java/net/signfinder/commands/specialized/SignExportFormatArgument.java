package net.signfinder.commands.specialized;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.signfinder.core.SignExportFormat;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SignExportFormatArgument implements ArgumentType<SignExportFormat>
{
	
	private static final SimpleCommandExceptionType INVALID_FORMAT_EXCEPTION =
		new SimpleCommandExceptionType(() -> "Invalid export format");
	
	public static SignExportFormatArgument exportFormat()
	{
		return new SignExportFormatArgument();
	}
	
	public static SignExportFormat getFormat(CommandContext<?> context,
		String name)
	{
		return context.getArgument(name, SignExportFormat.class);
	}
	
	@Override
	public SignExportFormat parse(StringReader reader)
		throws CommandSyntaxException
	{
		String input = reader.readUnquotedString().toUpperCase();
		try
		{
			return SignExportFormat.valueOf(input);
		}catch(IllegalArgumentException e)
		{
			throw INVALID_FORMAT_EXCEPTION.create();
		}
	}
	
	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(
		CommandContext<S> context, SuggestionsBuilder builder)
	{
		String input = builder.getRemaining().toLowerCase();
		int start = builder.getStart();
		int end = builder.getInput().length();
		
		List<Suggestion> suggestions = Arrays.stream(SignExportFormat.values())
			.map(format -> format.name().toLowerCase())
			.filter(name -> name.startsWith(input))
			.map(name -> new Suggestion(new StringRange(start, end), name))
			.collect(Collectors.toList());
		
		Suggestions result =
			Suggestions.create(builder.getInput(), suggestions);
		return CompletableFuture.completedFuture(result);
	}
}
