#import "ProcessorProvider.h"

@implementation ProcessorProvider

static NSMutableDictionary<NSString *, NSObject<VideoFrameProcessorDelegate> *> *processorMap;
static dispatch_queue_t processorMapQueue;

+ (void)initialize {
    processorMap = [[NSMutableDictionary alloc] init];
    processorMapQueue = dispatch_queue_create("com.webrtc.processorprovider", DISPATCH_QUEUE_CONCURRENT);
}

+ (NSObject<VideoFrameProcessorDelegate> *)getProcessor:(NSString *)name {
    __block NSObject<VideoFrameProcessorDelegate> *result = nil;
    dispatch_sync(processorMapQueue, ^{
        result = [processorMap objectForKey:name];
    });
    return result;
}

+ (void)addProcessor:(NSObject<VideoFrameProcessorDelegate> *)processor forName:(NSString *)name {
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
