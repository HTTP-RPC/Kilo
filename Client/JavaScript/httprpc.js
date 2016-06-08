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
 * @param baseURL The base URL of the service.
 */
var WebServiceProxy = function(baseURL) {
    this.baseURL = baseURL;
}

WebServiceProxy.GET_METHOD = "GET";
WebServiceProxy.POST_METHOD = "POST";
WebServiceProxy.PUT_METHOD = "PUT";
WebServiceProxy.DELETE_METHOD = "DELETE";

/**
 * Invokes an HTTP-RPC service method.
 *
 * @param method The HTTP verb associated with the remote method.
 * @param path The path associated with the remote method.
 * @param arguments The method arguments.
 * @param resultHandler A callback that will be invoked upon completion of the method.
 *
 * @return An XMLHttpRequest object representing the invocation request.
 */
WebServiceProxy.prototype.invoke = function(method, path, arguments, resultHandler) {
    var url = this.baseURL + "/" + path;

    var query = "";

    for (name in arguments) {
        var value = arguments[name];
        
        var values;
        if (value instanceof Array) {
            values = value;
        } else if (value instanceof Object) {
            values = [];
            
            for (key in value) {
                values.push(encodeURIComponent(key) + ":" + encodeURIComponent(value[key]));
            }
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

    if (method.toLowerCase() == WebServiceProxy.GET_METHOD.toLowerCase()
        || method.toLowerCase() == WebServiceProxy.DELETE_METHOD.toLowerCase()) {
        url += "?" + query;
    }

    var request = new XMLHttpRequest();

    request.onreadystatechange = function() {
        if (request.readyState == 4) {
            var status = request.status;

            if (status == 200) {
                var responseText = request.responseText;

                resultHandler((responseText.length > 0) ? JSON.parse(responseText) : null, null);
            } else {
                resultHandler(null, status);
            }
        }
    }

    request.open(method, url, true);
    request.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");

    if (method.toLowerCase() == WebServiceProxy.GET_METHOD.toLowerCase()
        || method.toLowerCase() == WebServiceProxy.DELETE_METHOD.toLowerCase()) {
        request.send();
    } else {
        request.send(query);
    }

    return request;
}
