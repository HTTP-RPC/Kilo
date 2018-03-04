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
#import <MobileCoreServices/MobileCoreServices.h>

#import "WSWebServiceProxy.h"

NSString * const WSApplicationXWWWFormURLEncoded = @"application/x-www-form-urlencoded";
NSString * const WSMultipartFormData = @"multipart/form-data";
NSString * const WSApplicationJSON = @"application/json";

NSString * const WSWebServiceErrorDomain = @"WSWebServiceErrorDomain";

NSString * const kCRLF = @"\r\n";

@interface NSString (HTTPRPC)

- (NSData *)UTF8Data;
- (NSString *)URLEncodedString;

@end

@interface NSMutableData (HTTPRPC)

- (void)appendUTF8DataForString:(NSString *)string;

@end

@implementation WSWebServiceProxy
{
    NSString *_encoding;

    NSString *_multipartBoundary;
}

- (instancetype)initWithSession:(NSURLSession *)session serverURL:(NSURL *)serverURL
{
    self = [super init];

    if (self) {
        _session = session;
        _serverURL = serverURL;

        _encoding = WSMultipartFormData;

        _multipartBoundary = [[NSUUID new] UUIDString];
    }

    return self;
}

- (NSString *)encoding
{
    return _encoding;
}

- (void)setEncoding:(NSString *)encoding
{
    _encoding = [encoding lowercaseString];
}

- (NSURLSessionTask *)invoke:(NSString *)method path:(NSString *)path
    resultHandler:(void (^)(id, NSError *))resultHandler
{
    return [self invoke:method path:path arguments:[NSDictionary new] resultHandler:resultHandler];
}

- (NSURLSessionTask *)invoke:(NSString *)method path:(NSString *)path
    arguments:(NSDictionary *)arguments
    resultHandler:(void (^)(id, NSError *))resultHandler
{
    return [self invoke:method path:path arguments:arguments responseHandler:^id (NSData *data, NSString *contentType, NSError **error) {
        id result = nil;

        if ([contentType hasPrefix:WSApplicationJSON]) {
            result = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingAllowFragments error:error];
        } else if ([contentType hasPrefix:@"image/"]) {
            result = [UIImage imageWithData:data];
        } else if ([contentType hasPrefix:@"text/"]) {
            result = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
        } else {
            *error = [NSError errorWithDomain:WSWebServiceErrorDomain code:-1 userInfo:@{
                NSLocalizedDescriptionKey:@"Unsupported response encoding."
            }];
        }

        return result;
    } resultHandler:resultHandler];
}

- (NSURLSessionTask *)invoke:(NSString *)method path:(NSString *)path
    arguments:(NSDictionary<NSString *, id> *)arguments
    responseHandler:(id (^)(NSData *data, NSString *contentType, NSError **error))responseHandler
    resultHandler:(void (^)(id, NSError *))resultHandler;
{
    NSURLSessionDataTask *task = nil;

    NSURL *url = [NSURL URLWithString:path relativeToURL:_serverURL];

    if (url != nil) {
        // Construct query
        BOOL upload = ([method caseInsensitiveCompare:@"POST"] == NSOrderedSame
            || (([method caseInsensitiveCompare:@"PUT"] == NSOrderedSame || [method caseInsensitiveCompare:@"PATCH"] == NSOrderedSame)
                && [_encoding isEqual:WSApplicationJSON]));

        if (!upload) {
            NSString *query = [WSWebServiceProxy encodeQueryWithArguments:arguments];

            if ([query length] > 0) {
                url = [NSURL URLWithString:[NSString stringWithFormat:@"%@?%@", [url absoluteString], query]];
            }
        }

        NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];

        [request setHTTPMethod:method];

        [request setValue:[NSString stringWithFormat:@"%@, image/*, text/*", WSApplicationJSON] forHTTPHeaderField:@"Accept"];

        // Authenticate request
        if (_authorization != nil) {
            NSString *credentials = [NSString stringWithFormat:@"%@:%@", [_authorization user], [_authorization password]];
            NSString *value = [NSString stringWithFormat:@"Basic %@", [[credentials UTF8Data] base64EncodedStringWithOptions:0]];

            [request setValue:value forHTTPHeaderField:@"Authorization"];
        }

        // Write request body
        NSError *error = nil;

        if (upload) {
            NSString *contentType;
            if ([_encoding isEqual:WSMultipartFormData]) {
                contentType = [NSString stringWithFormat:@"%@; boundary=%@", _encoding, _multipartBoundary];
            } else {
                contentType = _encoding;
            }

            [request setValue:[NSString stringWithFormat:@"%@;charset=UTF-8", contentType] forHTTPHeaderField:@"Content-Type"];

            NSData *body = nil;

            if ([_encoding isEqual:WSMultipartFormData]) {
                body = [WSWebServiceProxy encodeMultipartFormDataRequestWithArguments:arguments boundary:_multipartBoundary];
            } else if ([_encoding isEqual:WSApplicationXWWWFormURLEncoded]) {
                body = [WSWebServiceProxy encodeApplicationXWWWFormURLEncodedRequestWithArguments:arguments];
            } else if ([_encoding isEqual:WSApplicationJSON]) {
                body = [NSJSONSerialization dataWithJSONObject:arguments options:0 error:&error];
            } else {
                error = [NSError errorWithDomain:WSWebServiceErrorDomain code:-1 userInfo:@{
                    NSLocalizedDescriptionKey:@"Unsupported request encoding."
                }];
            }

            [request setHTTPBody:body];
        }

        // Execute request
        NSOperationQueue *resultHandlerQueue = [NSOperationQueue currentQueue];

        if (error == nil) {
            task = [_session dataTaskWithRequest:request completionHandler:^void (NSData *data, NSURLResponse *response, NSError *error) {
                id result = nil;

                if (error == nil) {
                    NSInteger statusCode = [(NSHTTPURLResponse *)response statusCode];

                    if (statusCode / 100 == 2) {
                        if (statusCode % 100 < 4) {
                            NSString *contentType = [response MIMEType];

                            if (contentType == nil) {
                                contentType = WSApplicationJSON;
                            }

                            result = responseHandler(data, contentType, &error);
                        }
                    } else {
                        NSDictionary *userInfo;
                        if ([[response MIMEType] hasPrefix:@"text/plain"]) {
                            userInfo = @{
                                NSLocalizedDescriptionKey: [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding]
                            };
                        } else if ([[response MIMEType] hasPrefix:WSApplicationJSON]) {
                            userInfo = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
                        } else {
                            userInfo = nil;
                        }
                        
                        error = [NSError errorWithDomain:WSWebServiceErrorDomain code:statusCode userInfo:userInfo];
                    }
                }

                [resultHandlerQueue addOperationWithBlock:^void () {
                    resultHandler(result, error);
                }];
            }];

            [task resume];
        } else {
            [resultHandlerQueue addOperationWithBlock:^void () {
                resultHandler(nil, error);
            }];
        }
    }

    return task;
}

+ (NSString *)encodeQueryWithArguments:(NSDictionary *)arguments
{
    NSMutableString *query = [NSMutableString new];

    for (NSString *name in arguments) {
        NSArray *values = [self parameterValuesForArgument:[arguments objectForKey:name]];

        for (id value in values) {
            if (value == [NSNull null]) {
                continue;
            }

            if ([query length] > 0) {
                [query appendString:@"&"];
            }

            [query appendString:[name URLEncodedString]];
            [query appendString:@"="];
            [query appendString:[[self parameterValueForElement:value] URLEncodedString]];
        }
    }

    return query;
}

+ (NSData *)encodeMultipartFormDataRequestWithArguments:(NSDictionary *)arguments boundary:(NSString *)boundary
{
    NSMutableData *body = [NSMutableData new];

    for (NSString *name in arguments) {
        NSArray *values = [self parameterValuesForArgument:[arguments objectForKey:name]];

        for (id value in values) {
            if (value == [NSNull null]) {
                continue;
            }

            [body appendUTF8DataForString:[NSString stringWithFormat:@"--%@%@", boundary, kCRLF]];
            [body appendUTF8DataForString:[NSString stringWithFormat:@"Content-Disposition: form-data; name=\"%@\"", name]];

            if ([value isKindOfClass:[NSURL self]]) {
                NSString *filename = [value lastPathComponent];

                [body appendUTF8DataForString:[NSString stringWithFormat:@"; filename=\"%@\"", filename]];
                [body appendUTF8DataForString:kCRLF];

                CFStringRef extension = (__bridge CFStringRef)[filename pathExtension];
                CFStringRef uti = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, extension, NULL);

                NSString *attachmentContentType = CFBridgingRelease(UTTypeCopyPreferredTagWithClass(uti, kUTTagClassMIMEType));

                CFRelease(uti);

                if (attachmentContentType == nil) {
                    attachmentContentType = @"application/octet-stream";
                }

                [body appendUTF8DataForString:[NSString stringWithFormat:@"%@: %@%@", @"Content-Type", attachmentContentType, kCRLF]];
                [body appendUTF8DataForString:kCRLF];

                [body appendData:[NSData dataWithContentsOfURL:value]];
            } else {
                [body appendUTF8DataForString:kCRLF];

                [body appendUTF8DataForString:kCRLF];
                [body appendUTF8DataForString:[self parameterValueForElement:value]];
            }

            [body appendUTF8DataForString:kCRLF];
        }
    }

    [body appendUTF8DataForString:[NSString stringWithFormat:@"--%@--%@", boundary, kCRLF]];

    return body;
}

+ (NSData *)encodeApplicationXWWWFormURLEncodedRequestWithArguments:(NSDictionary *)arguments
{
    NSMutableData *body = [NSMutableData new];

    for (NSString *name in arguments) {
        NSArray *values = [self parameterValuesForArgument:[arguments objectForKey:name]];

        for (id value in values) {
            if (value == [NSNull null]) {
                continue;
            }

            if ([body length] > 0) {
                [body appendUTF8DataForString:@"&"];
            }

            [body appendUTF8DataForString:[name URLEncodedString]];
            [body appendUTF8DataForString:@"="];
            [body appendUTF8DataForString:[[self parameterValueForElement:value] URLEncodedString]];
        }
    }

    return body;
}

+ (NSArray *)parameterValuesForArgument:(id)argument {
    NSArray *values;
    if ([argument isKindOfClass:[NSArray self]]) {
        values = (NSArray *)argument;
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

@implementation NSString (HTTPRPC)

- (NSData *)UTF8Data
{
    return [self dataUsingEncoding:NSUTF8StringEncoding];
}

- (NSString *)URLEncodedString
{
    return [[self stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]]
        stringByReplacingOccurrencesOfString:@"+" withString:@"%2B"];
}

@end

@implementation NSMutableData (HTTPRPC)

- (void)appendUTF8DataForString:(NSString *)string
{
    [self appendData:[string UTF8Data]];
}

@end
