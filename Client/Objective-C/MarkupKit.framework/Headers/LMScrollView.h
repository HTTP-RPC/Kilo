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
 * Scroll view that automatically adapts to the size of its content.
 */
@interface LMScrollView : UIScrollView

/**
 * The scroll view's content view.
 */
@property (nonatomic) UIView *contentView;

/**
 * Indicates that the width of the scroll view's content should match the scroll view's width.
 * The default value is <code>NO</code>.
 */
@property (nonatomic) BOOL fitToWidth;

/**
 * Indicates that the height of the scroll view's content should match the scroll view's height.
 * The default value is <code>NO</code>.
 */
@property (nonatomic) BOOL fitToHeight;

@end

NS_ASSUME_NONNULL_END
