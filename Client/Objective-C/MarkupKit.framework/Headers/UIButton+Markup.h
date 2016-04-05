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
 * Creates a custom button.
 */
+ (UIButton *)customButton;

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
 * The button's normal title.
 */
@property (nonatomic, nullable) NSString *normalTitle;

/**
 * The button's normal title color.
 */
@property (nonatomic, nullable) UIColor *normalTitleColor;

/**
 * The button's normal title shadow color.
 */
@property (nonatomic, nullable) UIColor *normalTitleShadowColor;

/**
 * The button's normal image.
 */
@property (nonatomic, nullable) UIImage *normalImage;

/**
 * The button's normal background image.
 */
@property (nonatomic, nullable) UIImage *normalBackgroundImage;

/**
 * The button's highlighted title.
 */
@property (nonatomic, nullable) NSString *highlightedTitle;

/**
 * The button's highlighted title color.
 */
@property (nonatomic, nullable) UIColor *highlightedTitleColor;

/**
 * The button's highlighted title shadow color.
 */
@property (nonatomic, nullable) UIColor *highlightedTitleShadowColor;

/**
 * The button's highlighted image.
 */
@property (nonatomic, nullable) UIImage *highlightedImage;

/**
 * The button's highlighted background image.
 */
@property (nonatomic, nullable) UIImage *highlightedBackgroundImage;

/**
 * The button's disabled title.
 */
@property (nonatomic, nullable) NSString *disabledTitle;

/**
 * The button's disabled title color.
 */
@property (nonatomic, nullable) UIColor *disabledTitleColor;

/**
 * The button's disabled title shadow color.
 */
@property (nonatomic, nullable) UIColor *disabledTitleShadowColor;

/**
 * The button's disabled image.
 */
@property (nonatomic, nullable) UIImage *disabledImage;

/**
 * The button's disabled background image.
 */
@property (nonatomic, nullable) UIImage *disabledBackgroundImage;

/**
 * The button's selected title.
 */
@property (nonatomic, nullable) NSString *selectedTitle;

/**
 * The button's selected title color.
 */
@property (nonatomic, nullable) UIColor *selectedTitleColor;

/**
 * The button's selected title shadow color.
 */
@property (nonatomic, nullable) UIColor *selectedTitleShadowColor;

/**
 * The button's selected image.
 */
@property (nonatomic, nullable) UIImage *selectedImage;

/**
 * The button's selected background image.
 */
@property (nonatomic, nullable) UIImage *selectedBackgroundImage;

/**
 * The top content edge inset.
 */
@property (nonatomic) CGFloat contentEdgeInsetTop;

/**
 * The left content edge inset.
 */
@property (nonatomic) CGFloat contentEdgeInsetLeft;

/**
 * The bottom content edge inset.
 */
@property (nonatomic) CGFloat contentEdgeInsetBottom;

/**
 * The right content edge inset.
 */
@property (nonatomic) CGFloat contentEdgeInsetRight;

@end

NS_ASSUME_NONNULL_END
