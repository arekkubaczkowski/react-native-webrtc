#import "CaptureController.h"
#import "VideoEffectProcessor.h"
#import "WebRTCModule.h"

@interface WebRTCModule (RTCMediaStream)

@property(nonatomic, strong) VideoEffectProcessor *videoEffectProcessor;

- (RTCVideoTrack *)createVideoTrackWithCaptureController:
    (CaptureController * (^)(RTCVideoSource *))captureControllerCreator;
- (NSArray *)createMediaStream:(NSArray<RTCMediaStreamTrack *> *)tracks;

- (RTCMediaStreamTrack *)trackForId:(nonnull NSString *)trackId pcId:(nonnull NSNumber *)pcId;

/**
 * Sets a video frame processor directly on a local video track.
 * This bypasses ProcessorProvider and allows external modules to inject
 * processors without string-based registration.
 *
 * @param processor The processor to attach, or nil to remove processing.
 * @param trackId The local video track ID.
 */
- (void)setVideoFrameProcessor:(nullable NSObject<VideoFrameProcessorDelegate> *)processor
                    forTrackId:(nonnull NSString *)trackId;

@end