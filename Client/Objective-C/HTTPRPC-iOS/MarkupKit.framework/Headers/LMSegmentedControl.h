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

NS_ASSUME_NONNULL_BEGIN

@interface LMSegmentedControl : UISegmentedControl

/**
 * Inserts a segment with an associated value.
 *
 * @param title The segment title.
 * @param value The segment value.
 * @param segment The segment index.
 * @param animated <code>YES</code> if the insertion should be animated; <code>NO</code>, otherwise.
 */
- (void)insertSegmentWithTitle:(nullable NSString *)title value:(nullable id)value atIndex:(NSUInteger)segment animated:(BOOL)animated NS_REFINED_FOR_SWIFT;

/**
 * Inserts a segment with an associated value.
 *
 * @param image The segment image.
 * @param value The segment value.
 * @param segment The segment value.
 * @param animated <code>YES</code> if the insertion should be animated; <code>NO</code>, otherwise.
 */
- (void)insertSegmentWithImage:(nullable UIImage *)image value:(nullable id)value atIndex:(NSUInteger)segment animated:(BOOL)animated NS_REFINED_FOR_SWIFT;

@end

NS_ASSUME_NONNULL_END
