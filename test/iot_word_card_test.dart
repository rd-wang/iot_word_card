import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:iot_word_card/iot_word_card.dart';

void main() {
  const MethodChannel channel = MethodChannel('iot_word_card');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

}
