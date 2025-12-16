package com.firefly.oshe.lunli.utils;

public class InitialInitUtils {
    static {
        System.loadLibrary("firefly");
    }

    private static native long createRenderer();
    private static native void destroyRenderer(long rendererPtr);
    private static native void setScreenSize(long rendererPtr, int width, int height);
    private static native void startRendering(long rendererPtr);
    private static native void stopRendering(long rendererPtr);
    private static native int getRenderData(long rendererPtr, float[] vertices, float[] colors, int maxVertices);
    private static native boolean isRendering(long rendererPtr);

    private long rendererPtr;

    public InitialInitUtils() {
        rendererPtr = createRenderer();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            destroy();
        } finally {
            super.finalize();
        }
    }

    public void destroy() {
        if (rendererPtr != 0) {
            destroyRenderer(rendererPtr);
            rendererPtr = 0;
        }
    }

    public void setScreenSize(int width, int height) {
        if (rendererPtr != 0) {
            setScreenSize(rendererPtr, width, height);
        }
    }

    public void startRendering() {
        if (rendererPtr != 0) {
            startRendering(rendererPtr);
        }
    }

    public void stopRendering() {
        if (rendererPtr != 0) {
            stopRendering(rendererPtr);
        }
    }

    public int getRenderData(float[] vertices, float[] colors, int maxVertices) {
        if (rendererPtr == 0) {
            return 0;
        }
        return getRenderData(rendererPtr, vertices, colors, maxVertices);
    }

    public boolean isRendering() {
        if (rendererPtr == 0) {
            return false;
        }
        return isRendering(rendererPtr);
    }

    public long getNativePointer() {
        return rendererPtr;
    }
}