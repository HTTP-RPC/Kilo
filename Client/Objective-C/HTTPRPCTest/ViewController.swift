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

class ViewController: UITableViewController, NSURLSessionDataDelegate {
    @IBOutlet var sumCell: UITableViewCell!
    @IBOutlet var sumAllCell: UITableViewCell!
    @IBOutlet var inverseCell: UITableViewCell!
    @IBOutlet var charactersCell: UITableViewCell!
    @IBOutlet var selectionCell: UITableViewCell!
    @IBOutlet var statisticsCell: UITableViewCell!
    @IBOutlet var testDataCell: UITableViewCell!
    @IBOutlet var voidCell: UITableViewCell!
    @IBOutlet var nullCell: UITableViewCell!
    @IBOutlet var localeCodeCell: UITableViewCell!
    @IBOutlet var userNameCell: UITableViewCell!
    @IBOutlet var userRoleStatusCell: UITableViewCell!
    @IBOutlet var attachmentInfoCell: UITableViewCell!

    override func loadView() {
        // Load view from markup
        view = LMViewBuilder.viewWithName("ViewController", owner: self, root: nil)
    }

    override func viewDidLoad() {
        super.viewDidLoad();

        // Configure session
        let configuration = NSURLSessionConfiguration.defaultSessionConfiguration()
        configuration.requestCachePolicy = NSURLRequestCachePolicy.ReloadIgnoringLocalAndRemoteCacheData

        let delegateQueue = NSOperationQueue()
        delegateQueue.maxConcurrentOperationCount = 10

        // Create service proxy
        let session = NSURLSession(configuration: configuration, delegate: self, delegateQueue: delegateQueue)

        let serviceProxy = WSWebServiceProxy(session: session, serverURL: NSURL(string: "https://localhost:8443")!)

        // Set credentials
        serviceProxy.authentication = WSBasicAuthentication(username: "tomcat", password: "tomcat")

        // Sum
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/sum", arguments: ["a": 2, "b": 4]) {(result, error) in
            self.validate(result as? Int == 6, error: error, cell: self.sumCell)
        }

        // Sum all
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/sumAll", arguments: ["values": [1, 2, 3, 4]]) {(result, error) in
            self.validate(result as? Int == 10, error: error, cell: self.sumAllCell)
        }

        // Inverse
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/inverse", arguments: ["value": true]) {(result, error) in
            self.validate(result as? Bool == false, error: error, cell: self.inverseCell)
        }

        // Characters
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/characters", arguments: ["text": "Hello, World!"]) {(result, error) in
            self.validate(result as? NSArray == ["H", "e", "l", "l", "o", ",", " ", "W", "o", "r", "l", "d", "!"], error: error, cell: self.charactersCell)
        }

        // Selection
        serviceProxy.invoke("POST", path: "/httprpc-server-test/test/selection", arguments: ["items": ["a", "b", "c", "d"]]) {(result, error) in
            self.validate(result as? String == "a, b, c, d", error: error, cell: self.selectionCell)
        }

        // Statistics
        serviceProxy.invoke("POST", path: "/httprpc-server-test/test/statistics", arguments: ["values": [1, 3, 5]]) {(result, error) in
            let statistics: Statistics? = (error == nil) ? Statistics(dictionary: result as! [String : AnyObject]) : nil

            self.validate(statistics?.count == 3 && statistics?.average == 3.0 && statistics?.sum == 9.0, error: error, cell: self.statisticsCell)
        }

        // Test data
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/testData") {(result, error) in
            self.validate(result as? NSArray == [
                ["a": "hello", "b": 1, "c": 2.0],
                ["a": "goodbye", "b": 2,"c": 4.0]
            ], error: error, cell: self.testDataCell)
        }

        // Void
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/void") {(result, error) in
            self.validate(result == nil, error: error, cell: self.voidCell)
        }

        // Null
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/null") {(result, error) in
            self.validate(result as? NSNull != nil, error: error, cell: self.nullCell)
        }

        // Locale code
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/localeCode") {(result, error) in
            self.validate(result != nil, error: error, cell: self.localeCodeCell)

            self.localeCodeCell.detailTextLabel!.text = result as? String
        }

        // User name
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/userName") {(result, error) in
            self.validate(result as? String == "tomcat", error: error, cell: self.userNameCell)
        }

        // User role status
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/userRoleStatus", arguments: ["role": "tomcat"]) {(result, error) in
            self.validate(result as? Bool == true, error: error, cell: self.userRoleStatusCell)
        }

        // Attachment info
        let mainBundle = NSBundle.mainBundle()

        let textTestURL = mainBundle.URLForResource("test", withExtension: "txt")!
        let imageTestURL = mainBundle.URLForResource("test", withExtension: "jpg")!

        serviceProxy.invoke("POST", path: "/httprpc-server-test/test/attachmentInfo",
            arguments:["text": "héllo", "attachments": [textTestURL, imageTestURL]],
            resultHandler: handleAttachmentInfoResult)
    }

    func handleAttachmentInfoResult(result: AnyObject?, error: NSError?) {
        validate(result as? NSDictionary == [
            "text": "héllo",
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
        ], error: error, cell: self.attachmentInfoCell)
    }

    func validate(condition: Bool, error: NSError?, cell: UITableViewCell) {
        if (condition) {
            cell.accessoryType = UITableViewCellAccessoryType.Checkmark
        } else {
            cell.textLabel!.textColor = UIColor.redColor()

            if (error != nil) {
                print(error!.description)
            }
        }
    }

    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()

        tableView.contentInset = UIEdgeInsets(top: topLayoutGuide.length, left: 0, bottom: bottomLayoutGuide.length, right: 0)
    }

    func URLSession(session: NSURLSession, didReceiveChallenge challenge: NSURLAuthenticationChallenge,
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Void) {
        // Allow self-signed certificates for testing purposes
        if (challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
            completionHandler(NSURLSessionAuthChallengeDisposition.UseCredential, NSURLCredential(forTrust: challenge.protectionSpace.serverTrust!))
        } else {
            completionHandler(NSURLSessionAuthChallengeDisposition.PerformDefaultHandling, nil)
        }
    }
}

