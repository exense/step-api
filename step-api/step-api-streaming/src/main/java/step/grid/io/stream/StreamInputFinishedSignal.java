package step.grid.io.stream;

import java.util.concurrent.CompletableFuture;

// For "client-side" use, MUST be used to signal completion of a stream.
// Currently, it's intended to signal an error message (non-null String),
// or no error=successful completion (null String).
public class StreamInputFinishedSignal extends CompletableFuture<String> {
}
