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

@interface UIResponder (Markup)

/**
 * Returns the bundle that will be used to load the view document.
 */
- (NSBundle *)bundleForView;

/**
 * Returns the bundle that will be used to load images.
 */
- (NSBundle *)bundleForImages;

/**
 * Returns the bundle that will be used to localize string values.
 */
- (NSBundle *)bundleForStrings;

/**
 * Returns the name of the string table that will be used to localize string values.
 */
- (nullable NSString *)tableForStrings;

/**
 * Returns a named formatter, or <code>nil</code> if no formatter with the given name exists.
 *
 * @param name The formatter name.
 * @param arguments The formatter arguments.
 */
- (nullable NSFormatter *)formatterWithName:(NSString *)name arguments:(NSDictionary<NSString *, id> *)arguments;

/**
 * Establishes a binding between this object and a view instance.
 *
 * @param expression An expression representing the binding source.
 * @param view The target view.
 * @param keyPath The key path of a property in the view to which the expression will be bound.
 */
- (void)bind:(NSString *)expression toView:(UIView *)view withKeyPath:(NSString *)keyPath;

/**
 * Releases all bindings.
 */
- (void)unbindAll;

@end

/**
 * Anchor options.
 */
typedef NS_OPTIONS(NSUInteger, LMAnchor) {
    /** No anchor. */
    LMAnchorNone = 0,

    /** Top anchor. */
    LMAnchorTop = 1 << 0,

    /** Bottom anchor. */
    LMAnchorBottom = 1 << 1,

    /** Left anchor. */
    LMAnchorLeft = 1 << 2,

    /** Right anchor. */
    LMAnchorRight = 1 << 3,

    /** Leading anchor. */
    LMAnchorLeading = 1 << 4,

    /** Trailing anchor. */
    LMAnchorTrailing = 1 << 5,

    /** All anchors. */
    LMAnchorAll = LMAnchorTop | LMAnchorBottom | LMAnchorLeading | LMAnchorTrailing
};

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
 * The view's aspect ratio, or <code>NaN</code> for no explicit aspect ratio.
 */
@property (nonatomic) CGFloat aspectRatio;

/**
 * The view's weight, or <code>NaN</code> for no weight.
 */
@property (nonatomic) CGFloat weight;

/**
 * The view's anchors.
 */
@property (nonatomic) LMAnchor anchor;

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
 * The leading layout margin.
 */
@property (nonatomic) CGFloat layoutMarginLeading;

/**
 * The trailing layout margin.
 */
@property (nonatomic) CGFloat layoutMarginTrailing;

/**
 * The amount of space to reserve above the view. The default is 0.
 */
@property (nonatomic) CGFloat topSpacing;

/**
 * The amount of space to reserve below the view. The default is 0.
 */
@property (nonatomic) CGFloat bottomSpacing;

/**
 * The amount of space to reserve at the view's leading edge. The default is 0.
 */
@property (nonatomic) CGFloat leadingSpacing;

/**
 * The amount of space to reserve at the view's trailing edge. The default is 0.
 */
@property (nonatomic) CGFloat trailingSpacing;

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
- (void)processMarkupElement:(NSString *)tag properties:(NSDictionary<NSString *, NSString *> *)properties;

/**
 * Appends a markup element view.
 *
 * @param view The view to append.
 */
- (void)appendMarkupElementView:(UIView *)view;

/**
 * Previews a named view.
 *
 * @param viewName The name of the view to preview.
 * @param owner The view's owner, or <code>nil</code> for no owner.
 */
- (void)preview:(NSString *)viewName owner:(nullable id)owner;

@end

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
 * Creates a "plain" button.
 */
+ (UIButton *)plainButton API_AVAILABLE(tvos(11.0)) __IOS_PROHIBITED;

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

@interface UILabel (Markup)

/**
 * The shadow offset width.
 */
@property (nonatomic) CGFloat shadowOffsetWidth;

/**
 * The shadow offset height.
 */
@property (nonatomic) CGFloat shadowOffsetHeight;

@end

@interface UIPickerView (Markup) <UIPickerViewDataSource, UIPickerViewDelegate>

/**
 * Returns the name of a component.
 *
 * @param component The component index.
 *
 * @return The component's name.
 */
- (nullable NSString *)nameForComponent:(NSInteger)component;

/**
 * Returns the index of the first component whose name matches the given name.
 *
 * @param name The component name.
 *
 * @return The component index, or <code>NSNotFound</code> if a matching component was not found.
 */
- (NSInteger)componentWithName:(NSString *)name;

/**
 * Returns the value for the given row and component.
 *
 * @param row The row index.
 * @param component The component index.
 */
- (nullable id)valueForRow:(NSInteger)row forComponent:(NSInteger)component;

/**
 * Sets the value for the given row and component.
 *
 * @param value The row value.
 * @param row The row index.
 * @param component The component index.
 */
- (void)setValue:(nullable id)value forRow:(NSInteger)row forComponent:(NSInteger)component;

/**
 * Returns the value associated with the selected row in the given component.
 *
 * @param component The component index.
 *
 * @return The selected value, or <code>nil</code> if no row is selected.
 */
- (nullable id)valueForComponent:(NSInteger)component;

/**
 * Selects the first row in the given component whose value matches the given value.
 *
 * @param value The value to select, or <code>nil</code> for no selection.
 * @param component The component index.
 * @param animated <code>YES</code> if the selection should be animated; <code>NO</code>, otherwise.
 */
- (void)setValue:(nullable id)value forComponent:(NSInteger)component animated:(BOOL)animated;

@end

@interface UIProgressView (Markup)

/**
 * Creates a default progress view.
 */
+ (UIProgressView *)defaultProgressView;

/**
 * Creates a bar progress view.
 */
+ (UIProgressView *)barProgressView __TVOS_PROHIBITED;

@end

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

/**
 * The index of the current page.
 */
@property (nonatomic) NSInteger currentPage __TVOS_PROHIBITED;

/**
 * Sets the current page.
 *
 * @param currentPage The page index.
 * @param animated <code>YES</code> if the transition should be animated; <code>NO</code>, otherwise.
 */
- (void)setCurrentPage:(NSInteger)currentPage animated:(BOOL)animated __TVOS_PROHIBITED;

@end

@interface UITableView (Markup) <UITableViewDataSource, UITableViewDelegate>

/**
 * Returns the name of a section.
 *
 * @param section The section index.
 *
 * @return The section's name.
 */
- (nullable NSString *)nameForSection:(NSInteger)section;

/**
 * Returns the index of the first section with the given name.
 *
 * @param name The section name.
 *
 * @return The section index, or <code>NSNotFound</code> if a matching section was not found.
 */
- (NSInteger)sectionWithName:(NSString *)name;

/**
 * Returns the value associated with the first checked row in the given section.
 *
 * @param section The section index.
 *
 * @return The selected value, or <code>nil</code> if no row is checked.
 */
- (nullable id)valueForSection:(NSInteger)section;

/**
 * Checks all rows in the given section whose value matches the given value.
 *
 * @param value The value to select, or <code>nil</code> for no selection.
 * @param section The section index.
 */
- (void)setValue:(nullable id)value forSection:(NSInteger)section;

/**
 * Returns the values associated with the checked rows in the given section.
 *
 * @param section The section index.
 *
 * @return The selected values. The array will be empty if no rows are checked.
 */
- (NSArray *)valuesForSection:(NSInteger)section;

/**
 * Checks all rows in the given section whose value matches any value in the given array.
 *
 * @param values The values to select.
 * @param section The section index.
 */
- (void)setValues:(NSArray *)values forSection:(NSInteger)section;

@end

@interface UITableViewCell (Markup)

/**
 * Creates a default table view cell.
 */
+ (UITableViewCell *)defaultTableViewCell;

/**
 * Creates a "value 1" table view cell.
 */
+ (UITableViewCell *)value1TableViewCell;

/**
 * Creates a "value 2" table view cell.
 */
+ (UITableViewCell *)value2TableViewCell;

/**
 * Creates a subtitled table view cell.
 */
+ (UITableViewCell *)subtitleTableViewCell;

/**
 * An optional value associated with the cell.
 */
@property (nonatomic, nullable) id value;

/**
 * The cell's checked state.
 */
@property (nonatomic) BOOL checked NS_SWIFT_NAME(isChecked);

@end

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

@interface UIVisualEffectView (Markup)

/**
 * Creates an extra-light blur effect view.
 */
+ (UIVisualEffectView *)extraLightBlurEffectView;

/**
 * Creates a light blur effect view.
 */
+ (UIVisualEffectView *)lightBlurEffectView;

/**
 * Creates a dark blur effect view.
 */
+ (UIVisualEffectView *)darkBlurEffectView;

/**
 * Creates an extra-dark blur effect view.
 */
+ (UIVisualEffectView *)extraDarkBlurEffectView __IOS_PROHIBITED;

/**
 * Creates a regular blur effect view.
 */
+ (UIVisualEffectView *)regularBlurEffectView;

/**
 * Creates a prominent blur effect view.
 */
+ (UIVisualEffectView *)prominentBlurEffectView;

@end

NS_ASSUME_NONNULL_END
