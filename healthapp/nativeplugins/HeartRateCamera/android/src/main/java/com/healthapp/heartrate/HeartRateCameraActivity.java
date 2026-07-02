package com.healthapp.heartrate;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.dcloud.feature.uniapp.bridge.UniJSCallback;

public class HeartRateCameraActivity extends AppCompatActivity {
    public static UniJSCallback callback;
    public static int durationSeconds = 90;
    public static boolean torchEnabled = true;

    private static HeartRateCameraActivity current;

    private PreviewView previewView;
    private TextView statusText;
    private TextView metricsText;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private long startMs;
    private long lastFrameMs;
    private int frameCount;
    private boolean running;
    private final LinkedList<Double> recentRed = new LinkedList<>();

    public static void stopFromModule() {
        if (current != null) current.runOnUiThread(current::finishMeasureActivity);
    }

    public static void setTorchFromModule(boolean enabled) {
        if (current != null && current.camera != null) {
            current.camera.getCameraControl().enableTorch(enabled);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        current = this;
        cameraExecutor = Executors.newSingleThreadExecutor();
        buildUi();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1001);
        } else {
            startCamera();
        }
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        previewView = new PreviewView(this);
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        root.addView(previewView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.argb(80, 0, 0, 0));
        root.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        statusText = new TextView(this);
        statusText.setText("请将手指轻轻覆盖后置摄像头和闪光灯");
        statusText.setTextColor(Color.WHITE);
        statusText.setTextSize(18);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(32, 18, 32, 18);
        statusText.setBackgroundColor(Color.argb(150, 15, 23, 42));
        FrameLayout.LayoutParams statusLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        statusLp.gravity = Gravity.BOTTOM;
        statusLp.setMargins(32, 0, 32, 80);
        root.addView(statusText, statusLp);

        metricsText = new TextView(this);
        metricsText.setText("CameraX PPG");
        metricsText.setTextColor(Color.WHITE);
        metricsText.setTextSize(12);
        metricsText.setPadding(22, 12, 22, 12);
        metricsText.setBackgroundColor(Color.argb(120, 0, 0, 0));
        FrameLayout.LayoutParams metricsLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        metricsLp.gravity = Gravity.TOP | Gravity.RIGHT;
        metricsLp.setMargins(0, 50, 28, 0);
        root.addView(metricsText, metricsLp);

        setContentView(root);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            emitError("NO_PERMISSION", "摄像头权限未授权");
            finish();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                );
                if (torchEnabled) camera.getCameraControl().enableTorch(true);

                running = true;
                startMs = System.currentTimeMillis();
                emit("started", null);
            } catch (Exception e) {
                emitError("START_FAILED", e.getMessage() == null ? "摄像头启动失败" : e.getMessage());
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeFrame(ImageProxy image) {
        try {
            if (!running) return;
            FrameStats stats = computeCenterStats(image);
            long now = System.currentTimeMillis();
            long elapsed = now - startMs;
            frameCount += 1;
            double fps = lastFrameMs > 0 ? 1000.0 / Math.max(1, now - lastFrameMs) : 0.0;
            lastFrameMs = now;
            recentRed.add(stats.red);
            if (recentRed.size() > 60) recentRed.removeFirst();

            String prompt = buildPrompt(stats);
            runOnUiThread(() -> {
                statusText.setText(prompt);
                metricsText.setText("red=" + round(stats.red) + " ratio=" + round(stats.redRatio) + " fps=" + round(fps));
            });

            Map<String, Object> data = new HashMap<>();
            data.put("type", "sample");
            data.put("time", elapsed / 1000.0);
            data.put("red", stats.red);
            data.put("green", stats.green);
            data.put("blue", stats.blue);
            data.put("brightness", stats.brightness);
            data.put("redRatio", stats.redRatio);
            data.put("sampleCount", frameCount);
            data.put("fps", fps);
            if (callback != null) callback.invokeAndKeepAlive(data);

            if (elapsed >= durationSeconds * 1000L) {
                emit("done", null);
                finishMeasureActivity();
            }
        } catch (Exception e) {
            emitError("ANALYZE_FAILED", e.getMessage() == null ? "帧分析失败" : e.getMessage());
        } finally {
            image.close();
        }
    }

    private String buildPrompt(FrameStats stats) {
        double redStd = redStd();
        if (stats.brightness < 35 || stats.red < 60) return "画面过暗，请确认闪光灯开启，并稍微放松手指";
        if (stats.red > 245 || stats.brightness > 240) return "画面过亮，请稍微移动手指，避免完全贴死镜头";
        if (stats.redRatio < 0.42) return "未检测到手指，请让手指同时覆盖摄像头和闪光灯";
        if (redStd > 18) return "信号不稳定，请保持手指不动";
        if (recentRed.size() > 20 && redStd < 0.3) return "波动过弱，请稍微调整手指位置";
        return "信号良好，请保持不动，正在测量";
    }

    private double redStd() {
        if (recentRed.isEmpty()) return 0;
        double sum = 0;
        for (Double value : recentRed) sum += value;
        double mean = sum / recentRed.size();
        double variance = 0;
        for (Double value : recentRed) variance += Math.pow(value - mean, 2);
        return Math.sqrt(variance / recentRed.size());
    }

    private FrameStats computeCenterStats(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        int startX = (int) (width * 0.35);
        int endX = (int) (width * 0.65);
        int startY = (int) (height * 0.35);
        int endY = (int) (height * 0.65);
        long rSum = 0, gSum = 0, bSum = 0;
        int count = 0;

        for (int y = startY; y < endY; y += 2) {
            for (int x = startX; x < endX; x += 2) {
                int index = (y * width + x) * 4;
                if (index + 3 < buffer.limit()) {
                    // CameraX RGBA_8888 在部分环境为 A/R/G/B 顺序；如真机 red 不正确，可调整此处索引。
                    int r = buffer.get(index + 1) & 0xff;
                    int g = buffer.get(index + 2) & 0xff;
                    int b = buffer.get(index + 3) & 0xff;
                    rSum += r;
                    gSum += g;
                    bSum += b;
                    count += 1;
                }
            }
        }

        if (count <= 0) return new FrameStats(0, 0, 0, 0, 0);
        double r = rSum * 1.0 / count;
        double g = gSum * 1.0 / count;
        double b = bSum * 1.0 / count;
        double brightness = (r + g + b) / 3.0;
        double redRatio = r / (r + g + b + 1.0);
        return new FrameStats(r, g, b, brightness, redRatio);
    }

    private void emit(String type, Map<String, Object> extra) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        if (extra != null) data.putAll(extra);
        if (callback != null) callback.invokeAndKeepAlive(data);
    }

    private void emitError(String code, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "error");
        data.put("code", code);
        data.put("message", message);
        if (callback != null) callback.invoke(data);
    }

    private String round(double value) {
        return String.valueOf(Math.round(value * 10.0) / 10.0);
    }

    private void finishMeasureActivity() {
        running = false;
        try {
            if (camera != null) camera.getCameraControl().enableTorch(false);
            if (cameraProvider != null) cameraProvider.unbindAll();
        } catch (Exception ignored) {
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        running = false;
        try {
            if (camera != null) camera.getCameraControl().enableTorch(false);
            if (cameraProvider != null) cameraProvider.unbindAll();
            if (cameraExecutor != null) cameraExecutor.shutdown();
        } catch (Exception ignored) {
        }
        if (current == this) current = null;
        super.onDestroy();
    }

    static class FrameStats {
        final double red;
        final double green;
        final double blue;
        final double brightness;
        final double redRatio;

        FrameStats(double red, double green, double blue, double brightness, double redRatio) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.brightness = brightness;
            this.redRatio = redRatio;
        }
    }
}
