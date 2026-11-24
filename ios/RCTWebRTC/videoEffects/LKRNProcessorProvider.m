#import "LKRNProcessorProvider.h"

@implementation LKRNProcessorProvider

static NSMutableDictionary<NSString *, NSObject<LKRNVideoFrameProcessorDelegate> *> *processorMap;
static dispatch_queue_t processorMapQueue;

+ (void)initialize {
    processorMap = [[NSMutableDictionary alloc] init];
    processorMapQueue = dispatch_queue_create("com.webrtc.processorprovider", DISPATCH_QUEUE_CONCURRENT);
}

+ (NSObject<LKRNVideoFrameProcessorDelegate> *)getProcessor:(NSString *)name {
    __block NSObject<LKRNVideoFrameProcessorDelegate> *result = nil;
    dispatch_sync(processorMapQueue, ^{
        result = [processorMap objectForKey:name];
    });
    return result;
}

+ (void)addProcessor:(NSObject<LKRNVideoFrameProcessorDelegate> *)processor forName:(NSString *)name {
    dispatch_barrier_sync(processorMapQueue, ^{
        [processorMap setObject:processor forKey:name];
    });
}

+ (void)removeProcessor:(NSString *)name {
    dispatch_barrier_sync(processorMapQueue, ^{
        [processorMap removeObjectForKey:name];
    });
}

@end
