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

@interface UIScrollView (Markup)

/**
 * The top content inset.
 */
@property (nonatomic) CGFloat contentInsetTop;

/**
 * The left content inset.
 */
@property (nonatomic) CGFloat contentInsetLeft;

/**
 * The bottom content inset.
 */
@property (nonatomic) CGFloat contentInsetBottom;

/**
 * The right content inset.
 */
@property (nonatomic) CGFloat contentInsetRight;

#if TARGET_OS_IOS
/**
 * The index of the current page.
 */
@property (nonatomic) NSInteger currentPage;

/**
 * Sets the current page.
 *
 * @param currentPage The page index.
 * @param animated <code>YES</code> if the transition should be animated; <code>NO</code>, otherwise.
 */
- (void)setCurrentPage:(NSInteger)currentPage animated:(BOOL)animated;
#endif

@end

NS_ASSUME_NONNULL_END
