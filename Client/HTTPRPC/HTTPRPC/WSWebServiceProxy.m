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

#import "WSWebServiceProxy.h"

NSString * const WSWebServiceErrorDomain = @"WSWebServiceErrorDomain";

NSString * const WSMethodNameKey = @"methodName";
NSString * const WSArgumentsKey = @"arguments";

@implementation WSWebServiceProxy

- (instancetype)initWithSession:(NSURLSession *)session baseURL:(NSURL *)baseURL
{
    self = [super init];

    if (self) {
        _session = session;
        _baseURL = baseURL;
    }

    return self;
}

- (NSURLSessionDataTask *)invoke:(NSString *)methodName resultHandler:(void (^)(id, NSError *))resultHandler
{
    return [self invoke:methodName withArguments:[NSDictionary new] resultHandler:resultHandler];
}

- (NSURLSessionDataTask *)invoke:(NSString *)methodName withArguments:(NSDictionary *)arguments resultHandler:(void (^)(id, NSError *))resultHandler
{
    NSURLSessionDataTask *task = nil;

    NSURL *requestURL = [NSURL URLWithString:methodName relativeToURL:_baseURL];

    if (requestURL != nil) {
        NSMutableString *parameters = [NSMutableString new];

        for (NSString *key in arguments) {
            id value = [arguments objectForKey:key];

            NSArray *components = [value isKindOfClass:[NSArray self]] ? (NSArray *)value : [NSArray arrayWithObject:value];

            for (id component in components) {
                if ([parameters length] > 0) {
                    [parameters appendString:@"&"];
                }

                NSString *argument;
                if (component == (void *)kCFBooleanTrue) {
                    argument = @"true";
                } else if (component == (void *)kCFBooleanFalse) {
                    argument = @"false";
                } else {
                    argument = [component description];
                }

                [parameters appendString:key];
                [parameters appendString:@"="];
                [parameters appendString:[argument stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]]];
            }
        }

        NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL: requestURL];

        [request setHTTPMethod:@"POST"];
        [request setHTTPBody:[parameters dataUsingEncoding:NSUTF8StringEncoding]];

        NSOperationQueue *resultHandlerQueue = [NSOperationQueue currentQueue];

        task = [_session dataTaskWithRequest:request completionHandler:^void (NSData *data, NSURLResponse *response, NSError *error) {
            id result = nil;

            if (error == nil) {
                NSInteger statusCode = [(NSHTTPURLResponse *)response statusCode];

                if (statusCode == 200) {
                    if ([data length] > 0) {
                        result = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingAllowFragments error: &error];
                    }
                } else {
                    error = [NSError errorWithDomain:WSWebServiceErrorDomain code:statusCode userInfo:@{
                        WSMethodNameKey: methodName,
                        WSArgumentsKey: arguments
                    }];
                }
            }

            [resultHandlerQueue addOperationWithBlock:^void () {
                resultHandler(result, error);
            }];
        }];

        [task resume];
    }

    return task;
}

@end
