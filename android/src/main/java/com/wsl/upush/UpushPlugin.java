package com.wsl.upush;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.umeng.message.PushAgent;
import com.umeng.message.UTrack;
import com.umeng.message.common.inter.ITagManager;
import com.umeng.message.tag.TagManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * UpushPlugin
 */
public class UpushPlugin implements FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    public MethodChannel channel;
    private static final String TAG = "FlutterUpushPlugin";
    private PushAgent mPushAgent;
    public static UpushPlugin instance;
    private Handler handler = new Handler();

    @Override
    public void onAttachedToEngine(FlutterPluginBinding flutterPluginBinding) {
        Log.d(TAG, "*****************onAttachedToEngine****************");
        UpushPlugin mFlutterupushpluginPlugin = new UpushPlugin();
        mFlutterupushpluginPlugin.channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "upush");
        mFlutterupushpluginPlugin.channel.setMethodCallHandler(mFlutterupushpluginPlugin);
        instance = mFlutterupushpluginPlugin;
        mFlutterupushpluginPlugin.mPushAgent = PushAgent.getInstance(flutterPluginBinding.getApplicationContext());
    }


    public static void registerWith(Registrar registrar) {
        Log.d(TAG, "*****************registerWith****************");
        if (registrar.activity() != null) {
            UpushPlugin mFlutterupushpluginPlugin = new UpushPlugin();
            final MethodChannel channel = new MethodChannel(registrar.messenger(), "upush");
            channel.setMethodCallHandler(mFlutterupushpluginPlugin);
            mFlutterupushpluginPlugin.mPushAgent = PushAgent.getInstance(registrar.context().getApplicationContext());
            instance = mFlutterupushpluginPlugin;
        }
    }


    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

        String method = call.method;

        switch (method) {
            case "addTags":
                addTags(call, result);
                break;
            case "deleteTags":
                deleteTags(call, result);
                break;
            case "getTags":
                getTags(call, result);
                break;
            case "addAlias":
                addAlias(call, result);
                break;
            case "deleteAlias":
                deleteAlias(call, result);
                break;
            case "setAlias":
                setAlias(call, result);
                break;
            case "getRegistrationId":
                getRegistrationId(call, result);
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    /**
     * 添加标签 示例：将“标签1”绑定至该设备
     */
    private void addTags(MethodCall call, final Result result) {
        List<String> tagList = call.arguments();
        final String[] tags = tagList.toArray(new String[0]);

        UPushApplication.mPushAgent.getTagManager().addTags((isSuccess, ITagManager_result) -> {
            Log.d(TAG, "**********addTags:" + isSuccess + " " + ITagManager_result.toString());
            handler.post(() -> {
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("isSuccess", isSuccess);
                resultMap.put("message", ITagManager_result.msg);
                result.success(resultMap);
            });
        }, tags);
    }

    /**
     * 删除标签,将之前添加的标签中的一个或多个删除
     */
    private void deleteTags(MethodCall call, final Result result) {
        List<String> tagList = call.arguments();
        final String[] tags = tagList.toArray(new String[0]);

        UPushApplication.mPushAgent.getTagManager().deleteTags(new TagManager.TCallBack() {

            @Override
            public void onMessage(final boolean isSuccess, final ITagManager.Result ITagManagerresult) {
                handler.post(() -> {
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("isSuccess", isSuccess);
                    resultMap.put("message", ITagManagerresult.msg);
                    result.success(resultMap);
                });
            }
        }, tags);
    }

    /**
     * 离线消息的点击
     */
    public void onOffLineMsgClickHandler(final String json) {
        if (instance != null && !TextUtils.isEmpty(json)) {
            handler.post(() -> {
                if (channel != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("json", json);
                    Log.d(TAG, "**********onOffLineMsgClickHandler:" + json + " " + json.toString());
                    instance.channel.invokeMethod("onReceiveOffLineNotification", map);
                }
            });
        }
    }


    /**
     * 获取服务器端的所有标签
     */
    private void getTags(MethodCall call, final Result result) {
        UPushApplication.mPushAgent.getTagManager().getTags(new TagManager.TagListCallBack() {

            @Override
            public void onMessage(boolean isSuccess, final List<String> _result) {
                handler.post(() -> result.success(_result));
            }
        });
    }


    /**
     * 别名增加，将某一类型的别名ID绑定至某设备，老的绑定设备信息还在，别名ID和device_token是一对多的映射关系
     */
    private void addAlias(MethodCall call, final Result result) {
        Map<String,Object> map = call.arguments();
        String aliasId = String.valueOf(map.get("aliasId"));
        String aliasType = String.valueOf(map.get("aliasType"));
        mPushAgent.addAlias(aliasId, aliasType, new UTrack.ICallBack() {
            @Override
            public void onMessage(final boolean isSuccess, final String message) {
                handler.post(() -> {
                    Log.d(TAG, " isSuccess :" + isSuccess + " message:" + message);
                    Map<String, Object> resultMap = new HashMap<>();
                    resultMap.put("isSuccess", isSuccess);
                    resultMap.put("message", message);
                    result.success(resultMap);
                });
            }
        });
    }

    /**
     * //移除别名ID
     */
    private void deleteAlias(MethodCall call, final Result result) {
        Map<String,Object> map = call.arguments();
        String aliasId = String.valueOf(map.get("aliasId"));
        String aliasType = String.valueOf(map.get("aliasType"));
        mPushAgent.deleteAlias(aliasId, aliasType, new UTrack.ICallBack() {

            @Override
            public void onMessage(final boolean isSuccess, final String message) {
                handler.post(() -> {
                    Log.d(TAG, " isSuccess :" + isSuccess + " message:" + message);
                    Map<String,Object> resultMap = new HashMap<>();
                    resultMap.put("isSuccess", isSuccess);
                    resultMap.put("message", message);
                    result.success(resultMap);
                });
            }
        });
    }

    /**
     * //别名绑定，将某一类型的别名ID绑定至某设备，老的绑定设备信息被覆盖，别名ID和deviceToken是一对一的映射关系
     */
    private void setAlias(MethodCall call, final Result result) {
        Map<String,Object> map = call.arguments();
        String aliasId = String.valueOf(map.get("aliasId"));
        String aliasType = String.valueOf(map.get("aliasType"));
        mPushAgent.setAlias(aliasId, aliasType, new UTrack.ICallBack() {

            @Override
            public void onMessage(final boolean isSuccess, final String message) {
                handler.post(() -> {
                    Log.d(TAG, " isSuccess :" + isSuccess + " message:" + message);
                    Map<String,Object> resultMap = new HashMap<>();
                    resultMap.put("isSuccess", isSuccess);
                    resultMap.put("message", message);
                    result.success(resultMap);
                });
            }
        });
    }

    private void getRegistrationId(MethodCall call, final Result result) {
        result.success(mPushAgent.getRegistrationId());
    }
}
