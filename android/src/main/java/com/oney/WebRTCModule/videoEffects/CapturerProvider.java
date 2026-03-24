package com.oney.WebRTCModule.videoEffects;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Registry for custom video capturer factories.
 * External modules can register a {@link CapturerFactoryInterface} to provide
 * custom video capturers (e.g., for video effects processing).
 *
 * Only one factory can be registered at a time (single active effects pipeline).
 */
public class CapturerProvider {
    private static final AtomicReference<CapturerFactoryInterface> factory =
            new AtomicReference<>(null);

    /**
     * Registers a custom capturer factory.
     * Replaces any previously registered factory.
     */
    public static void setFactory(CapturerFactoryInterface capturerFactory) {
        factory.set(capturerFactory);
    }

    /**
     * Returns the registered capturer factory, or null if none is registered.
     */
    public static CapturerFactoryInterface getFactory() {
        return factory.get();
    }

    /**
     * Removes the registered capturer factory.
     */
    public static void removeFactory() {
        factory.set(null);
    }

    /**
     * Returns true if a custom capturer factory is registered.
     */
    public static boolean hasFactory() {
        return factory.get() != null;
    }
}
