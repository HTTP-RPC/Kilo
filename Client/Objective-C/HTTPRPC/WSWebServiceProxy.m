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
        NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL: requestURL];

        [request setHTTPMethod:@"POST"];

        if ([attachments count] == 0) {
            NSMutableString *parameters = [NSMutableString new];

            for (NSString *name in arguments) {
                NSArray *values = [WSWebServiceProxy parameterValuesForArgument:[arguments objectForKey:name]];

                for (NSUInteger i = 0, n = [values count]; i < n; i++) {
                    if ([parameters length] > 0) {
                        [parameters appendString:@"&"];
                    }

                    NSString *value = [WSWebServiceProxy parameterValueForElement:[values objectAtIndex:i]];

                    [parameters appendString:[name stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]]];
                    [parameters appendString:@"="];
                    [parameters appendString:[value stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]]];
                }
            }

            [request setHTTPBody:[parameters dataUsingEncoding:NSUTF8StringEncoding]];
        } else {
            NSString *boundary = [[NSUUID new] UUIDString];
            NSString *contentType = [NSString stringWithFormat:@"multipart/form-data; boundary=%@", boundary];

            [request addValue:contentType forHTTPHeaderField:@"Content-Type"];

            NSMutableData *body = [NSMutableData new];

            NSData *boundaryData = [[NSString stringWithFormat:@"--%@\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding];

            NSString *contentDispositionFormat = @"Content-Disposition: form-data; name=\"%@\"";

            for (NSString *name in arguments) {
                NSArray *values = [WSWebServiceProxy parameterValuesForArgument:[arguments objectForKey:name]];

                for (id element in values) {
                    NSString *value = [WSWebServiceProxy parameterValueForElement:element];

                    [body appendData:boundaryData];

                    [body appendData:[[NSString stringWithFormat:contentDispositionFormat, name] dataUsingEncoding:NSUTF8StringEncoding]];
                    [body appendData:[[NSString stringWithFormat:@"\r\n\r\n%@\r\n", value] dataUsingEncoding:NSUTF8StringEncoding]];
                }
            }

            NSString *filenameParameterFormat = @"; filename=\"%@\"";

            NSData *octetStreamContentTypeData = [@"Content-Type: application/octet-stream\r\n" dataUsingEncoding:NSUTF8StringEncoding];

            for (NSString *name in attachments) {
                NSArray *urls = [attachments objectForKey:name];

                for (NSURL *url in urls) {
                    [body appendData:boundaryData];

                    [body appendData:[[NSString stringWithFormat:contentDispositionFormat, name] dataUsingEncoding:NSUTF8StringEncoding]];
                    [body appendData:[[NSString stringWithFormat:filenameParameterFormat, [url filePathURL]] dataUsingEncoding:NSUTF8StringEncoding]];
                    [body appendData:[@"\r\n" dataUsingEncoding:NSUTF8StringEncoding]];

                    [body appendData:octetStreamContentTypeData];
                    [body appendData:[@"\r\n" dataUsingEncoding:NSUTF8StringEncoding]];

                    [body appendData:[NSData dataWithContentsOfURL:url]];
                    [body appendData:[@"\r\n" dataUsingEncoding:NSUTF8StringEncoding]];
                }
            }

            [body appendData:[[NSString stringWithFormat:@"--%@--\r\n", boundary] dataUsingEncoding:NSUTF8StringEncoding]];

            [request setHTTPBody:body];
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

+ (NSArray *)parameterValuesForArgument:(id)argument {
    NSArray *values;
    if ([argument isKindOfClass:[NSArray self]]) {
        values = (NSArray *)argument;
    } else if ([argument isKindOfClass:[NSDictionary self]]) {
        NSDictionary *dictionary = (NSDictionary *)argument;

        NSMutableArray *entries = [NSMutableArray new];

        for (NSString *key in dictionary) {
            [entries addObject:[NSString stringWithFormat:@"%@:%@",
                [key stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]],
                [[[dictionary objectForKey:key] description] stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]]]];
        }

        values = entries;
    } else {
        values = [NSArray arrayWithObject:argument];
    }

    return values;
}

+ (NSString *)parameterValueForElement:(id)element {
    id value;
    if (element == (void *)kCFBooleanTrue) {
        value = @"true";
    } else if (element == (void *)kCFBooleanFalse) {
        value = @"false";
    } else {
        value = [element description];
    }

    return value;
}

@end
