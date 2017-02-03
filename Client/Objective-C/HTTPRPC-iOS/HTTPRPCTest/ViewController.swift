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
    @IBOutlet var postMultipartCell: UITableViewCell!
    @IBOutlet var postURLEncodedCell: UITableViewCell!
    @IBOutlet var postJSONCell: UITableViewCell!
    @IBOutlet var putCell: UITableViewCell!
    @IBOutlet var putJSONCell: UITableViewCell!
    @IBOutlet var patchCell: UITableViewCell!
    @IBOutlet var patchJSONCell: UITableViewCell!
    @IBOutlet var deleteCell: UITableViewCell!
    @IBOutlet var timeoutCell: UITableViewCell!
    @IBOutlet var cancelCell: UITableViewCell!
    @IBOutlet var errorCell: UITableViewCell!
    @IBOutlet var imageCell: UITableViewCell!
    @IBOutlet var textCell: UITableViewCell!

    override func loadView() {
        // Load view from markup
        view = LMViewBuilder.view(withName: "ViewController", owner: self, root: nil)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        title = "HTTP-RPC Test"

        edgesForExtendedLayout = UIRectEdge()

        tableView.delegate = self

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
            "flag": true,
            ]) { result, error in
            if let dictionary = result as? NSDictionary {
                self.validate(dictionary.value(forKeyPath: "string") as! String == "héllo"
                    && dictionary.value(forKeyPath: "strings") as! [String] == ["a", "b", "c"]
                    && dictionary.value(forKeyPath: "number") as! Int == 123
                    && dictionary.value(forKeyPath: "flag") as! Bool == true,
                    error: error, cell: self.getCell)
            }
        }

        // POST
        let textTestURL = Bundle.main.url(forResource: "test", withExtension: "txt")!
        let imageTestURL = Bundle.main.url(forResource: "test", withExtension: "jpg")!

        serviceProxy.encoding = WSMultipartFormData
        serviceProxy.invoke("POST", path: "/httprpc-server/test", arguments: [
            "string": "héllo",
            "strings": ["a", "b", "c"],
            "number": 123,
            "flag": true,
            "attachments": [textTestURL, imageTestURL]
            ]) { result, error in
            self.validate(result as? NSDictionary == [
                "string": "héllo",
                "strings": ["a", "b", "c"],
                "number": 123,
                "flag": true,
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
            ], error: error, cell: self.postMultipartCell)
        }

        serviceProxy.encoding = WSApplicationXWWWFormURLEncoded
        serviceProxy.invoke("POST", path: "/httprpc-server/test", arguments: [
            "string": "héllo",
            "strings": ["a", "b", "c"],
            "number": 123,
            "flag": true
            ]) { result, error in
            self.validate(result as? NSDictionary == [
                "string": "héllo",
                "strings": ["a", "b", "c"],
                "number": 123,
                "flag": true,
                "attachmentInfo": [
                ]
            ], error: error, cell: self.postURLEncodedCell)
        }

        serviceProxy.encoding = WSApplicationJSON
        serviceProxy.invoke("POST", path: "/httprpc-server/test", arguments: [
            "string": "héllo",
            "strings": ["a", "b", "c"],
            "number": 123,
            "flag": true
            ]) { result, error in
            self.validate(result as? NSDictionary == [
                "string": "héllo",
                "strings": ["a", "b", "c"],
                "number": 123,
                "flag": true
            ], error: error, cell: self.postJSONCell)
        }

        // PUT
        serviceProxy.encoding = WSMultipartFormData
        serviceProxy.invoke("PUT", path: "/httprpc-server/test", arguments: ["text": "héllo"]) { result, error in
            self.validate(result as? String == "héllo", error: error, cell: self.putCell)
        }

        serviceProxy.encoding = WSApplicationJSON
        serviceProxy.invoke("PUT", path: "/httprpc-server/test", arguments: ["text": "héllo"]) { result, error in
            self.validate(result as? NSDictionary == ["text": "héllo"], error: error, cell: self.putJSONCell)
        }

        // PATCH
        serviceProxy.encoding = WSMultipartFormData
        serviceProxy.invoke("PATCH", path: "/httprpc-server/test", arguments: ["text": "héllo"]) { result, error in
            self.validate(result as? String == "héllo", error: error, cell: self.patchCell)
        }

        serviceProxy.encoding = WSApplicationJSON
        serviceProxy.invoke("PATCH", path: "/httprpc-server/test", arguments: ["text": "héllo"]) { result, error in
            self.validate(result as? NSDictionary == ["text": "héllo"], error: error, cell: self.patchJSONCell)
        }

        // DELETE
        serviceProxy.invoke("DELETE", path: "/httprpc-server/test", arguments: ["id": 101]) { result, error in
            self.validate(result as? Bool == true, error: error, cell: self.deleteCell)
        }

        // Error
        serviceProxy.invoke("GET", path: "/httprpc-server/xyz") { result, error in
            let error = error as? NSError

            self.validate(error != nil && error!.domain == WSWebServiceErrorDomain && error!.code == 404, error: error, cell: self.errorCell)
        }

        // Timeout
        serviceProxy.invoke("GET", path: "/httprpc-server/test", arguments: ["value": 123, "delay": 6000]) { result, error in
            self.validate(error != nil, error: error, cell: self.timeoutCell)
        }

        // Cancel
        let task = serviceProxy.invoke("GET", path: "/httprpc-server/test", arguments: ["value": 123, "delay": 6000]) { result, error in
            self.validate(error != nil, error: error, cell: self.cancelCell)
        }

        Timer.scheduledTimer(timeInterval: 1, target: BlockOperation(block: {
            task!.cancel()
        }), selector: #selector(Operation.main), userInfo: nil, repeats: false)

        // Image
        serviceProxy.invoke("GET", path: "/httprpc-server/test.jpg") { result, error in
            self.validate(result != nil, error: error, cell: self.imageCell)

            self.imageCell.imageView?.image = result as? UIImage
        }

        // Image
        serviceProxy.invoke("GET", path: "/httprpc-server/test.txt") { result, error in
            self.validate(result != nil, error: error, cell: self.textCell)

            self.textCell.textLabel?.text = result as? String
        }
    }

    override func tableView(_ tableView: UITableView, canFocusRowAt indexPath: IndexPath) -> Bool {
        return true
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

