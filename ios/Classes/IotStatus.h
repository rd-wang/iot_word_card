//
//  IotStatus.h
//  iot_word_card
//
//  Created by wangyazhou on 2022/4/20.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

static const int IOT_CONNECTED          = 100100;// 链接成功
static const int IOT_CONNECTING         = 100101;//链接中
static const int IOT_CONNECTFAIL        = 100102;//链接失败
static const int IOT_DISCONNECTED       = 100103;
static const int IOT_MESSAGE            = 200100;//下行消息（标记）
static const int IOT_INIT_SUCCESS       = 200200;//初始化成功
static const int IOT_INIT_FAIL          = -200200;//初始化失败
static const int IOT_PUBLISH_SUCCESS    = 200201;//发布消息成功
static const int IOT_PUBLISH_FAIL       = -200201;//发布消息失败
static const int IOT_SUBSCRIBE_SUCCESS  = 200202;//订阅成功
static const int IOT_SUBSCRIBE_FAIL     = -200202;//订阅失败
    
@interface IotStatus : NSObject

@end

NS_ASSUME_NONNULL_END
