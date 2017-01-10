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

NSString * const WSWebServiceErrorDomain = @"WSWebServiceErrorDomain";

NSString * const WSMethodKey = @"method";
NSString * const WSPathKey = @"path";
NSString * const WSArgumentsKey = @"arguments";

NSString * const kMultipartFormDataMIMEType = @"multipart/form-data";
NSString * const kApplicationXWWWFormURLEncodedMIMEType = @"application/x-www-form-urlencoded";
NSString * const kApplicationJSONMIMEType = @"application/json";

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

        _encoding = kMultipartFormDataMIMEType;

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
    NSURLSessionDataTask *task = nil;

    NSURL *url = [NSURL URLWithString:path relativeToURL:_serverURL];

    if (url != nil) {
        // Construct query
        BOOL encode = ([method caseInsensitiveCompare:@"POST"] == NSOrderedSame
            || ([method caseInsensitiveCompare:@"PUT"] == NSOrderedSame && [_encoding isEqual:kApplicationJSONMIMEType]));

        if (!encode) {
            NSMutableString *query = [NSMutableString new];

            NSUInteger i = 0;

            for (NSString *name in arguments) {
                NSArray *values = [WSWebServiceProxy parameterValuesForArgument:[arguments objectForKey:name]];

                for (NSUInteger j = 0, n = [values count]; j < n; j++) {
                    if (i > 0) {
                        [query appendString:@"&"];
                    }

                    NSString *value = [WSWebServiceProxy parameterValueForElement:[values objectAtIndex:j]];

                    [query appendString:[name URLEncodedString]];
                    [query appendString:@"="];
                    [query appendString:[value URLEncodedString]];

                    i++;
                }
            }

            if ([query length] > 0) {
                url = [NSURL URLWithString:[NSString stringWithFormat:@"%@?%@", [url absoluteString], query]];
            }
        }

        NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];

        [request setHTTPMethod:method];

        [request setValue:[NSString stringWithFormat:@"%@, image/*, text/*", kApplicationJSONMIMEType] forHTTPHeaderField:@"Accept"];

        // Authenticate request
        if (_authorization != nil) {
            NSString *credentials = [NSString stringWithFormat:@"%@:%@", [_authorization user], [_authorization password]];
            NSString *value = [NSString stringWithFormat:@"Basic %@", [[credentials UTF8Data] base64EncodedStringWithOptions:0]];

            [request setValue:value forHTTPHeaderField:@"Authorization"];
        }

        // Write request body
        NSError *error = nil;

        if (encode) {
            NSString *contentType;
            if ([_encoding isEqual:kMultipartFormDataMIMEType]) {
                contentType = [NSString stringWithFormat:@"%@; boundary=%@", _encoding, _multipartBoundary];
            } else {
                contentType = _encoding;
            }

            [request setValue:contentType forHTTPHeaderField:@"Content-Type"];
            [request setHTTPBody:[self encodeRequestWithArguments:arguments error:&error]];
        }

        // Execute request
        NSOperationQueue *resultHandlerQueue = [NSOperationQueue currentQueue];

        if (error == nil) {
            task = [_session dataTaskWithRequest:request completionHandler:^void (NSData *data, NSURLResponse *response, NSError *error) {
                id result = nil;

                if (error == nil) {
                    NSInteger statusCode = [(NSHTTPURLResponse *)response statusCode];

                    if (statusCode / 100 == 2) {
                        NSString *mimeType = [response MIMEType];

                        if (mimeType != nil) {
                            result = [self decodeResponse:data withContentType:[mimeType lowercaseString] error:&error];
                        }
                    } else {
                        error = [NSError errorWithDomain:WSWebServiceErrorDomain code:statusCode userInfo:@{
                            WSMethodKey:method, WSPathKey:path, WSArgumentsKey:arguments
                        }];
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

- (NSData *)encodeRequestWithArguments:(NSDictionary *)arguments error:(NSError **)error
{
    NSData *body;
    if ([_encoding isEqual:kMultipartFormDataMIMEType]) {
        body = [self encodeMultipartFormDataRequestWithArguments:arguments];
    } else if ([_encoding isEqual:kApplicationXWWWFormURLEncodedMIMEType]) {
        body = [self encodeApplicationXWWWFormURLEncodedRequestWithArguments:arguments];
    } else if ([_encoding isEqual:kApplicationJSONMIMEType]) {
        body = [NSJSONSerialization dataWithJSONObject:arguments options:0 error:error];
    } else {
        body = nil;

        *error = [NSError errorWithDomain:WSWebServiceErrorDomain code:-1 userInfo:@{
            NSLocalizedDescriptionKey:@"Unsupported request encoding."
        }];
    }

    return body;
}

- (NSData *)encodeMultipartFormDataRequestWithArguments:(NSDictionary *)arguments
{
    NSMutableData *body = [NSMutableData new];

    for (NSString *name in arguments) {
        NSArray *values = [WSWebServiceProxy parameterValuesForArgument:[arguments objectForKey:name]];

        for (__strong id value in values) {
            [body appendUTF8DataForString:[NSString stringWithFormat:@"--%@%@", _multipartBoundary, kCRLF]];
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
                [body appendUTF8DataForString:[WSWebServiceProxy parameterValueForElement:value]];
            }

            [body appendUTF8DataForString:kCRLF];
        }
    }

    [body appendUTF8DataForString:[NSString stringWithFormat:@"--%@--%@", _multipartBoundary, kCRLF]];

    return body;
}

- (NSData *)encodeApplicationXWWWFormURLEncodedRequestWithArguments:(NSDictionary *)arguments
{
    NSMutableData *body = [NSMutableData new];

    NSUInteger i = 0;

    for (NSString *name in arguments) {
        NSArray *values = [WSWebServiceProxy parameterValuesForArgument:[arguments objectForKey:name]];

        for (NSUInteger j = 0, n = [values count]; j < n; j++) {
            if (i > 0) {
                [body appendUTF8DataForString:@"&"];
            }

            NSString *value = [WSWebServiceProxy parameterValueForElement:[values objectAtIndex:j]];

            [body appendUTF8DataForString:[name URLEncodedString]];
            [body appendUTF8DataForString:@"="];
            [body appendUTF8DataForString:[value URLEncodedString]];

            i++;
        }
    }

    return body;
}

- (id)decodeResponse:(NSData *)data withContentType:(NSString *)contentType error:(NSError **)error
{
    id value;
    if ([contentType hasPrefix:kApplicationJSONMIMEType]) {
        value = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingAllowFragments error:error];
    } else if ([contentType hasPrefix:@"image/"]) {
        value = [UIImage imageWithData:data];
    } else if ([contentType hasPrefix:@"text/"]) {
        value = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
    } else {
        value = nil;

        *error = [NSError errorWithDomain:WSWebServiceErrorDomain code:-1 userInfo:@{
            NSLocalizedDescriptionKey:@"Unsupported response encoding."
        }];
    }

    return value;
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
    return [self stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]];
}

@end

@implementation NSMutableData (HTTPRPC)

- (void)appendUTF8DataForString:(NSString *)string
{
    [self appendData:[string UTF8Data]];
}

@end
