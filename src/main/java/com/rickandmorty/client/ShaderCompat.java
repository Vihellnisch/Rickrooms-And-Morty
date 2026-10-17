package com.rickandmorty.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/** Whether a shader pack (Iris) is active. Iris is optional, so this goes through reflection. */
public final class ShaderCompat {
    private static final MethodHandle IN_USE = find();

    private ShaderCompat() {
    }

    private static MethodHandle find() {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            return MethodHandles.publicLookup().findVirtual(api, "isShaderPackInUse", MethodType.methodType(boolean.class)).bindTo(instance);
        } catch (Throwable notInstalled) {
            return null;
        }
    }

    public static boolean shaderPackInUse() {
        if (IN_USE == null) return false;
        try {
            return (boolean) IN_USE.invokeExact();
        } catch (Throwable e) {
            return false;
        }
    }
}
