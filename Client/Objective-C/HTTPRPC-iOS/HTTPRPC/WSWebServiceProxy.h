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

/**
 * HTTP-RPC web service error domain.
 */
extern NSString * const WSWebServiceErrorDomain;

/**
 * HTTP-RPC method key.
 */
extern NSString * const WSMethodKey;

/**
 * HTTP-RPC path key.
 */
extern NSString * const WSPathKey;

/**
 * HTTP-RPC arguments key.
 */
extern NSString * const WSArgumentsKey;

/**
 * Web service invocation proxy.
 */
@interface WSWebServiceProxy : NSObject

- (instancetype)init NS_UNAVAILABLE;

/**
 * Creates a new web service proxy.
 * 
 * @param session The URL session the service proxy will use to execute HTTP requests.
 * @param serverURL The server URL.
 */
- (instancetype)initWithSession:(NSURLSession *)session serverURL:(NSURL *)serverURL NS_DESIGNATED_INITIALIZER;

/**
 * The URL session the service proxy uses to execute HTTP requests.
 */
@property (nonatomic, readonly) NSURLSession *session;

/**
 * The server URL.
 */
@property (nonatomic, readonly) NSURL *serverURL;

/**
 * The service proxy's authorization credentials, or <code>nil</code> for no credentials.
 */
@property (nonatomic, nullable) NSURLCredential *authorization;

/**
 * Executes a service operation.
 *
 * @param method The HTTP verb associated with the request.
 * @param path The path associated with the request.
 * @param resultHandler A callback that will be invoked upon completion of the request.
 *
 * @return A session data task representing the invocation request.
 */
- (nullable NSURLSessionDataTask *)invoke:(NSString *)method path:(NSString *)path
    resultHandler:(void (^)(id _Nullable, NSError * _Nullable))resultHandler;

/**
 * Executes a service operation.
 *
 * @param method The HTTP verb associated with the request.
 * @param path The path associated with the request.
 * @param arguments The request arguments.
 * @param resultHandler A callback that will be invoked upon completion of the request.
 *
 * @return A session data task representing the invocation request.
 */
- (nullable NSURLSessionDataTask *)invoke:(NSString *)method path:(NSString *)path
    arguments:(NSDictionary<NSString *, id> *)arguments
    resultHandler:(void (^)(id _Nullable, NSError * _Nullable))resultHandler;

@end

NS_ASSUME_NONNULL_END
