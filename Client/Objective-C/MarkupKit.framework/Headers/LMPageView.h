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

@interface LMPageView : UIScrollView

/**
 * The list of pages managed by the page view.
 */
@property (nonatomic, readonly, copy) NSArray<UIView *> *pages;

/**
 * Adds a page.
 * 
 * @param page The page to add.
 */
- (void)addPage:(UIView *)page;

/**
 * Inserts a page.
 *
 * @param page The page to insert.
 * @param index The index at which to insert the page.
 */
- (void)insertPage:(UIView *)page atIndex:(NSUInteger)index;

/**
 * Removes a page.
 *
 * @param page The page to remove.
 */
- (void)removePage:(UIView *)page;

@end

NS_ASSUME_NONNULL_END
