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

#import "LMBoxView.h"

NS_ASSUME_NONNULL_BEGIN

/**
 * Baseline options.
 */
typedef NS_ENUM(NSInteger, LMBaseline) {
    /** First baseline. */
	LMBaselineFirst,

    /** Last baseline. */
	LMBaselineLast
};

/**
 * Layout view that arranges subviews horizontally in a row.
 */
@interface LMRowView : LMBoxView

/**
 * Specifies that subviews should be baseline-aligned. The default value is
 * <code>NO</code>.
 */
@property (nonatomic) BOOL alignToBaseline;

/**
 * The baseline to which subviews will be aligned when aligning to baseline.
 * By default, subviews will be aligned to the first baseline.
 */
@property (nonatomic) LMBaseline baseline;

@end

NS_ASSUME_NONNULL_END
