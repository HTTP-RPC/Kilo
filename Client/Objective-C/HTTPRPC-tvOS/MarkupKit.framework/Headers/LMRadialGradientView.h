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
 * Radial gradient view.
 */
@interface LMRadialGradientView : LMGradientView

/**
 * The center x-coordinate.
 */
@property (nonatomic) CGFloat centerX;

/**
 * The center y-coordinate.
 */
@property (nonatomic) CGFloat centerY;

/**
 * The center point of the gradient.
 */
@property (nonatomic) CGPoint centerPoint;

/**
 * The radius of the gradient.
 */
@property (nonatomic) CGFloat radius;

@end

NS_ASSUME_NONNULL_END
