package xyz.inv1s1bl3.countries.storage;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gson adapter for LocalDateTime serialization and deserialization.
 * Handles conversion between LocalDateTime objects and JSON strings.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    @Override
    @NotNull
    public JsonElement serialize(@NotNull final LocalDateTime src, 
                               @NotNull final Type typeOfSrc, 
                               @NotNull final JsonSerializationContext context) {
        return new JsonPrimitive(src.format(FORMATTER));
    }
    
    @Override
    @NotNull
    public LocalDateTime deserialize(@NotNull final JsonElement json, 
                                   @NotNull final Type typeOfT, 
                                   @NotNull final JsonDeserializationContext context) throws JsonParseException {
        try {
            return LocalDateTime.parse(json.getAsString(), FORMATTER);
        } catch (final Exception exception) {
            throw new JsonParseException("Failed to parse LocalDateTime: " + json.getAsString(), exception);
        }
    }
}