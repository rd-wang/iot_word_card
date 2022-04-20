package com.example.iot_word_card;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aliyun.alink.linksdk.tools.AError;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * IotWordCardPlugin
 */
public class IotWordCardPlugin implements FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    private Context context;

    private EventChannel eventChannel;
    private EventChannel.EventSink eventSink;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "iot_word_card");
        context = flutterPluginBinding.getApplicationContext();
        channel.setMethodCallHandler(this);
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "iot_word_card_event");
        eventChannel.setStreamHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "getSDKVersion":
                result.success("iot_1.0.0");
                break;
            case "init":
                Iot.connect(context, call, result, eventSink);
                break;
            case "subscribe":
                Iot.subscribe(call, result);
                break;
            case "publish":
                Iot.publish(call,result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        this.channel.setMethodCallHandler(null);
        this.channel = null;
        this.eventChannel.setStreamHandler(null);
        this.eventChannel = null;
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        eventSink = events;
    }

    @Override
    public void onCancel(Object arguments) {
        eventSink = null;
    }
}
