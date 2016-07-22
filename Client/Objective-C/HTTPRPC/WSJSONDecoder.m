//
//  WSJSONDecoder.m
//  HTTPRPC
//
//  Created by Greg Brown on 7/22/16.
//  Copyright © 2016 HTTP-RPC. All rights reserved.
//

#import "WSJSONDecoder.h"

@implementation WSJSONDecoder

- (id)readValue:(NSData *)data contentType:(NSString *)contentType error:(NSError **)error
{
    return [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingAllowFragments error:error];
}

@end
