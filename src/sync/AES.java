package sync;

import sync.SyncIntegration;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Deprecated
final class AES {
    private static final String ALGORITHM = "AES/CBC/NoPadding";
    private static final byte[] IV = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final byte[] KEY = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private Cipher dCipher;
    private Cipher eCipher;
    private final SyncIntegration.ILogger logger = SyncIntegration.getDelegate().getLogger(getClass());

    public AES() {
        SecretKeySpec key = new SecretKeySpec(KEY, "AES");
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(IV);
        try {
            this.eCipher = Cipher.getInstance(ALGORITHM);
            this.dCipher = Cipher.getInstance(ALGORITHM);
            this.eCipher.init(1, key, paramSpec);
            this.dCipher.init(2, key, paramSpec);
        } catch (Exception e) {
            this.logger.error("AES ciphers not available", e);
        }
    }
}
