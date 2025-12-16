package com.firefly.oshe.lunli;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.firefly.oshe.lunli.utils.InitialInitUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class LaunchAppView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private InitialInitUtils appView;
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private int maxVertices = 1000;

    public LaunchAppView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        setRenderer(this);

        appView = new InitialInitUtils();

        vertexBuffer = ByteBuffer.allocateDirect(maxVertices * 3 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        colorBuffer = ByteBuffer.allocateDirect(maxVertices * 3 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }

    public void startRenderView() {
        appView.startRendering();
    }

    public void stopRenderView() {
        appView.stopRendering();
    }

    public void setScreenSize(int width, int height) {
        appView.setScreenSize(width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        appView.setScreenSize(getWidth(), getHeight());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        appView.setScreenSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        float[] vertices = new float[maxVertices * 3];
        float[] colors = new float[maxVertices * 3];

        int vertexCount = appView.getRenderData(vertices, colors, maxVertices);

        if (vertexCount > 0) {
            vertexBuffer.clear();
            vertexBuffer.put(vertices, 0, vertexCount * 3);
            vertexBuffer.position(0);

            colorBuffer.clear();
            colorBuffer.put(colors, 0, vertexCount * 3);
            colorBuffer.position(0);

            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
            gl.glColorPointer(3, GL10.GL_FLOAT, 0, colorBuffer);

            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, vertexCount);

            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (appView != null) {
            appView.destroy();
        }
    }
}