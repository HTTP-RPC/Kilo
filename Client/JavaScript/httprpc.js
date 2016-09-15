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

/**
 * Creates a new HTTP-RPC service proxy.
 * 
 * @param timeout The request timeout.
 */
var WebServiceProxy = function(timeout) {
    this.timeout = timeout;
}

/**
 * Executes a service operation.
 *
 * @param method The HTTP verb associated with the request.
 * @param path The path associated with the request.
 * @param arguments The request arguments.
 * @param resultHandler A callback that will be invoked upon completion of the request.
 *
 * @return An XMLHttpRequest object representing the invocation request.
 */
WebServiceProxy.prototype.invoke = function(method, path, arguments, resultHandler) {
    // Construct query
    var query = "";

    for (name in arguments) {
        var value = arguments[name];
        
        var values;
        if (value instanceof Array) {
            values = value;
        } else {
            values = [value];
        }
        
        for (var i = 0; i < values.length; i++) {
            var element = values[i];

            if (element == null) {
                continue;
            }

            if (query.length > 0) {
                query += "&";
            }
            
            query += encodeURIComponent(name) + "=" + encodeURIComponent(element);
        }
    }

    // Execute request
    var request = new XMLHttpRequest();

    request.timeout = this.timeout;

    request.onreadystatechange = function() {
        if (request.readyState == 4) {
            var status = request.status;

            if (Math.floor(status / 100) == 2) {
                var responseText = request.responseText;

                resultHandler((responseText.length > 0) ? JSON.parse(responseText) : null, null);
            } else {
                resultHandler(null, status);
            }
        }
    }

    if (method.toLowerCase() == "post") {
        request.open(method, path, true);
        request.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        request.send(query);
    } else {
        if (query.length > 0) {
            path += "?" + query;
        }

        request.open(method, path, true);
        request.send();
    }

    return request;
}
