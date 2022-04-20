import 'dart:async';

import 'package:flutter/services.dart';

typedef IotNotify = void Function(IotMessage iotMessage);

class IotWordCard {
  static const MethodChannel _channel = const MethodChannel('iot_word_card');
  static const EventChannel _eventChannel = const EventChannel('iot_word_card_event');

  static bool _eventChannelReadied = false;

  static Future<String> get sdkVersion async {
    final String sdkVersion = await _channel.invokeMethod('getSDKVersion');
    return sdkVersion;
  }

  static Future<String> init(IotConfig config) async {
    return await _channel.invokeMethod('init', config.toJson());
  }

  static Future<String> subscribe(String topic) async {
    return await _channel.invokeMethod('subscribe', {"topic", topic});
  }

  static Future<String> publish(String topic, String data) async {
    return await _channel.invokeMethod('publish', {"topic": topic, "data": data});
  }

  static Future<bool> listen(IotNotify onNotify) async {
    if (_eventChannelReadied != true) {
      _eventChannel.receiveBroadcastStream().listen(onNotify);
      _eventChannelReadied = true;
    }
    return true;
  }
}
//  onNotify(dynamic event) {
//
// }


class IotMessage {
  int code;
  int subCode;
  String desc;
  Message message;
}

class Message {
  String msgType;
  String from;
  String originalName;
  String targetName;
  String content;
  String electric;
  String version;
  String deviceId;

}

class IotConfig {
  String productKey;
  String deviceName;
  String deviceSecret;
  String productSecret;

  IotConfig(this.productKey,
      this.deviceName,
      this.deviceSecret,
      this.productSecret,);

  Map<String, dynamic> toJson() {
    Map<String, dynamic> jsonObject = Map<String, dynamic>();
    jsonObject.putIfAbsent("productKey", () => productKey);
    jsonObject.putIfAbsent("deviceName", () => deviceName);
    jsonObject.putIfAbsent("deviceSecret", () => deviceSecret);
    jsonObject.putIfAbsent("productSecret", () => productSecret);
    return jsonObject;
  }
}
