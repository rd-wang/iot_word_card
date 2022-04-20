//
//  IotLog.h
//  iot_word_card
//
//  Created by wangyazhou on 2022/4/20.
//

#import <Foundation/Foundation.h>
#import <IotLog.h>

NS_ASSUME_NONNULL_BEGIN

@interface IotLog : NSObject
+ (void)iotSetAllTagsLevel:(IMSLogLevel) level;
+ (void)iotSetLevel:(IMSLogLevel)level forTag:(NSString *_Nonnull)tag;
+ (void)iotShowInConsole:(BOOL)show;

@end

NS_ASSUME_NONNULL_END
