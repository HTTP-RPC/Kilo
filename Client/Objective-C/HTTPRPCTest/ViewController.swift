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
    @IBOutlet var sumCell: UITableViewCell!
    @IBOutlet var sumAllCell: UITableViewCell!
    @IBOutlet var inverseCell: UITableViewCell!
    @IBOutlet var charactersCell: UITableViewCell!
    @IBOutlet var selectionCell: UITableViewCell!
    @IBOutlet var putCell: UITableViewCell!
    @IBOutlet var deleteCell: UITableViewCell!
    @IBOutlet var statisticsCell: UITableViewCell!
    @IBOutlet var testDataCell: UITableViewCell!
    @IBOutlet var voidCell: UITableViewCell!
    @IBOutlet var nullCell: UITableViewCell!
    @IBOutlet var localeCodeCell: UITableViewCell!
    @IBOutlet var userNameCell: UITableViewCell!
    @IBOutlet var userRoleStatusCell: UITableViewCell!
    @IBOutlet var attachmentInfoCell: UITableViewCell!
    @IBOutlet var dateCell: UITableViewCell!
    @IBOutlet var datesCell: UITableViewCell!
    @IBOutlet var echoCell: UITableViewCell!
    @IBOutlet var delayedResultCell: UITableViewCell!
    @IBOutlet var longListCell: UITableViewCell!

    override func loadView() {
        // Load view from markup
        view = LMViewBuilder.view(withName: "ViewController", owner: self, root: nil)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

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
        serviceProxy.authentication = WSBasicAuthentication(username: "tomcat", password: "tomcat")

        // Sum
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/sum", arguments: ["a": 2, "b": 4]) {(result, error) in
            self.validate(result as? Int == 6, error: error, cell: self.sumCell)
        }

        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/sum", arguments: ["values": [1, 2, 3, 4]]) {(result, error) in
            self.validate(result as? Int == 10, error: error, cell: self.sumAllCell)
        }

        // Inverse
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/inverse", arguments: ["value": true]) {(result, error) in
            self.validate(result as? Bool == false, error: error, cell: self.inverseCell)
        }

        // Characters
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/characters", arguments: ["text": "Héllo, World!"]) {(result, error) in
            self.validate(result as? NSArray == ["H", "é", "l", "l", "o", ",", " ", "W", "o", "r", "l", "d", "!"], error: error, cell: self.charactersCell)
        }

        // Selection
        serviceProxy.invoke("POST", path: "/httprpc-server-test/test/selection", arguments: ["items": ["å", "b", "c", "d"]]) {(result, error) in
            self.validate(result as? String == "å, b, c, d", error: error, cell: self.selectionCell)
        }

        // Put
        serviceProxy.invoke("PUT", path: "/httprpc-server-test/test", arguments: ["value": "héllo"]) {(result, error) in
            self.validate(result as? String == "héllo", error: error, cell: self.putCell)
        }

        // Delete
        serviceProxy.invoke("DELETE", path: "/httprpc-server-test/test", arguments: ["value": 101]) {(result, error) in
            self.validate(result as? Int == 101, error: error, cell: self.deleteCell)
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
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/user/name") {(result, error) in
            self.validate(result as? String == "tomcat", error: error, cell: self.userNameCell)
        }

        // User role status
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/user/roleStatus", arguments: ["role": "tomcat"]) {(result, error) in
            self.validate(result as? Bool == true, error: error, cell: self.userRoleStatusCell)
        }

        // Attachment info
        let mainBundle = Bundle.main

        let textTestURL = mainBundle.url(forResource: "test", withExtension: "txt")!
        let imageTestURL = mainBundle.url(forResource: "test", withExtension: "jpg")!

        serviceProxy.invoke("POST", path: "/httprpc-server-test/test/attachmentInfo",
            arguments:["text": "héllo", "attachments": [textTestURL, imageTestURL]],
            resultHandler: handleAttachmentInfoResult)

        // Dates
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/echo", arguments: ["date": 0]) {(result, error) in
            self.validate(result as? Int == 0, error: error, cell: self.dateCell)
        }

        let dates: NSArray = ["2016-09-15", "2016-09-16"]

        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/echo", arguments: ["dates": dates]) {(result, error) in
            self.validate(result as? NSArray == dates, error: error, cell: self.datesCell)
        }

        // Echo
        serviceProxy.invoke("POST", path: "/httprpc-server-test/test/echo", arguments:["attachment": imageTestURL]) {(result, error) in
            self.validate(result != nil, error: error, cell: self.echoCell)

            self.echoCell.imageView?.image = result as? UIImage
        }

        // Long list
        let task = serviceProxy.invoke("GET", path: "/httprpc-server-test/test/longList") {(result, error) in
            self.validate(error != nil, error: error, cell: self.longListCell)
        }

        Timer.scheduledTimer(timeInterval: 1, target: BlockOperation(block: {
            task!.cancel()
        }), selector: #selector(Operation.main), userInfo: nil, repeats: false)

        // Delayed result
        serviceProxy.invoke("GET", path: "/httprpc-server-test/test/delayedResult", arguments: ["result": "abcdefg", "delay": 9000]) {(result, error) in
            self.validate(error != nil, error: error, cell: self.delayedResultCell)
        }
    }

    func handleAttachmentInfoResult(_ result: Any?, error: Error?) {
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

    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()

        tableView.contentInset = UIEdgeInsets(top: topLayoutGuide.length, left: 0, bottom: bottomLayoutGuide.length, right: 0)
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

