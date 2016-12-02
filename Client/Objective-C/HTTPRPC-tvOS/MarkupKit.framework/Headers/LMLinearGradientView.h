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

#import "LMGradientView.h"

NS_ASSUME_NONNULL_BEGIN

/**
 * Linear gradient view.
 */
@interface LMLinearGradientView : LMGradientView

/**
 * The starting x-coordinate.
 */
@property (nonatomic) CGFloat startX;

/**
 * The starting y-coordinate.
 */
@property (nonatomic) CGFloat startY;

/**
 * The starting point of the gradient.
 */
@property (nonatomic) CGPoint startPoint;

/**
 * The ending x-coordinate.
 */
@property (nonatomic) CGFloat endX;

/**
 * The ending y-coordinate.
 */
@property (nonatomic) CGFloat endY;

/**
 * The ending point of the gradient.
 */
@property (nonatomic) CGPoint endPoint;

@end

NS_ASSUME_NONNULL_END
