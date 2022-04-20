
import 'dart:async';

import 'package:flutter/services.dart';

class IotWordCard {
  static const MethodChannel _channel =
      const MethodChannel('iot_word_card');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
