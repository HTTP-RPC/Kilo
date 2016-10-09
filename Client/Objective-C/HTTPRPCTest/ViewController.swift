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

import UIKit
import MarkupKit
import HTTPRPC

class ViewController: UITableViewController, URLSessionDataDelegate {
    @IBOutlet var getCell: UITableViewCell!
    @IBOutlet var postCell: UITableViewCell!
    @IBOutlet var putCell: UITableViewCell!
    @IBOutlet var deleteCell: UITableViewCell!
    @IBOutlet var delayedResultCell: UITableViewCell!
    @IBOutlet var longListCell: UITableViewCell!
    @IBOutlet var imageCell: UITableViewCell!

    override func loadView() {
        // Load view from markup
        view = LMViewBuilder.view(withName: "ViewController", owner: self, root: nil)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        title = "HTTP-RPC Test"

        edgesForExtendedLayout = UIRectEdge()

        // Configure session
        let configuration = URLSessionConfiguration.default
        configuration.requestCachePolicy = NSURLRequest.CachePolicy.reloadIgnoringLocalAndRemoteCacheData
        configuration.timeoutIntervalForRequest = 3
        configuration.timeoutIntervalForResource = 3

        let delegateQueue = OperationQueue()
        delegateQueue.maxConcurrentOperationCount = 10

        // Create service proxy
        let session = URLSession(configuration: configuration, delegate: self, delegateQueue: delegateQueue)

        let serviceProxy = WSWebServiceProxy(session: session, serverURL: URL(string: "https://localhost:8443")!)

        // Set credentials
        serviceProxy.authorization = URLCredential(user: "tomcat", password: "tomcat", persistence: .none)

        // GET
        serviceProxy.invoke("GET", path: "/httprpc-server/test", arguments: [
            "string": "héllo",
            "strings": ["a", "b", "c"],
            "number": 123,
            "boolean": true,
            ]) { result, error in
            if let dictionary = result as? NSDictionary {
                self.validate(dictionary.value(forKeyPath: "string") as! String == "héllo"
                    && dictionary.value(forKeyPath: "strings") as! [String] == ["a", "b", "c"]
                    && dictionary.value(forKeyPath: "number") as! Int == 123
                    && dictionary.value(forKeyPath: "boolean") as! Bool == true,
                    error: error, cell: self.getCell)
            }
        }

        // POST
        let textTestURL = Bundle.main.url(forResource: "test", withExtension: "txt")!
        let imageTestURL = Bundle.main.url(forResource: "test", withExtension: "jpg")!

        serviceProxy.invoke("POST", path: "/httprpc-server/test", arguments: [
            "string": "héllo",
            "strings": ["a", "b", "c"],
            "number": 123,
            "boolean": true,
            "attachments": [textTestURL, imageTestURL]
            ]) { result, error in
            self.validate(result as? NSDictionary == [
                "string": "héllo",
                "strings": ["a", "b", "c"],
                "number": 123,
                "boolean": true,
                "attachmentInfo": [
                    [
                        "bytes": 26,
                        "checksum": 2412
                    ],
                    [
                        "bytes": 10392,
                        "checksum": 1038036
                    ]
                ]
            ], error: error, cell: self.postCell)
        }

        // PUT
        serviceProxy.invoke("PUT", path: "/httprpc-server/test", arguments: ["text": "héllo"]) { result, error in
            self.validate(result as? String == "göodbye", error: error, cell: self.putCell)
        }

        // DELETE
        serviceProxy.invoke("DELETE", path: "/httprpc-server/test", arguments: ["id": 101]) { result, error in
            self.validate(result as? Bool == true, error: error, cell: self.deleteCell)
        }

        // Long list
        let task = serviceProxy.invoke("GET", path: "/httprpc-server/test/longList") { result, error in
            self.validate(error != nil, error: error, cell: self.longListCell)
        }

        Timer.scheduledTimer(timeInterval: 1, target: BlockOperation(block: {
            task!.cancel()
        }), selector: #selector(Operation.main), userInfo: nil, repeats: false)

        // Delayed result
        serviceProxy.invoke("GET", path: "/httprpc-server/test/delayedResult", arguments: ["result": "abcdefg", "delay": 6000]) { result, error in
            self.validate(error != nil, error: error, cell: self.delayedResultCell)
        }

        // Image
        serviceProxy.invoke("GET", path: "/httprpc-server/test.jpg") { result, error in
            self.validate(result != nil, error: error, cell: self.imageCell)

            self.imageCell.imageView?.image = result as? UIImage
        }
    }

    func validate(_ condition: Bool, error: Error?, cell: UITableViewCell) {
        if (condition) {
            cell.accessoryType = UITableViewCellAccessoryType.checkmark
        } else {
            cell.textLabel!.textColor = UIColor.red

            if (error != nil) {
                print(error!)
            }
        }
    }

    func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge,
        completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        // Allow self-signed certificates for testing purposes
        if (challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
            completionHandler(URLSession.AuthChallengeDisposition.useCredential, URLCredential(trust: challenge.protectionSpace.serverTrust!))
        } else {
            completionHandler(URLSession.AuthChallengeDisposition.performDefaultHandling, nil)
        }
    }
}

