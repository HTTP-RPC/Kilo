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

@interface UIButton (Markup)

/**
 * Creates a system button.
 */
+ (UIButton *)systemButton;

/**
 * Creates a detail disclosure button.
 */
+ (UIButton *)detailDisclosureButton;

/**
 * Creates a light info button.
 */
+ (UIButton *)infoLightButton;

/**
 * Creates a dark info button.
 */
+ (UIButton *)infoDarkButton;

/**
 * Creates an "add contact" button.
 */
+ (UIButton *)contactAddButton;

/**
 * The button's title.
 */
@property (nonatomic, nullable) NSString *title;

/**
 * The button's title color.
 */
@property (nonatomic, nullable) UIColor *titleColor UI_APPEARANCE_SELECTOR;

/**
 * The button's title shadow color.
 */
@property (nonatomic, nullable) UIColor *titleShadowColor UI_APPEARANCE_SELECTOR;

/**
 * The button's attributed title.
 */
@property (nonatomic, nullable) NSAttributedString *attributedTitle;

/**
 * The button's image.
 */
@property (nonatomic, nullable) UIImage *image;

/**
 * The button's background image.
 */
@property (nonatomic, nullable) UIImage *backgroundImage UI_APPEARANCE_SELECTOR;

/**
 * The top content edge inset.
 */
@property (nonatomic) CGFloat contentEdgeInsetTop UI_APPEARANCE_SELECTOR;

/**
 * The left content edge inset.
 */
@property (nonatomic) CGFloat contentEdgeInsetLeft UI_APPEARANCE_SELECTOR;

/**
 * The bottom content edge inset.
 */
@property (nonatomic) CGFloat contentEdgeInsetBottom UI_APPEARANCE_SELECTOR;

/**
 * The right content edge inset.
 */
@property (nonatomic) CGFloat contentEdgeInsetRight UI_APPEARANCE_SELECTOR;

/**
 * The top title edge inset.
 */
@property (nonatomic) CGFloat titleEdgeInsetTop;

/**
 * The left title edge inset.
 */
@property (nonatomic) CGFloat titleEdgeInsetLeft;

/**
 * The bottom title edge inset.
 */
@property (nonatomic) CGFloat titleEdgeInsetBottom;

/**
 * The right title edge inset.
 */
@property (nonatomic) CGFloat titleEdgeInsetRight;

/**
 * The top image edge inset.
 */
@property (nonatomic) CGFloat imageEdgeInsetTop;

/**
 * The left image edge inset.
 */
@property (nonatomic) CGFloat imageEdgeInsetLeft;

/**
 * The bottom image edge inset.
 */
@property (nonatomic) CGFloat imageEdgeInsetBottom;

/**
 * The right image edge inset.
 */
@property (nonatomic) CGFloat imageEdgeInsetRight;

@end

NS_ASSUME_NONNULL_END
