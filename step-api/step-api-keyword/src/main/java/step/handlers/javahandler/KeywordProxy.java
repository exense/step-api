package step.handlers.javahandler;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import step.functions.io.AbstractSession;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;

import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;

public class KeywordProxy {

    private final AbstractSession session;
    private Output<JsonObject> lastOutput;
    private final Map<String, String> properties;
    private OutputBuilder parentOutputBuilder;
    private final boolean mergeOutputsToParentOutput;

    public KeywordProxy() {
        this(new AbstractSession(), new HashMap<>());
    }

    public KeywordProxy(AbstractSession session, Map<String, String> properties) {
        this.session = session;
        this.properties = properties;
        this.mergeOutputsToParentOutput = false;
    }

    public KeywordProxy(AbstractKeyword parentKeyword, boolean mergeOutputsToParentOutput) {
        this.session = parentKeyword.session;
        this.properties = parentKeyword.properties;
        this.parentOutputBuilder = parentKeyword.output;
        this.mergeOutputsToParentOutput = mergeOutputsToParentOutput;
    }

    public <T extends AbstractKeyword> T getProxy(Class<T> keywordClass) {
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(keywordClass);
        factory.setFilter(method -> method.getAnnotation(Keyword.class) != null);

        MethodHandler handler = (self, keywordMethod, proceed, args) -> {
            Keyword keywordAnnotation = keywordMethod.getAnnotation(Keyword.class);
            KeywordExecutor keywordExecutor = new KeywordExecutor(true);
            long start = System.currentTimeMillis();
            lastOutput = keywordExecutor.executeKeyword(session, session, properties, keywordMethod, args, keywordAnnotation);
            long duration = System.currentTimeMillis() - start;
            if (parentOutputBuilder != null) {
                parentOutputBuilder.addMeasure(KeywordExecutor.getKeywordName(keywordMethod, keywordAnnotation), duration);
                if (mergeOutputsToParentOutput) {
                    parentOutputBuilder.mergeOutput(lastOutput);
                }
            }
            return null;
        };

        try {
            return (T) factory.create(new Class<?>[0], new Object[0], handler);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while creating proxy for class " + keywordClass.getName(), e);
        }
    }

    public Output<JsonObject> getLastOutput() {
        return lastOutput;
    }
}
