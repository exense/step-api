package step.handlers.javahandler;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import step.functions.io.AbstractSession;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;

import javax.json.JsonObject;
import java.lang.reflect.InvocationTargetException;

public class KeywordProxy {

    private AbstractSession session = new AbstractSession();
    private Output<JsonObject> lastOutput;

    public <T extends AbstractKeyword> T getProxy(Class<T> keywordClass) {
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(keywordClass);
        factory.setFilter(method -> method.getAnnotation(Keyword.class) != null);

        MethodHandler handler = (self, thisMethod, proceed, args) -> {
            Keyword annotation = thisMethod.getAnnotation(Keyword.class);
            AbstractKeyword abstractKeyword = (AbstractKeyword) self;
            abstractKeyword.setSession(session);
            abstractKeyword.setOutputBuilder(new OutputBuilder());
            String keywordName = annotation.name();
            abstractKeyword.beforeKeyword(keywordName, annotation);

            try {
                return proceed.invoke(self, args);
            } finally {
                abstractKeyword.afterKeyword(keywordName, annotation);
                lastOutput = abstractKeyword.getOutputBuilder().build();
            }
        };

        try {
            return (T) factory.create(new Class<?>[0], new Object[0], handler);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public Output<JsonObject> getLastOutput() {
        return lastOutput;
    }
}
