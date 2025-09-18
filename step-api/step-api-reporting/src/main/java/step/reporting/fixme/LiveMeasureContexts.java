package step.reporting.fixme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.reports.Measure;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LiveMeasureContexts {
    private static final Logger logger = LoggerFactory.getLogger(LiveMeasureContexts.class);
    private final Map<String, LiveMeasureContextListener> listeners = new ConcurrentHashMap<>();

    private final String injectionUrlTemplate;

    public LiveMeasureContexts(String injectionUrlTemplate) {
        this.injectionUrlTemplate = injectionUrlTemplate;
    }

    public void onMeasuresReceived(String contextHandle, List<Measure> measures) {
        LiveMeasureContextListener listener = listeners.get(contextHandle);
        if (listener != null) {
            try {
                listener.accept(measures);
            } catch (Exception e) {
                logger.error("Error while dispatching {} measures for context handle {}", measures.size(), contextHandle, e);
            }
        } else {
            // should we be stricter here, i.e., throw an exception?
            logger.warn("Received {} measures for invalid/unregistered context handle {}", measures.size(), contextHandle);
        }
    }

    public String register(LiveMeasureContextListener contextListener) {
        String handle = UUID.randomUUID().toString();
        listeners.put(handle, contextListener);
        return handle;
    }

    public void unregister(String liveMeasureContextHandle) {
        listeners.remove(liveMeasureContextHandle);
    }

    public String getInjectionUrl(String liveMeasureContextHandle) {
        // we know the handles are UUIDs, so there is no need to URL-encode anything
        return injectionUrlTemplate + liveMeasureContextHandle;
    }
}
