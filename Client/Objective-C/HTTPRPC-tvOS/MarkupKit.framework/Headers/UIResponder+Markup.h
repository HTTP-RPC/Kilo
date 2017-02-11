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
