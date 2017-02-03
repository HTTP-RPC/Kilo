//
//  UIResponder+Markup.h
//  MarkupKit-iOS
//
//  Created by Greg Brown on 1/31/17.
//
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface UIResponder (Markup)

/**
 * Returns the bundle that will be used to load view documents. The default implementation returns the bundle used to load this class.
 */
- (NSBundle *)bundleForView;

/**
 * Returns the bundle that will be used to load images. The default implementation returns the main bundle.
 */
- (NSBundle *)bundleForImages;

/**
 * Returns the bundle that will be used to localize string values. The default implementation returns the main bundle.
 */
- (NSBundle *)bundleForStrings;

/**
 * Establishes a two-way binding between this object and an associated view instance.
 *
 * @param property The key path of a property in this object.
 * @param view The associated view instance.
 * @param keyPath The key path of a property in the view.
 */
- (void)bind:(NSString *)property toView:(UIView *)view withKeyPath:(NSString *)keyPath;

/**
 * Releases all bindings.
 */
- (void)unbindAll;

@end

NS_ASSUME_NONNULL_END
