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
    return [self invoke:methodName withArguments:arguments attachments:[NSDictionary new] resultHandler:resultHandler];
}

- (NSURLSessionDataTask *)invoke:(NSString *)methodName withArguments:(NSDictionary *)arguments attachments:(NSDictionary *)attachments resultHandler:(void (^)(id, NSError *))resultHandler
{
    NSURLSessionDataTask *task = nil;

    NSURL *requestURL = [NSURL URLWithString:methodName relativeToURL:_baseURL];

    if (requestURL != nil) {
        NSMutableString *parameters = [NSMutableString new];

        for (NSString *name in arguments) {
            id value = [arguments objectForKey:name];

            NSArray *values;
            if ([value isKindOfClass:[NSArray self]]) {
                values = (NSArray *)value;
            } else if ([value isKindOfClass:[NSDictionary self]]) {
                NSDictionary *dictionary = (NSDictionary *)value;

                NSMutableArray *entries = [NSMutableArray new];

                for (NSString *key in dictionary) {
                    [entries addObject:[NSString stringWithFormat:@"%@:%@",
                        [key stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]],
                        [[[dictionary objectForKey:key] description] stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]]]];
                }

                values = entries;
            } else {
                values = [NSArray arrayWithObject:value];
            }

            for (NSUInteger i = 0, n = [values count]; i < n; i++) {
                if ([parameters length] > 0) {
                    [parameters appendString:@"&"];
                }

                id element = [values objectAtIndex:i];

                if (element == (void *)kCFBooleanTrue) {
                    element = @"true";
                } else if (element == (void *)kCFBooleanFalse) {
                    element = @"false";
                } else {
                    element = [element description];
                }

                [parameters appendString:[name stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]]];
                [parameters appendString:@"="];
                [parameters appendString:[element stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]]];
            }
        }

        NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL: requestURL];

        [request setHTTPMethod:@"POST"];
        [request setHTTPBody:[parameters dataUsingEncoding:NSUTF8StringEncoding]];

        for (NSString *name in attachments) {
            NSURL *url = [attachments objectForKey:name];

            // TODO
            NSLog(@"%@: %@", name, url);
        }

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
