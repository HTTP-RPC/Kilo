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

/**
 * Abstract base class for layout views.
 */
@interface LMLayoutView : UIView
{
    NSMutableArray *_arrangedSubviews;
}

/**
 * The list of subviews whose sizes and positions are managed by the layout view.
 */
@property (nonatomic, readonly, copy) NSArray<UIView *> *arrangedSubviews;

/**
 * Adds an arranged subview.
 * 
 * @param view The view to add.
 */
- (void)addArrangedSubview:(UIView *)view;

/**
 * Inserts an arranged subview.
 *
 * @param view The view to insert.
 * @param index The index at which to insert the view.
 */
- (void)insertArrangedSubview:(UIView *)view atIndex:(NSUInteger)index;

/**
 * Removes an arranged subview.
 *
 * @param view The view to remove.
 */
- (void)removeArrangedSubview:(UIView *)view;

/**
 * Specifies that subviews will be arranged relative to the view's layout margins.
 * The default value is <code>YES</code>.
 */
@property (nonatomic) BOOL layoutMarginsRelativeArrangement;

@end

NS_ASSUME_NONNULL_END

