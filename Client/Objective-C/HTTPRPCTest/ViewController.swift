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
    @IBOutlet var addCell: UITableViewCell!
    @IBOutlet var addValuesCell: UITableViewCell!
    @IBOutlet var invertValueCell: UITableViewCell!
    @IBOutlet var getCharactersCell: UITableViewCell!
    @IBOutlet var getSelectionCell: UITableViewCell!
    @IBOutlet var getMapCell: UITableViewCell!
    @IBOutlet var getStatisticsCell: UITableViewCell!
    @IBOutlet var getTestDataCell: UITableViewCell!
    @IBOutlet var getVoidCell: UITableViewCell!
    @IBOutlet var getNullCell: UITableViewCell!
    @IBOutlet var getLocaleCodeCell: UITableViewCell!
    @IBOutlet var getUserNameCell: UITableViewCell!
    @IBOutlet var isUserInRoleCell: UITableViewCell!
    @IBOutlet var getAttachmentInfoCell: UITableViewCell!

    override func loadView() {
        // Load view from markup
        view = LMViewBuilder.viewWithName("ViewController", owner: self, root: nil)
    }

    override func viewDidLoad() {
        super.viewDidLoad();
        
        // Set invalid user credentials
        let credential = NSURLCredential(user: "tomcatx", password: "tomcat", persistence: NSURLCredentialPersistence.ForSession);
        let protectionSpace = NSURLProtectionSpace(host: "localhost", port: 8443, protocol: "https", realm: "tomcat",
            authenticationMethod: NSURLAuthenticationMethodHTTPBasic)

        NSURLCredentialStorage.sharedCredentialStorage().setDefaultCredential(credential, forProtectionSpace: protectionSpace)

        // Configure session
        let configuration = NSURLSessionConfiguration.defaultSessionConfiguration()
        configuration.requestCachePolicy = NSURLRequestCachePolicy.ReloadIgnoringLocalAndRemoteCacheData

        let delegateQueue = NSOperationQueue()
        delegateQueue.maxConcurrentOperationCount = 10

        let session = NSURLSession(configuration: configuration, delegate: self, delegateQueue: delegateQueue)

        // Initialize service and invoke methods
        let baseURL = NSURL(string: "https://localhost:8443/httprpc-server-test/test/")

        let serviceProxy = WSWebServiceProxy(session: session, baseURL: baseURL!)

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

        serviceProxy.invoke("add", withArguments: ["a": 2, "b": 4]) {(result, error) in
            validate(result as? Int == 6, error: error, cell: self.addCell)
        }

        serviceProxy.invoke("addValues", withArguments: ["values": [1, 2, 3, 4]]) {(result, error) in
            validate(result as? Int == 10, error: error, cell: self.addValuesCell)
        }

        serviceProxy.invoke("invertValue", withArguments: ["value": true]) {(result, error) in
            validate(result as? Bool == false, error: error, cell: self.invertValueCell)
        }

        serviceProxy.invoke("getCharacters", withArguments: ["text": "Hello, World!"]) {(result, error) in
            validate(result as? NSArray == ["H", "e", "l", "l", "o", ",", " ", "W", "o", "r", "l", "d", "!"], error: error, cell: self.getCharactersCell)
        }

        serviceProxy.invoke("getSelection", withArguments: ["items": ["a", "b", "c", "d"]]) {(result, error) in
            validate(result as? String == "a, b, c, d", error: error, cell: self.getSelectionCell)
        }

        let map = ["a": 123, "b": 456, "c": 789];

        serviceProxy.invoke("getMap", withArguments: ["map": map]) {(result, error) in
            validate(result as? NSDictionary == map, error: error, cell: self.getMapCell)
        }

        serviceProxy.invoke("getStatistics", withArguments: ["values": [1, 3, 5]]) {(result, error) in
            let statistics: Statistics? = (error == nil) ? Statistics(dictionary: result as! [String : AnyObject]) : nil

            validate(statistics?.count == 3 && statistics?.average == 3.0 && statistics?.sum == 9.0, error: error, cell: self.getStatisticsCell)
        }

        serviceProxy.invoke("getTestData") {(result, error) in
            validate(result as? NSArray == [
                ["a": "hello", "b": 1, "c": 2.0],
                ["a": "goodbye", "b": 2,"c": 4.0]
            ], error: error, cell: self.getTestDataCell)
        }

        serviceProxy.invoke("getVoid") {(result, error) in
            validate(result == nil, error: error, cell: self.getVoidCell)
        }

        serviceProxy.invoke("getNull") {(result, error) in
            validate(result as? NSNull != nil, error: error, cell: self.getNullCell)
        }

        serviceProxy.invoke("getLocaleCode") {(result, error) in
            validate(result != nil, error: error, cell: self.getLocaleCodeCell)

            self.getLocaleCodeCell.detailTextLabel!.text = result as? String
        }

        serviceProxy.invoke("getUserName") {(result, error) in
            validate(result as? String == "tomcat", error: error, cell: self.getUserNameCell)
        }

        serviceProxy.invoke("isUserInRole", withArguments: ["role": "tomcat"]) {(result, error) in
            validate(result as? Bool == true, error: error, cell: self.isUserInRoleCell)
        }

        let mainBundle = NSBundle.mainBundle()
        let textTestURL = mainBundle.URLForResource("test", withExtension: "txt")!
        let imageTestURL = mainBundle.URLForResource("test", withExtension: "jpg")!

        serviceProxy.invoke("getAttachmentInfo", withArguments:[:], attachments:["test": [textTestURL, imageTestURL]]) {(result, error) in
            let attachmentInfo = result as! [[String: AnyObject]];

            let textInfo = attachmentInfo[0];
            let imageInfo = attachmentInfo[1];
            
            validate(textInfo["contentType"] as! String == "text/plain"
                && textInfo["size"] as! Int == 26 && textInfo["checksum"] as! Int == 2412
                && imageInfo["contentType"] as! String == "image/jpeg"
                && imageInfo["size"] as! Int == 10392 && imageInfo["checksum"] as! Int == 1038036,
                error: error, cell: self.getAttachmentInfoCell)
        }
    }

    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()

        tableView.contentInset = UIEdgeInsets(top: topLayoutGuide.length, left: 0, bottom: bottomLayoutGuide.length, right: 0)
    }

    func URLSession(session: NSURLSession, didReceiveChallenge challenge: NSURLAuthenticationChallenge,
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Void) {
        // Allow self-signed certificate
        if (challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
            completionHandler(NSURLSessionAuthChallengeDisposition.UseCredential, NSURLCredential(forTrust: challenge.protectionSpace.serverTrust!))
        } else {
            completionHandler(NSURLSessionAuthChallengeDisposition.PerformDefaultHandling, nil)
        }
    }

    func URLSession(session: NSURLSession, task: NSURLSessionTask, didReceiveChallenge challenge: NSURLAuthenticationChallenge,
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Void) {
        // Re-authenticate user
        var credential: NSURLCredential
        if (challenge.previousFailureCount == 0 && challenge.proposedCredential != nil) {
            credential = challenge.proposedCredential!
        } else {
            credential = NSURLCredential(user: "tomcat", password: "tomcat", persistence: NSURLCredentialPersistence.ForSession)
        }

        completionHandler(NSURLSessionAuthChallengeDisposition.UseCredential, credential)
    }
}

