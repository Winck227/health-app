package com.healthapp.heartrate;

import android.app.Activity;
import android.content.Intent;

import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

public class HeartRateCameraModule extends UniModule {

    @UniJSMethod(uiThread = true)
    public void isSupported(UniJSCallback callback) {
        Map<String, Object> result = new HashMap<>();
        result.put("supported", true);
        result.put("platform", "android");
        result.put("message", "CameraX ImageAnalysis native plugin");
        if (callback != null) callback.invoke(result);
    }

    @UniJSMethod(uiThread = true)
    public void start(JSONObject options, UniJSCallback callback) {
        Activity activity = (Activity) mUniSDKInstance.getContext();
        HeartRateCameraActivity.callback = callback;
        HeartRateCameraActivity.durationSeconds = options == null ? 90 : Math.max(10, options.getIntValue("durationSeconds"));
        HeartRateCameraActivity.torchEnabled = options == null || !options.containsKey("torch") || options.getBooleanValue("torch");
        Intent intent = new Intent(activity, HeartRateCameraActivity.class);
        activity.startActivity(intent);
    }

    @UniJSMethod(uiThread = true)
    public void stop() {
        HeartRateCameraActivity.stopFromModule();
    }

    @UniJSMethod(uiThread = true)
    public void setTorch(boolean enabled) {
        HeartRateCameraActivity.setTorchFromModule(enabled);
    }
}
