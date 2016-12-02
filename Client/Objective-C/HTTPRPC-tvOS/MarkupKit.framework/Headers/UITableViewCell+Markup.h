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
@property (nonatomic) BOOL checked;

@end

NS_ASSUME_NONNULL_END
