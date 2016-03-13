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
 * HTTP-RPC method name key.
 */
extern NSString * const WSMethodNameKey;

/**
 * HTTP-RPC arguments key.
 */
extern NSString * const WSArgumentsKey;

/**
 * Invocation proxy for HTTP-RPC web services.
 */
@interface WSWebServiceProxy : NSObject

- (instancetype)init NS_UNAVAILABLE;

/**
 * Creates a new HTTP-RPC service proxy.
 * 
 * @param session The URL session the service proxy will use to execute HTTP requests.
 * @param baseURL The base URL of the service.
 */
- (instancetype)initWithSession:(NSURLSession *)session baseURL:(NSURL *)baseURL NS_DESIGNATED_INITIALIZER;

/**
 * The URL session the service proxy uses to execute HTTP requests.
 */
@property (nonatomic, readonly) NSURLSession *session;

/**
 * The base URL of the service.
 */
@property (nonatomic, readonly) NSURL *baseURL;

/**
 * Invokes an HTTP-RPC service method.
 *
 * @param methodName The name of the method to invoke.
 * @param resultHandler A callback that will be invoked upon completion of the method.
 *
 * @return A session data task representing the invocation request.
 */
- (NSURLSessionDataTask *)invoke:(NSString *)methodName
    resultHandler:(void (^)(id _Nullable, NSError * _Nullable))resultHandler;

/**
 * Invokes an HTTP-RPC service method.
 *
 * @param methodName The name of the method to invoke.
 * @param arguments The method arguments.
 * @param resultHandler A callback that will be invoked upon completion of the method.
 *
 * @return A session data task representing the invocation request.
 */
- (NSURLSessionDataTask *)invoke:(NSString *)methodName
    withArguments:(NSDictionary<NSString *, id> *)arguments
    resultHandler:(void (^)(id _Nullable, NSError * _Nullable))resultHandler;

/**
 * Invokes an HTTP-RPC service method.
 *
 * @param methodName The name of the method to invoke.
 * @param arguments The method arguments.
 * @param attachments The method attachments.
 * @param resultHandler A callback that will be invoked upon completion of the method.
 *
 * @return A session data task representing the invocation request.
 */
- (NSURLSessionDataTask *)invoke:(NSString *)methodName
    withArguments:(NSDictionary<NSString *, id> *)arguments
    attachments:(NSDictionary<NSString *, NSURL *> *)attachments
    resultHandler:(void (^)(id _Nullable, NSError * _Nullable))resultHandler;

@end

NS_ASSUME_NONNULL_END
