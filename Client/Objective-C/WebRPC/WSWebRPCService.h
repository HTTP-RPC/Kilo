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

/**
 * Web RPC service error domain.
 */
extern NSString * const WSWebRPCServiceErrorDomain;

/**
 * Web RPC method name key.
 */
extern NSString * const WSWebRPCMethodNameKey;

/**
 * Web RPC argument name key.
 */
extern NSString * const WSWebRPCArgumentsKey;

/**
 * Invocation proxy for web RPC services.
 */
@interface WSWebRPCService : NSObject

/**
 * Creates a new web RPC service.
 * 
 * @param session The URL session the service will use to execute HTTP requests.
 * @param baseURL The base URL of the service.
 */
- (instancetype)initWithSession:(NSURLSession *)session baseURL:(NSURL *)baseURL;

/**
 * The URL session the RPC service uses to execute HTTP requests.
 */
@property (nonatomic, readonly) NSURLSession *session;

/**
 * The base URL of the service.
 */
@property (nonatomic, readonly) NSURL *baseURL;

/**
 * Invokes a web RPC service method.
 *
 * @param methodName The name of the method to invoke.
 * @param resultHandler A callback that will be invoked upon completion of the method.
 *
 * @return A session data task representing the invocation request.
 */
- (NSURLSessionDataTask *)invoke:(NSString *)methodName resultHandler:(void (^)(id, NSError *))resultHandler;

/**
 * Invokes a web RPC service method.
 *
 * @param methodName The name of the method to invoke.
 * @param arguments The method arguments.
 * @param resultHandler A callback that will be invoked upon completion of the method.
 *
 * @return A session data task representing the invocation request.
 */
- (NSURLSessionDataTask *)invoke:(NSString *)methodName withArguments:(NSDictionary *)arguments resultHandler:(void (^)(id, NSError *))resultHandler;

@end
