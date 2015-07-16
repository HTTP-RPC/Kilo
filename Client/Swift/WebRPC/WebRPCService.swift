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

import Foundation

/**
 * Web RPC service error domain.
 */
public let WebRPCServiceErrorDomain = "RPCServiceErrorDomain"

/**
 * Web RPC method name key.
 */
public let WebRPCMethodNameKey = "methodName"

/**
 * Web RPC argument name key.
 */
public let WebRPCArgumentsKey = "arguments"

/**
 * Invocation proxy for web RPC services.
 */
@objc public class WebRPCService {
    /**
     * Creates a new web RPC service.
     * 
     * :param: session The URL session the service will use to execute HTTP requests.
     * :param: baseURL The base URL of the service.
     */
    public init(session: NSURLSession, baseURL: NSURL) {
        self.session = session
        self.baseURL = baseURL
    }

    /**
     * The URL session the RPC service uses to execute HTTP requests.
     */
    public private(set) var session: NSURLSession

    /**
     * The base URL of the service.
     */
    public private(set) var baseURL: NSURL

    /**
     * Invokes a web RPC service method.
     *
     * :param: methodName The name of the method to invoke.
     * :param: resultHandler A callback that will be invoked upon completion of the method.
     *
     * :returns: A session data task representing the invocation request.
     */
    public func invoke(methodName: String, resultHandler: (AnyObject?, NSError?) -> Void) -> NSURLSessionDataTask? {
        return invoke(methodName, withArguments: [String: AnyObject](), resultHandler: resultHandler)
    }

    /**
     * Invokes a web RPC service method.
     *
     * :param: methodName The name of the method to invoke.
     * :param: arguments The method arguments.
     * :param: resultHandler A callback that will be invoked upon completion of the method.
     *
     * :returns: A session data task representing the invocation request.
     */
    public func invoke(methodName: String, withArguments arguments: [String: AnyObject], resultHandler: (AnyObject?, NSError?) -> Void) -> NSURLSessionDataTask? {
        var task: NSURLSessionDataTask?

        if let requestURL = NSURL(string: methodName, relativeToURL: baseURL) {
            var parameters = ""

            func encode(value: AnyObject) -> String {
                return value.description.stringByAddingPercentEncodingWithAllowedCharacters(.URLQueryAllowedCharacterSet())!
            }

            for (key, value) in arguments {
                if (!parameters.isEmpty) {
                    parameters += "&"
                }

                if (value is [AnyObject]) {
                    var values = value as! [AnyObject]

                    if (values.count > 0) {
                        parameters += key + "=" + join("&" + key + "=", values.map(encode))
                    }
                } else {
                    parameters += key + "=" + encode(value)
                }
            }

            let request = NSMutableURLRequest(URL: requestURL)

            request.HTTPMethod = "POST"
            request.HTTPBody = parameters.dataUsingEncoding(NSUTF8StringEncoding)

            var resultHandlerQueue = NSOperationQueue.currentQueue()

            task = session.dataTaskWithRequest(request) {(data, response, var error: NSError?) in
                var result: AnyObject?

                if (error == nil) {
                    let statusCode = (response as! NSHTTPURLResponse).statusCode

                    if (statusCode == 200) {
                        if (data.length > 0) {
                            result = NSJSONSerialization.JSONObjectWithData(data, options: NSJSONReadingOptions.AllowFragments, error: &error)
                        }
                    } else {
                        error = NSError(domain: WebRPCServiceErrorDomain, code: statusCode, userInfo: [
                            WebRPCMethodNameKey: methodName,
                            WebRPCArgumentsKey: arguments
                        ])
                    }
                }

                resultHandlerQueue?.addOperationWithBlock() {
                    resultHandler(result, error)
                }
            }

            task!.resume()
        }

        return task
    }
}
