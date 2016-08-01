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

#import "WSWebServiceProxy.h"

NS_ASSUME_NONNULL_BEGIN

/**
 * Web service proxy delegate protocol.
 */
@protocol WSWebServiceProxyDelegate <NSObject>

/**
 * Decodes response data.
 *
 * @param webServiceProxy The web service proxy instance.
 * @param data The data to decode.
 * @param contentType The MIME type of the content, or <code>nil</code> if the content type is unknown.
 * 
 * @return The decoded value, or <code>nil</code> if the value could not be decoded.
 */
- (nullable id)webServiceProxy:(WSWebServiceProxy *)webServiceProxy decodeData:(NSData *)data withContentType:(NSString *)contentType;

@end

NS_ASSUME_NONNULL_END