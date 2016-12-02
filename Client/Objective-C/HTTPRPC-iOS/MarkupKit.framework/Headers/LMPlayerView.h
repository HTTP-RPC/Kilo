//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

#import <UIKit/UIKit.h>

#import <AVFoundation/AVPlayerLayer.h>

NS_ASSUME_NONNULL_BEGIN

@protocol LMPlayerViewDelegate;

/**
 * View that presents an AV player.
 */
@interface LMPlayerView : UIView

/**
 * The view's layer as an AV player layer.
 */
@property (readonly, nonatomic) AVPlayerLayer *layer;

/**
 * The player view delegate.
 */
@property (weak, nonatomic) id<LMPlayerViewDelegate> delegate;

@end

/** 
 * Player view delegate protocol.
 */
@protocol LMPlayerViewDelegate <NSObject>

@optional

/**
 * Notifies the delegate that the player view's ready-for-display state changed.
 */
- (void)playerView:(LMPlayerView *)playerView isReadyForDisplay:(BOOL)readyForDisplay;

@end

NS_ASSUME_NONNULL_END