package decrypt;

import sync.SyncIntegration;

public class MyHushpuppySyncIntegration implements SyncIntegration.IDelegate {
    public SyncIntegration.ILogger getLogger(Class class_) {
        return new SyncIntegration.ILogger() {

            public void trace(String message) {
//                System.out.println("t: " + message);
            }

            public void debug(String message) {
//                System.out.println("d: " + message);
            }

            public void error(String message) {
                System.err.println("e: " + message);
            }

            public void error(String message, Throwable throwable) {
                System.err.print("e: " + message);
                System.err.print(throwable);
            }
        };
    }

    public void reportFailureMetric() {
        System.out.println("Someone wanted to report falure metric");
    }
}
