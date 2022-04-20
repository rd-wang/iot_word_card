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
     * 判断是否初始化完成
     * 未初始化完成，所有和云端的长链通信都不通
     */
    public static boolean isInitDone = false;


    /**
     * 初始化
     * 耗时操作，建议放到异步线程
     */
    public static void connect(final Context context, @NonNull MethodCall call,
                               @NonNull MethodChannel.Result result, EventChannel.EventSink eventSink) {
        AppLog.d(TAG, "connect() called");
        // SDK初始化
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 构造三元组信息对象
                DeviceInfo deviceInfo = new DeviceInfo();
                // 产品类型
                deviceInfo.productKey = (String) call.argument("productKey");
                // 设备名称
                deviceInfo.deviceName = (String) call.argument("deviceName");
                // 设备密钥
                deviceInfo.deviceSecret = (String) call.argument("deviceSecret");
                // 产品密钥
                deviceInfo.productSecret = (String) call.argument("productSecret");

                final LinkKitInitParams params = new LinkKitInitParams();
                params.deviceInfo = deviceInfo;

                LinkKit.getInstance().registerOnPushListener(new IConnectNotifyListener() {
                    /**
                     * onNotify 会触发的前提是 shouldHandle 没有指定不处理这个topic
                     * @param connectId 连接类型，这里判断是否长链 connectId == ConnectSDK.getInstance().getPersistentConnectId()
                     * @param topic 下行的topic
                     * @param aMessage 下行的数据内容
                     */
                    @Override
                    public void onNotify(String connectId, String topic, AMessage aMessage) {
                        String data = new String((byte[]) aMessage.data);
                        // 服务端返回数据示例  data = {"method":"thing.service.test_service","id":"123374967","params":{"vv":60},"version":"1.0.0"}
                        AppLog.d(TAG, "onNotify() called with: connectId = [" + connectId + "], topic = [" + topic + "], aMessage = [" + data + "]");
                        AppLog.i(TAG, "收到云端下行：topic=" + topic + ",data=" + data);

                        eventSink.success(new IotMessage(200100, new Gson().fromJson(data, Message.class)));
                    }

                    /**
                     * @param connectId 连接类型，这里判断是否长链 connectId == ConnectSDK.getInstance().getPersistentConnectId()
                     * @param topic 下行topic
                     * @return 是否要处理这个topic，如果为true，则会回调到onNotify；如果为false，onNotify不会回调这个topic相关的数据。建议默认为true。
                     */
                    @Override
                    public boolean shouldHandle(String connectId, String topic) {
                        return true;
                    }

                    /**
                     * @param connectId 连接类型，这里判断是否长链 connectId == ConnectSDK.getInstance().getPersistentConnectId()
                     * @param connectState {@link ConnectState}
                     *     CONNECTED, 连接成功
                     *     DISCONNECTED, 已断链
                     *     CONNECTING, 连接中
                     *     CONNECTFAIL; 连接失败
                     */
                    @Override
                    public void onConnectStateChange(String connectId, ConnectState connectState) {
                        AppLog.d(TAG, "onConnectStateChange() called with: connectId = [" + connectId + "], connectState = [" + connectState + "]");
                        switch (connectState) {
                            case CONNECTED:
                                eventSink.success(new IotMessage(100100));
                                break;
                            case CONNECTING:
                                eventSink.success(new IotMessage(100101));
                                break;
                            case CONNECTFAIL:
                                eventSink.success(new IotMessage(100102));
                                break;
                            case DISCONNECTED:
                                eventSink.success(new IotMessage(100103));
                                break;
                        }

                    }
                });

                LinkKit.getInstance().init(context, params, new ILinkKitConnectListener() {
                    @Override
                    public void onError(AError error) {
                        AppLog.d(TAG, "onError() called with: aError = [" + getAErrorString(error) + "]");
                        // 初始化失败，初始化失败之后需要用户负责重新初始化
                        // 如一开始网络不通导致初始化失败，后续网络恢复之后需要重新初始化
                        if (error != null) {
                            AppLog.d(TAG, "初始化失败，错误信息：" + error.getCode() + "-" + error.getSubCode() + ", " + error.getMsg());
                            result.success(new IotMessage(200200, error.getCode() + ":" + error.getSubCode() + ":" + error.getMsg()));
                        } else {
                            AppLog.d(TAG, "初始化失败");
                            result.success(new IotMessage(-200200, "LinkKit init error but AError is null"));
                        }
                    }

                    @Override
                    public void onInitDone(Object data) {
                        AppLog.d(TAG, "onInitDone() called with: data = [" + data + "]");
                        isInitDone = true;
                        result.success(new IotMessage(200200, "LinkKit init .. onInitDone"));
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
     * 耗时操作，建议放到异步线程
     * 反初始化同步接口
     */
    public void deinit() {
        AppLog.d(TAG, "deinit");
        isInitDone = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 同步接口
                LinkKit.getInstance().deinit();
//                showToast("反初始化成功");
                AppLog.d(TAG, "反初始化成功");
            }
        }).start();
    }

    /**
     * @param topic "/" + productKey + "/" + deviceName + "/user/xxx
     */
    public static void subscribe(String topic, @NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        MqttSubscribeRequest subscribeRequest = new MqttSubscribeRequest();
        subscribeRequest.isSubscribe = true;
        subscribeRequest.topic = topic;
        subscribeRequest.qos = 0;
        LinkKit.getInstance().subscribe(subscribeRequest, new IConnectSubscribeListener() {
            @Override
            public void onSuccess() {
                AppLog.d(TAG, "订阅成功");
                result.success(new IotMessage(200202, "订阅成功"));

            }

            @Override
            public void onFailure(AError error) {
                AppLog.d(TAG, "订阅失败");
                result.success(new IotMessage(-200202, error.getCode() + ":" + error.getSubCode() + "" + error.getMsg()));
            }
        });

    }

    public static void publish(String topic, JSONObject publishObject, @NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        MqttPublishRequest request = new MqttPublishRequest();
        request.qos = 0;
        request.isRPC = false;
        request.topic = topic;
        request.msgId = String.valueOf(IDGenerater.generateId());
        request.payloadObj = publishObject.toString();
        LinkKit.getInstance().publish(request, new IConnectSendListener() {
            @Override
            public void onResponse(ARequest aRequest, AResponse aResponse) {
                AppLog.d(TAG, "onResponse() called with: aRequest = [" + aRequest + "], aResponse = [" + (aResponse == null ? null : aResponse.data) + "]");
                if (aRequest instanceof MqttPublishRequest) {
                    AppLog.d(TAG, ((MqttPublishRequest) aRequest).topic + "成功");
                    if (((MqttPublishRequest) aRequest).topic != null && ((MqttPublishRequest) aRequest).topic.contains("thing/reset")) {
                        isInitDone = false;
                    }
                    return;
                }

                AppLog.d(TAG, "请求成功");
                result.success(new IotMessage(200201, "发布成功"));
            }

            @Override
            public void onFailure(ARequest aRequest, AError aError) {
                AppLog.d(TAG, "onFailure() called with: aRequest = [" + aRequest + "], aError = [" + aError + "]");
                if (aRequest instanceof MqttPublishRequest) {
                    AppLog.d(TAG, ((MqttPublishRequest) aRequest).topic + "失败");
                    if (((MqttPublishRequest) aRequest).topic != null && ((MqttPublishRequest) aRequest).topic.contains("thing/reset")) {
                        isInitDone = false;
                    }
                    return;
                }
                AppLog.d(TAG, "请求失败");
                result.success(new IotMessage(-200201, aError.getCode() + ":" + aError.getSubCode() + "" + aError.getMsg()));
            }
        });

    }
}