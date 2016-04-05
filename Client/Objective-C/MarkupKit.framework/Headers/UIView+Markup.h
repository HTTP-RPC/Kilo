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

@interface UIView (Markup)

/**
 * The view's width, or <code>NaN</code> for no explicit width.
 */
@property (nonatomic) CGFloat width;

/**
 * The view's minimum width, or <code>NaN</code> for no explicit minimum width.
 */
@property (nonatomic) CGFloat minimumWidth;

/**
 * The view's maximum width, or <code>NaN</code> for no explicit maximum width.
 */
@property (nonatomic) CGFloat maximumWidth;

/**
 * The view's height, or <code>NaN</code> for no explicit height.
 */
@property (nonatomic) CGFloat height;

/**
 * The view's minimum height, or <code>NaN</code> for no explicit minimum height.
 */
@property (nonatomic) CGFloat minimumHeight;

/**
 * The view's maximum height, or <code>NaN</code> for no explicit maximum height.
 */
@property (nonatomic) CGFloat maximumHeight;

/**
 * The view's weight, or <code>NaN</code> for no weight.
 */
@property (nonatomic) CGFloat weight;

/**
 * The view's horizontal content compression resistance priority.
 */
@property (nonatomic) CGFloat horizontalContentCompressionResistancePriority;

/**
 * The view's horizontal content hugging priority.
 */
@property (nonatomic) CGFloat horizontalContentHuggingPriority;

/**
 * The view's vertical content compression resistance priority.
 */
@property (nonatomic) CGFloat verticalContentCompressionResistancePriority;

/**
 * The view's vertical content hugging priority.
 */
@property (nonatomic) CGFloat verticalContentHuggingPriority;

/**
 * The top layout margin.
 */
@property (nonatomic) CGFloat layoutMarginTop;

/**
 * The left layout margin.
 */
@property (nonatomic) CGFloat layoutMarginLeft;

/**
 * The bottom layout margin.
 */
@property (nonatomic) CGFloat layoutMarginBottom;

/**
 * The right layout margin.
 */
@property (nonatomic) CGFloat layoutMarginRight;

/**
 * Processes a markup instruction.
 *
 * @param target The markup instruction's target.
 * @param data The markup instruction's data.
 */
- (void)processMarkupInstruction:(NSString *)target data:(NSString *)data;

/**
 * Processes a markup element.
 *
 * @param tag The element's tag.
 * @param properties The element's properties.
 */
- (void)processMarkupElement:(NSString *)tag properties:(NSDictionary *)properties;

/**
 * Appends a markup element view.
 *
 * @param view The view to append.
 */
- (void)appendMarkupElementView:(UIView *)view;

@end

NS_ASSUME_NONNULL_END
