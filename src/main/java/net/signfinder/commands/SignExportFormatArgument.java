package net.signfinder.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.signfinder.SignExportFormat;
import java.util.concurrent.CompletableFuture;

public class SignExportFormatArgument implements ArgumentType<SignExportFormat> {

    private static final SimpleCommandExceptionType INVALID_FORMAT_EXCEPTION =
            new SimpleCommandExceptionType(() -> Text.literal("Invalid export format"));

    public static SignExportFormatArgument exportFormat() {
        return new SignExportFormatArgument();
    }

    public static SignExportFormat getFormat(CommandContext<?> context, String name) {
        return context.getArgument(name, SignExportFormat.class);
    }

    @Override
    public SignExportFormat parse(StringReader reader) throws CommandSyntaxException {
        String input = reader.readUnquotedString().toUpperCase();
        try {
            return SignExportFormat.valueOf(input);
        } catch (IllegalArgumentException e) {
            throw INVALID_FORMAT_EXCEPTION.create();
        }
    }

    @Override
    public <S> CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return com.mojang.brigadier.suggestion.Suggestions.create(
                builder,
                SignExportFormat.values(),
                format -> format.get().toLowerCase(),
                (format, input) -> format.name().toLowerCase().startsWith(input.toLowerCase())
        );
    }
}
