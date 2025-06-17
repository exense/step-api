package step.grid.io.stream;

import java.util.concurrent.Callable;

// Base class for messages sent via Websockets (at least that's the intended use).
// Could also be renamed to WebsocketMessage for clarity.

// Unfortunately, in this context, we don't have the full Jackson libraries available so we cannot
// specify (de)serialization behavior. This could be solved either by depending on the jackson-annotations etc.,
// or (as is done now) by requiring a separate class providing the codec implementation.
// See the JsonMessageCodec class in step-grid-api for the implementation.
// I know this is ugly (even if it's a one-liner to initialize), but that's easy to forget.
// Why it's done this way? Because (aside from the required initialization) it provides a really simple and natural
// way for working with the messages.
public class JsonMessage {
    private static Codec codec;

    public interface Codec {
        String toString(JsonMessage message);

        <T extends JsonMessage> T fromString(String json);
    }

    public static void setCodecIfRequired(Callable<Codec> codecProvider) {
        if (JsonMessage.codec == null) {
            try {
                JsonMessage.codec = codecProvider.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Codec codec() {
        if (codec == null) {
            throw new IllegalStateException("No codec available, please register it using the setCodec() method");
        }
        return codec;
    }

    @Override
    public String toString() {
        return codec().toString(this);
    }

    public static String toString(JsonMessage message) {
        return codec().toString(message);
    }

    public static <T extends JsonMessage> T fromString(String json) {
        return codec().fromString(json);
    }
}
