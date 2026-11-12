package dev.jaonoctus.easykeychain;


import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import dev.jaonoctus.easykeychain.shadows.ShadowWorkManager;


public class KeychainTestRunner extends RobolectricTestRunner {

    public KeychainTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected Config buildGlobalConfig() {
        return new Config.Builder()
                .setSdk(27)
                .setShadows(new Class[] { ShadowWorkManager.class })
                .build();
    }
}
