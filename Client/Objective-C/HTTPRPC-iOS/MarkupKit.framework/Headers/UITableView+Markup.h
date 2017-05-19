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

NS_ASSUME_NONNULL_END
