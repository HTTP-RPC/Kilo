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

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface NSObject (Markup)

/**
 * Applies a markup property value.
 *
 * @param value The value to apply.
 * @param keyPath The property's key.
 */
- (void)applyMarkupPropertyValue:(nullable id)value forKey:(NSString *)key;

/**
 * Applies a markup property value.
 *
 * @param value The value to apply.
 * @param keyPath The property's key path.
 */
- (void)applyMarkupPropertyValue:(nullable id)value forKeyPath:(NSString *)keyPath;

@end

NS_ASSUME_NONNULL_END