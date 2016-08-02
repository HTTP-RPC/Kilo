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
#import "NSString+HTTPRPC.h"

NSString * const WSWebServiceErrorDomain = @"WSWebServiceErrorDomain";

NSString * const WSMethodKey = @"method";
NSString * const WSPathKey = @"path";
NSString * const WSArgumentsKey = @"arguments";

NSString * const kPostMethod = @"POST";

NSString * const kContentTypeField = @"Content-Type";
NSString * const kMultipartFormDataMIMEType = @"multipart/form-data";
NSString * const kBoundaryParameterFormat = @"; boundary=%@";

NSString * const kOctetStreamMIMEType = @"application/octet-stream";

NSString * const kContentDispositionHeader = @"Content-Disposition: form-data";
NSString * const kNameParameterFormat = @"; name=\"%@\"";
NSString * const kFilenameParameterFormat = @"; filename=\"%@\"";

NSString * const kCRLF = @"\r\n";

NSString * const kJSONMIMEType = @"application/json";
NSString * const kImageMIMETypePrefix = @"image/";

@implementation WSWebServiceProxy

- (instancetype)initWithSession:(NSURLSession *)session serverURL:(NSURL *)serverURL
{
    self = [super init];

    if (self) {
        _session = session;
        _serverURL = serverURL;
    }

    return self;
}

- (NSURLSessionDataTask *)invoke:(NSString *)method path:(NSString *)path
    resultHandler:(void (^)(id, NSError *))resultHandler
{
    return [self invoke:method path:path arguments:[NSDictionary new] resultHandler:resultHandler];
}

- (NSURLSessionDataTask *)invoke:(NSString *)method path:(NSString *)path
    arguments:(NSDictionary *)arguments
    resultHandler:(void (^)(id, NSError *))resultHandler
{
    NSURLSessionDataTask *task = nil;

    NSURL *url = [NSURL URLWithString:path relativeToURL:_serverURL];

    if (url != nil) {
        // Construct query
        if ([method caseInsensitiveCompare:kPostMethod] != NSOrderedSame) {
            NSMutableString *query = [NSMutableString new];

            for (NSString *name in arguments) {
                NSArray *values = [WSWebServiceProxy parameterValuesForArgument:[arguments objectForKey:name]];

                for (NSUInteger i = 0, n = [values count]; i < n; i++) {
                    if ([query length] > 0) {
                        [query appendString:@"&"];
                    }

                    NSString *value = [WSWebServiceProxy parameterValueForElement:[values objectAtIndex:i]];

                    [query appendString:[name URLEncodedString]];
                    [query appendString:@"="];
                    [query appendString:[value URLEncodedString]];
                }
            }

            if ([query length] > 0) {
                url = [NSURL URLWithString:[NSString stringWithFormat:@"%@?%@", [url absoluteString], query]];
            }
        }

        NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:url];

        [request setHTTPMethod:method];

        // Authenticate request
        id<WSAuthentication> authentication = [self authentication];

        if (authentication != nil) {
            [authentication authenticateRequest:request];
        }

        // Write request body
        if ([method caseInsensitiveCompare:kPostMethod] == NSOrderedSame) {
            NSString *boundary = [[NSUUID new] UUIDString];
            NSString *requestContentType = [kMultipartFormDataMIMEType stringByAppendingString:[NSString stringWithFormat:kBoundaryParameterFormat, boundary]];

            [request setValue:requestContentType forHTTPHeaderField:kContentTypeField];

            // Construct multi-part form data
            NSMutableData *body = [NSMutableData new];

            NSData *boundaryData = [[NSString stringWithFormat:@"--%@%@", boundary, kCRLF] UTF8Data];

            NSData *contentDispositionHeaderData = [kContentDispositionHeader UTF8Data];

            for (NSString *name in arguments) {
                NSArray *values = [WSWebServiceProxy parameterValuesForArgument:[arguments objectForKey:name]];

                for (__strong id value in values) {
                    [body appendData:boundaryData];

                    [body appendData:contentDispositionHeaderData];
                    [body appendData:[[NSString stringWithFormat:kNameParameterFormat, name] UTF8Data]];

                    if ([value isKindOfClass:[NSURL self]]) {
                        NSString *filename = [value lastPathComponent];

                        [body appendData:[[NSString stringWithFormat:kFilenameParameterFormat, filename] UTF8Data]];
                        [body appendData:[kCRLF UTF8Data]];

                        CFStringRef extension = (__bridge CFStringRef)[filename pathExtension];
                        CFStringRef uti = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, extension, NULL);

                        NSString *attachmentContentType = CFBridgingRelease(UTTypeCopyPreferredTagWithClass(uti, kUTTagClassMIMEType));

                        CFRelease(uti);

                        if (attachmentContentType == nil) {
                            attachmentContentType = kOctetStreamMIMEType;
                        }

                        [body appendData:[[NSString stringWithFormat:@"%@: %@%@", kContentTypeField, attachmentContentType, kCRLF] UTF8Data]];
                        [body appendData:[kCRLF UTF8Data]];

                        [body appendData:[NSData dataWithContentsOfURL:value]];
                    } else {
                        [body appendData:[kCRLF UTF8Data]];

                        [body appendData:[kCRLF UTF8Data]];
                        [body appendData:[[WSWebServiceProxy parameterValueForElement:value] UTF8Data]];
                    }

                    [body appendData:[kCRLF UTF8Data]];
                }
            }

            [body appendData:[[NSString stringWithFormat:@"--%@--%@", boundary, kCRLF] UTF8Data]];

            [request setHTTPBody:body];
        }

        // Execute request
        NSOperationQueue *resultHandlerQueue = [NSOperationQueue currentQueue];

        task = [_session dataTaskWithRequest:request completionHandler:^void (NSData *data, NSURLResponse *response, NSError *error) {
            id result = nil;

            if (error == nil) {
                NSInteger statusCode = [(NSHTTPURLResponse *)response statusCode];

                if (statusCode / 100 == 2) {
                    if ([data length] > 0) {
                        NSString *mimeType = [response MIMEType];

                        if ([mimeType hasPrefix:kJSONMIMEType]) {
                            result = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingAllowFragments error:&error];
                        } else if ([mimeType hasPrefix:kImageMIMETypePrefix]) {
                            result = [UIImage imageWithData:data];
                        } else {
                            result = data;
                        }
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
    }

    return task;
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
    NSAssert([element isKindOfClass:[NSString self]] || [element isKindOfClass:[NSNumber self]], @"Invalid parameter element.");

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

