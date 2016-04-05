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

@interface UIPickerView (Markup)

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
 * Returns the title for the given row and component.
 *
 * @param row The row index.
 * @param component The component index.
 */
- (nullable NSString *)titleForRow:(NSInteger)row forComponent:(NSInteger)component;

/**
 * Returns the value for the given row and component.
 *
 * @param row The row index.
 * @param component The component index.
 */
- (nullable id)valueForRow:(NSInteger)row forComponent:(NSInteger)component;

/**
 * Returns the index of the first row in the given component whose value matches the given value.
 *
 * @param value The row value.
 * @param component The component index.
 * 
 * @return The row index, or <code>NSNotFound</code> if a matching row was not found.
 */
- (NSInteger)rowWithValue:(nullable id)value inComponent:(NSInteger)component;

@end

NS_ASSUME_NONNULL_END
