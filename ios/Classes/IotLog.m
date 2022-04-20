//
//  IotLog.m
//  iot_word_card
//
//  Created by wangyazhou on 2022/4/20.
//

#import "IotLog.h"

@implementation IotLog
+ (void)iotSetAllTagsLevel:(IMSLogLevel) level {
    [IotLog iotSetAllTagsLevel:level];
}

+ (void)iotSetLevel:(IMSLogLevel)level forTag:(NSString *_Nonnull)tag {
    [IotLog iotSetLevel:level forTag:tag];
}

+ (void)iotShowInConsole:(BOOL)show {
    [IotLog iotShowInConsole:show];
}
@end
