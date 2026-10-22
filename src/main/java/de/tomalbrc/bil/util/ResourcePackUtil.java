package de.tomalbrc.bil.util;

import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;


public class ResourcePackUtil {
    private static ConcurrentHashMap<Identifier, byte[]> data = new ConcurrentHashMap<>();
    private static final ThreadLocal<Map<Identifier, byte[]>> STAGED = new ThreadLocal<>();

    public static byte[] add(Identifier location, byte[] bytes) {
        Map<Identifier, byte[]> staged = STAGED.get();
        return staged != null ? staged.put(location, bytes) : data.put(location, bytes);
    }

    /**
     * Holds this thread's {@link #add} calls until {@code action} returns, and drops them if it
     * throws, so a half-imported model adds nothing and keeps what an earlier load added.
     */
    public static <T> T staged(Supplier<T> action) {
        if (STAGED.get() != null) return action.get();

        Map<Identifier, byte[]> staged = new Object2ObjectLinkedOpenHashMap<>();
        STAGED.set(staged);
        try {
            T result = action.get();
            data.putAll(staged);
            return result;
        } finally {
            STAGED.remove();
        }
    }

    public static void addAdditional(ResourcePackBuilder resourcePackBuilder) {
        for (var entry : data.entrySet()) {
            resourcePackBuilder.addData(entry.getKey().getPath(), entry.getValue());
        }
    }
}
