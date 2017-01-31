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
 * Class that reads a view hierarchy from markup.
 */
@interface LMViewBuilder : NSObject

/**
 * Loads a named view.
 *
 * @param name The name of the view to load.
 * @param owner The view's owner, or <code>nil</code> for no owner.
 * @param root The root view, or <code>nil</code> for no root view.
 *
 * @return The named view, or <code>nil</code> if the view could not be loaded.
 */
+ (nullable UIView *)viewWithName:(NSString *)name owner:(nullable id)owner root:(nullable UIView *)root;

/**
 * Decodes a color value.
 *
 * @param value The encoded color value.
 *
 * @return The decoded color value, or <code>nil</code> if the color could not be decoded.
 */
+ (nullable UIColor *)colorValue:(NSString *)value;

/**
 * Decodes a font value.
 *
 * @param value The encoded font value.
 *
 * @return The decoded font value, or <code>nil</code> if the font could not be decoded.
 */
+ (nullable UIFont *)fontValue:(NSString *)value;

@end

NS_ASSUME_NONNULL_END
