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

NS_ASSUME_NONNULL_END
