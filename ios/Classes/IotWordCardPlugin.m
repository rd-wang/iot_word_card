#import "IotWordCardPlugin.h"
#import <IotLinkKit/IotLinkKit.h>
#import <AlinkIoTExpress/AlinkIoTExpress.h>
#import "IotStatus.h"
#import "IotLog.h"

@interface IotWordCardPlugin()<LinkkitChannelListener,FlutterStreamHandler>
@property (nonatomic,copy) FlutterEventSink eventSink;
@end

@implementation IotWordCardPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"iot_word_card"
            binaryMessenger:[registrar messenger]];
  IotWordCardPlugin* instance = [[IotWordCardPlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
}

- (instancetype)initWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    if (self = [super init]) {
        [self setupEventChannel:registrar];
        return  self;
    }
    return  nil;
}

- (void)setupEventChannel:(NSObject<FlutterPluginRegistrar>*)registrar {
    FlutterEventChannel *channel = [FlutterEventChannel eventChannelWithName:@"iot_word_card_event" binaryMessenger:[registrar messenger]];
    [channel setStreamHandler:self];
}

- (FlutterError* _Nullable)onListenWithArguments:(id _Nullable)arguments eventSink:(FlutterEventSink)events {
    if (self.eventSink == nil) {
        self.eventSink = events;
        NSLog(@"FlutterEventChannel____注册成功");
    }
    return nil;
}

#pragma mark ----------------handleMethodCall

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"getPlatformVersion" isEqualToString:call.method]) {
    result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
      
  } else if ([call.method isEqualToString:@"init"]) {
      [self setIotLog];
      [self initIot:call result:result];
    
  } else if ([call.method isEqualToString:@"state"]) {
      
  } else if ([call.method isEqualToString:@"publish"]) {
      [self pushData:call result:result];
      
  } else if ([call.method isEqualToString:@"subscribe"]) {
      [self subscribeTopic:call result:result];
      
  } else {
    result(FlutterMethodNotImplemented);
  }
}

- (void)setIotLog {
    [IMSLog setAllTagsLevel:IMSLogLevelAll];
    //创建日志面板助手，注入到日志框架
    //[IMSLog addAssistant:[[IMSDashboardLogAssistant alloc] init]];
    //设置在控制台显示日志
    [IMSLog showInConsole:YES];
    //设置 Demo 的日志 tag
    [IMSLog registerTag:IMS_DEMO_TAG];
}

// 初始化
- (void)initIot:(FlutterMethodCall*)call result:(FlutterResult)result {
    [LinkKitEntry sharedKit];
    [[LinkKitEntry sharedKit] registerChannelListener:self];

    NSDictionary *param = call.arguments;
    
    LinkkitChannelConfig * channelConfig = [[LinkkitChannelConfig alloc] init];
    channelConfig.productKey = param[@"productKey"];
    channelConfig.deviceName = param[@"deviceName"];
    channelConfig.deviceSecret = param[@"deviceSecret"];
    channelConfig.cleanSession = 1;
    
    LinkkitSetupParams * setupParams = [[LinkkitSetupParams alloc] init];
//    setupParams.appVersion = self.appVersion;
    setupParams.channelConfig = channelConfig;
    [[LinkKitEntry sharedKit] setup:setupParams resultBlock:^(BOOL succeeded, NSError * _Nullable error) {
        LinkkitLogDebug(@"setup error : %@", error);
        dispatch_async(dispatch_get_main_queue(), ^{
            if (!succeeded) {
                result([self mapToString:@{@"code":@(IOT_INIT_FAIL),@"message":[self getError:error]}]);
                return;
            }
            result([self mapToString:@{@"code":@(IOT_INIT_SUCCESS)}]);
        });
    }];
}


// 订阅
- (void)subscribeTopic:(FlutterMethodCall*)call result:(FlutterResult)result {
    NSDictionary *param = call.arguments;
    NSString *topic = param[@"topic"];
    [[LinkKitEntry sharedKit] subscribe:topic
                            resultBlock:^(BOOL succeeded, NSError * _Nullable error) {
                                LinkkitLogDebug(@"kit subscribe error : %@", error);
                                dispatch_async(dispatch_get_main_queue(), ^{
                                    if (!succeeded) {
                                        result([self mapToString:@{@"code":@(IOT_SUBSCRIBE_FAIL),@"message":[self getError:error]}]);
                                        return;
                                    }
                                    result([self mapToString:@{@"code":@(IOT_SUBSCRIBE_SUCCESS)}]);
                                });
    }];
}

// 取消订阅
- (void)unSubscribeTopic:(FlutterMethodCall*)call result:(FlutterResult)result {
    NSString *topic = @"";
    [[LinkKitEntry sharedKit] unsubscribe:topic
                            resultBlock:^(BOOL succeeded, NSError * _Nullable error) {
                                LinkkitLogDebug(@"kit unsubscribe error : %@", error);
                                dispatch_async(dispatch_get_main_queue(), ^{

                                });
                            }];
}

// 上行数据
- (void)pushData:(FlutterMethodCall*)call result:(FlutterResult)result {
    NSDictionary *param = call.arguments;
    NSString * sUptopic = param[@"topic"] ;
    NSString * sUpcontent = param[@"data"];
    NSInteger sUpQos = 1;
    [[LKIoTExpress sharedInstance] uploadData:sUptopic
                                         data:[sUpcontent dataUsingEncoding:NSUTF8StringEncoding]
                                          qos:(int)sUpQos
                                     complete:^(NSError * _Nonnull err) {
        NSLog(@"%@", [NSString stringWithFormat:@"Linkkit 上行数据 : %@", err == nil ? @"成功" : @"失败"]);
        if (err == nil) {
            result([self mapToString:@{@"code":@(IOT_PUBLISH_SUCCESS)}]);
            return;
        }
        result([self mapToString:@{@"code":@(IOT_PUBLISH_FAIL),@"message":[self getError:err]}]);
    }];
}


#pragma mark -----------------------LinkkitChannelListener

- (void)onConnectStateChange:(nonnull NSString *)connectId state:(LinkkitChannelConnectState)state {
    NSString * connectTip = nil;
    int status = IOT_CONNECTED;
    if (state == LinkkitChannelStateConnected) {
        connectTip = @"已连接";
    } else if (state == LinkkitChannelStateDisconnected) {
        connectTip = @"未连接";
        status = IOT_CONNECTFAIL;
    } else {
        connectTip = @"连接中";
        status = IOT_CONNECTING;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        self.eventSink([self mapToString:@{@"code":@(status)}]);
    });
}

- (void)onNotify:(nonnull NSString *)connectId topic:(nonnull NSString *)topic data:(id _Nullable)data {
    NSString * downData = [NSString stringWithFormat:@"收到下推，topic : %@ \r\n", topic];
    downData = [downData stringByAppendingString:[NSString stringWithFormat:@"\r\n数据 : %@", data]];
    
    LinkkitLogDebug(@"kit recv  topic : %@", topic);
    
    dispatch_async(dispatch_get_main_queue(), ^{
        self.eventSink([self mapToString:@{@"code":@(IOT_MESSAGE),@"message":data}]);
    });
}

- (BOOL)shouldHandle:(nonnull NSString *)connectId topic:(nonnull NSString *)topic {
    return YES;
}

- (NSString *)mapToString:(NSDictionary *)map {
    NSData *data = [NSJSONSerialization dataWithJSONObject:map options:NSJSONWritingPrettyPrinted error:nil];
    return [[NSString alloc]initWithData:data encoding:NSUTF8StringEncoding];
}

- (NSDictionary *)getError:(NSError *)error {
    return @{@"errorCode":@(error.code),@"info":error.userInfo,@"des":error.localizedDescription};
}

@end
