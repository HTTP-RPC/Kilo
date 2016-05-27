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

#import "WSBasicAuthentication.h"
#import "NSString+HTTPRPC.h"

NSString * const kAuthorizationField = @"Authorization";

@implementation WSBasicAuthentication
{
    NSString *_username;
    NSString *_password;
}

- (instancetype)initWithUsername:(NSString *)username password:(NSString *)password
{
    self = [super init];

    if (self) {
        _username = username;
        _password = password;
    }

    return self;
}

- (void)authenticate:(NSMutableURLRequest *)request
{
    NSString *credentials = [NSString stringWithFormat:@"%@:%@", _username, _password];
    NSString *authorization = [NSString stringWithFormat: @"Basic %@", [[credentials UTF8Data] base64EncodedStringWithOptions:0]];

    [request setValue:authorization forHTTPHeaderField:kAuthorizationField];
}

@end
