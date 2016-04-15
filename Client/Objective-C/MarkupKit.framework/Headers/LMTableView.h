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
 * Table view selection modes.
 */
typedef NS_ENUM(NSInteger, LMTableViewSelectionMode) {
    /** Default selection mode. */
    LMTableViewSelectionModeDefault,

    /** Single-checkmark selection mode. */
    LMTableViewSelectionModeSingleCheckmark,

    /** Multiple-checkmark selection mode. */
    LMTableViewSelectionModeMultipleCheckmarks
};

/**
 * Table view that supports declarative content. 
 */
@interface LMTableView : UITableView

/**
 * Creates a plain table view.
 */
+ (LMTableView *)plainTableView;

/**
 * Creates a grouped table view.
 */
+ (LMTableView *)groupedTableView;

/**
 * Inserts a new section.
 *
 * @param section The index at which the section will be inserted.
 */
- (void)insertSection:(NSInteger)section;

/**
 * Deletes an existing section.
 *
 * @param section The index of the section to delete.
 */
- (void)deleteSection:(NSInteger)section;

/**
 * Sets the name of a section.
 *
 * @param name The section name.
 * @param section The section index.
 */
- (void)setName:(nullable NSString *)name forSection:(NSInteger)section;

/**
 * Returns the selection mode for a section.
 *
 * @param section The section index.
 *
 * @return The section's selection mode.
 */
- (LMTableViewSelectionMode)selectionModeForSection:(NSInteger)section;

/**
 * Sets the selection mode for a section.
 *
 * @param name The selection mode.
 * @param section The section index.
 */
- (void)setSelectionMode:(LMTableViewSelectionMode)selectionMode forSection:(NSInteger)section;

/**
 * Returns the header view for a section.
 *
 * @param section The section index.
 *
 * @return The header view for the section.
 */
- (nullable UIView *)viewForHeaderInSection:(NSInteger)section;

/**
 * Sets the header view for a section.
 * 
 * @param view The header view.
 * @param section The section index.
 */
- (void)setView:(nullable UIView *)view forHeaderInSection:(NSInteger)section;

/**
 * Returns the footer view for a section.
 *
 * @param section The section index.
 *
 * @return The footer view for the section.
 */
- (nullable UIView *)viewForFooterInSection:(NSInteger)section;

/**
 * Sets the footer view for a section.
 * 
 * @param view The footer view.
 * @param section The section index.
 */
- (void)setView:(nullable UIView *)footerView forFooterInSection:(NSInteger)section;

/**
 * Inserts a new row into the table view.
 *
 * @param cell The cell representing the row to insert.
 * @param indexPath The index path at which the row will be inserted.
 */
- (void)insertCell:(UITableViewCell *)cell forRowAtIndexPath:(NSIndexPath *)indexPath;

/**
 * Deletes an existing row from the table view.
 *
 * @param indexPath The index path of the row to delete.
 */
- (void)deleteCellForRowAtIndexPath:(NSIndexPath *)indexPath;

@end

NS_ASSUME_NONNULL_END
