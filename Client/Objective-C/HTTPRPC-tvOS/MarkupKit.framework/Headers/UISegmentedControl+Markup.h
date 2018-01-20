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

@interface UISegmentedControl (Markup)

/**
 * Returns the value for the given segment.
 *
 * @param segment The segment index.
 */
- (nullable id)valueForSegmentAtIndex:(NSUInteger)segment NS_REFINED_FOR_SWIFT;

/**
 * Sets the value for the given segment.
 *
 * @param value The segment value.
 * @param segment The segment index.
 */
- (void)setValue:(nullable id)value forSegmentAtIndex:(NSUInteger)segment NS_REFINED_FOR_SWIFT;

/**
 * The value associated with the selected segment.
 */
@property (nonatomic, nullable) id value;

@end

NS_ASSUME_NONNULL_END
