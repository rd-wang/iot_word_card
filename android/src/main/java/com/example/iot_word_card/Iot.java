package com.example.iot_word_card;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.aliyun.alink.dm.api.DeviceInfo;
import com.aliyun.alink.dm.model.ResponseModel;
import com.aliyun.alink.linkkit.api.ILinkKitConnectListener;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linkkit.api.LinkKitInitParams;
import com.aliyun.alink.linksdk.channel.core.base.IOnCallListener;
import com.aliyun.alink.linksdk.channel.core.persistent.mqtt.MqttConfigure;
import com.aliyun.alink.linksdk.channel.core.persistent.mqtt.MqttInitParams;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttPublishRequest;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttRrpcRegisterRequest;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttSubscribeRequest;
import com.aliyun.alink.linksdk.cmp.core.base.AMessage;
import com.aliyun.alink.linksdk.cmp.core.base.ARequest;
import com.aliyun.alink.linksdk.cmp.core.base.AResponse;
import com.aliyun.alink.linksdk.cmp.core.base.ConnectState;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectNotifyListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSubscribeListener;
import com.aliyun.alink.linksdk.id2.Id2ItlsSdk;
import com.aliyun.alink.linksdk.tools.AError;
import com.aliyun.alink.linksdk.tools.ALog;
import com.aliyun.alink.linksdk.tools.ThreadTools;
import com.aliyun.alink.linksdk.tools.log.IDGenerater;
import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

import java.util.Map;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class Iot {
    private static final String TAG = "Iot";


    /**
     * ???????????????????????????
     * ????????????????????????????????????????????????????????????
     */
    public static boolean isInitDone = false;


    /**
     * ?????????
     * ???????????????????????????????????????
     */
    public static void connect(final Context context, @NonNull MethodCall call,
                               @NonNull MethodChannel.Result result, EventChannel.EventSink eventSink) {
        AppLog.d(TAG, "connect() called");
        // SDK?????????
        new Thread(new Runnable() {
            @Override
            public void run() {
                // ???????????????????????????
                DeviceInfo deviceInfo = new DeviceInfo();
                // ????????????
                deviceInfo.productKey = (String) call.argument("productKey");
                // ????????????
                deviceInfo.deviceName = (String) call.argument("deviceName");
                // ????????????
                deviceInfo.deviceSecret = (String) call.argument("deviceSecret");
                // ????????????
                deviceInfo.productSecret = (String) call.argument("productSecret");

                final LinkKitInitParams params = new LinkKitInitParams();
                params.deviceInfo = deviceInfo;

                LinkKit.getInstance().registerOnPushListener(new IConnectNotifyListener() {
                    /**
                     * onNotify ????????????????????? shouldHandle ???????????????????????????topic
                     * @param connectId ??????????????????????????????????????? connectId == ConnectSDK.getInstance().getPersistentConnectId()
                     * @param topic ?????????topic
                     * @param aMessage ?????????????????????
                     */
                    @Override
                    public void onNotify(String connectId, String topic, AMessage aMessage) {
                        String data = new String((byte[]) aMessage.data);
                        // ???????????????????????????  data = {"method":"thing.service.test_service","id":"123374967","params":{"vv":60},"version":"1.0.0"}
                        AppLog.d(TAG, "onNotify() called with: connectId = [" + connectId + "], topic = [" + topic + "], aMessage = [" + data + "]");
                        AppLog.i(TAG, "?????????????????????topic=" + topic + ",data=" + data);

                        eventSink.success(new IotMessage(200100, new Gson().fromJson(data, Message.class)).toJson());
                    }

                    /**
                     * @param connectId ??????????????????????????????????????? connectId == ConnectSDK.getInstance().getPersistentConnectId()
                     * @param topic ??????topic
                     * @return ?????????????????????topic????????????true??????????????????onNotify????????????false???onNotify??????????????????topic?????????????????????????????????true???
                     */
                    @Override
                    public boolean shouldHandle(String connectId, String topic) {
                        return true;
                    }

                    /**
                     * @param connectId ??????????????????????????????????????? connectId == ConnectSDK.getInstance().getPersistentConnectId()
                     * @param connectState {@link ConnectState}
                     *     CONNECTED, ????????????
                     *     DISCONNECTED, ?????????
                     *     CONNECTING, ?????????
                     *     CONNECTFAIL; ????????????
                     */
                    @Override
                    public void onConnectStateChange(String connectId, ConnectState connectState) {
                        AppLog.d(TAG, "onConnectStateChange() called with: connectId = [" + connectId + "], connectState = [" + connectState + "]");
                        switch (connectState) {
                            case CONNECTED:
                                eventSink.success(new IotMessage(100100).toJson());
                                break;
                            case CONNECTING:
                                eventSink.success(new IotMessage(100101).toJson());
                                break;
                            case CONNECTFAIL:
                                eventSink.success(new IotMessage(100102).toJson());
                                break;
                            case DISCONNECTED:
                                eventSink.success(new IotMessage(100103).toJson());
                                break;
                        }

                    }
                });

                LinkKit.getInstance().init(context, params, new ILinkKitConnectListener() {
                    @Override
                    public void onError(AError error) {
                        AppLog.d(TAG, "onError() called with: aError = [" + getAErrorString(error) + "]");
                        // ????????????????????????????????????????????????????????????????????????
                        // ?????????????????????????????????????????????????????????????????????????????????????????????
                        if (error != null) {
                            AppLog.d(TAG, "?????????????????????????????????" + error.getCode() + "-" + error.getSubCode() + ", " + error.getMsg());
                            result.success(new IotMessage(200200, error.getCode() + ":" + error.getSubCode() + ":" + error.getMsg()).toJson());
                        } else {
                            AppLog.d(TAG, "???????????????");
                            result.success(new IotMessage(-200200, "LinkKit init error but AError is null").toJson());
                        }
                    }

                    @Override
                    public void onInitDone(Object data) {
                        AppLog.d(TAG, "onInitDone() called with: data = [" + data + "]");
                        isInitDone = true;
                        result.success(new IotMessage(200200, "LinkKit init .. onInitDone").toJson());
                    }
                });
            }
        }).start();
    }


    public static String getAErrorString(AError error) {
        if (error == null) {
            return null;
        }
        return JSONObject.toJSONString(error);
    }

    /**
     * ???????????????????????????????????????
     * ????????????????????????
     */
    public void deinit() {
        AppLog.d(TAG, "deinit");
        isInitDone = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // ????????????
                LinkKit.getInstance().deinit();
//                showToast("??????????????????");
                AppLog.d(TAG, "??????????????????");
            }
        }).start();
    }

    /**
     * @param topic "/" + productKey + "/" + deviceName + "/user/xxx
     */
    public static void subscribe(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        MqttSubscribeRequest subscribeRequest = new MqttSubscribeRequest();
        subscribeRequest.isSubscribe = true;
        subscribeRequest.topic = call.argument("topic");
        subscribeRequest.qos = 0;
        LinkKit.getInstance().subscribe(subscribeRequest, new IConnectSubscribeListener() {
            @Override
            public void onSuccess() {
                AppLog.d(TAG, "????????????");
                result.success(new IotMessage(200202, "????????????").toJson());

            }

            @Override
            public void onFailure(AError error) {
                AppLog.d(TAG, "????????????");
                result.success(new IotMessage(-200202, error.getCode() + ":" + error.getSubCode() + "" + error.getMsg()).toJson());
            }
        });

    }

    public static void publish(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        MqttPublishRequest request = new MqttPublishRequest();
        request.qos = 0;
        request.isRPC = false;
        request.topic = call.argument("topic");
        request.msgId = String.valueOf(IDGenerater.generateId());
        request.payloadObj = call.argument("data");
        LinkKit.getInstance().publish(request, new IConnectSendListener() {
            @Override
            public void onResponse(ARequest aRequest, AResponse aResponse) {
                AppLog.d(TAG, "onResponse() called with: aRequest = [" + aRequest + "], aResponse = [" + (aResponse == null ? null : aResponse.data) + "]");
                if (aRequest instanceof MqttPublishRequest) {
                    AppLog.d(TAG, ((MqttPublishRequest) aRequest).topic + "??????");
                    if (((MqttPublishRequest) aRequest).topic != null && ((MqttPublishRequest) aRequest).topic.contains("thing/reset")) {
                        isInitDone = false;
                    }
                    return;
                }

                AppLog.d(TAG, "????????????");
                result.success(new IotMessage(200201, "????????????").toJson());
            }

            @Override
            public void onFailure(ARequest aRequest, AError aError) {
                AppLog.d(TAG, "onFailure() called with: aRequest = [" + aRequest + "], aError = [" + aError + "]");
                if (aRequest instanceof MqttPublishRequest) {
                    AppLog.d(TAG, ((MqttPublishRequest) aRequest).topic + "??????");
                    if (((MqttPublishRequest) aRequest).topic != null && ((MqttPublishRequest) aRequest).topic.contains("thing/reset")) {
                        isInitDone = false;
                    }
                    return;
                }
                AppLog.d(TAG, "????????????");
                result.success(new IotMessage(-200201, aError.getCode() + ":" + aError.getSubCode() + "" + aError.getMsg()).toJson());
            }
        });

    }
}