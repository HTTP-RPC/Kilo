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

@interface UICollectionViewFlowLayout (Markup)

/**
 * The item width.
 */
@property (nonatomic) CGFloat itemWidth;

/**
 * The item height.
 */
@property (nonatomic) CGFloat itemHeight;

/**
 * The estimated item width.
 */
@property (nonatomic) CGFloat estimatedItemWidth;

/**
 * The estimated item height.
 */
@property (nonatomic) CGFloat estimatedItemHeight;

/**
 * The top section inset.
 */
@property (nonatomic) CGFloat sectionInsetTop;

/**
 * The left section inset.
 */
@property (nonatomic) CGFloat sectionInsetLeft;

/**
 * The bottom section inset.
 */
@property (nonatomic) CGFloat sectionInsetBottom;

/**
 * The right section inset.
 */
@property (nonatomic) CGFloat sectionInsetRight;

/**
 * The header reference width.
 */
@property (nonatomic) CGFloat headerReferenceWidth;

/**
 * The header reference height.
 */
@property (nonatomic) CGFloat headerReferenceHeight;

/**
 * The footer reference width.
 */
@property (nonatomic) CGFloat footerReferenceWidth;

/**
 * The footer reference height.
 */
@property (nonatomic) CGFloat footerReferenceHeight;

@end

NS_ASSUME_NONNULL_END
