package sync;

public class SyncIntegration {
    private static IDelegate delegate;

    public interface IDelegate {
        ILogger getLogger(Class cls);

        void reportFailureMetric();
    }

    public interface ILogger {
        void debug(String str);

        void error(String str);

        void error(String str, Throwable th);

        void trace(String str);
    }

    public static void setDelegate(IDelegate delegate2) {
        delegate = delegate2;
    }

    public static IDelegate getDelegate() {
        if (delegate != null) {
            return delegate;
        }
        throw new IllegalStateException("sync integration not yet configured");
    }
}
