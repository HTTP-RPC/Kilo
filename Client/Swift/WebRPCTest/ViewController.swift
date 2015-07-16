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
import WebRPC

class ViewController: UIViewController, UITableViewDataSource, NSURLSessionDataDelegate {
    var cells: [UITableViewCell]!

    var addCell: UITableViewCell!
    var addArrayCell: UITableViewCell!
    var addVarargsCell: UITableViewCell!
    var getCharactersCell: UITableViewCell!
    var getSelectionCell: UITableViewCell!
    var getStatisticsCell: UITableViewCell!
    var getTestDataCell: UITableViewCell!
    var getVoidCell: UITableViewCell!
    var getNullCell: UITableViewCell!
    var getLocaleCodeCell: UITableViewCell!
    var getUserNameCell: UITableViewCell!
    var isUserInRoleCell: UITableViewCell!

    var configuration: NSURLSessionConfiguration!
    var session: NSURLSession!

    var service: WebRPCService!

    static let baseURL = NSURL(string: "https://localhost:8443/webrpc-test-1.0/test/")

    override func loadView() {
        var tableView = UITableView()

        tableView.contentInset = UIEdgeInsets(top: 20, left: 0, bottom: 0, right: 0)
        tableView.dataSource = self

        cells = [];

        addCell = UITableViewCell(style: UITableViewCellStyle.Value1, reuseIdentifier: nil)
        addCell.textLabel!.text = "add()"
        cells.append(addCell)

        addArrayCell = UITableViewCell(style: UITableViewCellStyle.Value1, reuseIdentifier: nil)
        addArrayCell.textLabel!.text = "addArray()"
        cells.append(addArrayCell)

        addVarargsCell = UITableViewCell(style: UITableViewCellStyle.Value1, reuseIdentifier: nil)
        addVarargsCell.textLabel!.text = "addVarargs()"
        cells.append(addVarargsCell)

        getCharactersCell = UITableViewCell(style: UITableViewCellStyle.Value1, reuseIdentifier: nil)
        getCharactersCell.textLabel!.text = "getCharacters()"
        cells.append(getCharactersCell)

        getSelectionCell = UITableViewCell(style: UITableViewCellStyle.Value1, reuseIdentifier: nil)
        getSelectionCell.textLabel!.text = "getSelection()"
        cells.append(getSelectionCell)

        getStatisticsCell = UITableViewCell(style: UITableViewCellStyle.Value1, reuseIdentifier: nil)
        getStatisticsCell.textLabel!.text = "getStatistics()"
        cells.append(getStatisticsCell)

        getTestDataCell = UITableViewCell(style: UITableViewCellStyle.Value1, reuseIdentifier: nil)
        getTestDataCell.textLabel!.text = "getTestData()"
        cells.append(getTestDataCell)

        getVoidCell = UITableViewCell(style: UITableViewCellStyle.Value1, reuseIdentifier: nil)
        getVoidCell.textLabel!.text = "getVoid()"
        cells.append(getVoidCell)

        getNullCell = UITableViewCell(style: UITableViewCellStyle.Value1, reuseIdentifier: nil)
        getNullCell.textLabel!.text = "getNull()"
        cells.append(getNullCell)

        getLocaleCodeCell = UITableViewCell(style: UITableViewCellStyle.Value1, reuseIdentifier: nil)
        getLocaleCodeCell.textLabel!.text = "getLocaleCode()"
        cells.append(getLocaleCodeCell)

        getUserNameCell = UITableViewCell(style: UITableViewCellStyle.Value1, reuseIdentifier: nil)
        getUserNameCell.textLabel!.text = "getUserName()"
        cells.append(getUserNameCell)

        isUserInRoleCell = UITableViewCell(style: UITableViewCellStyle.Value1, reuseIdentifier: nil)
        isUserInRoleCell.textLabel!.text = "isUserInRole()"
        cells.append(isUserInRoleCell)

        view = tableView
    }

    override func viewDidLoad() {
        // Set invalid user credentials
        let credential = NSURLCredential(user: "tomcatx", password: "tomcat", persistence: NSURLCredentialPersistence.ForSession);
        let protectionSpace = NSURLProtectionSpace(host: "localhost", port: 8443, `protocol`: "https", realm: "tomcat",
            authenticationMethod: NSURLAuthenticationMethodHTTPBasic)

        NSURLCredentialStorage.sharedCredentialStorage().setDefaultCredential(credential, forProtectionSpace: protectionSpace)

        // Configure session
        configuration = NSURLSessionConfiguration.defaultSessionConfiguration()

        configuration.requestCachePolicy = NSURLRequestCachePolicy.ReloadIgnoringLocalAndRemoteCacheData

        var delegateQueue = NSOperationQueue()
        delegateQueue.maxConcurrentOperationCount = 10

        session = NSURLSession(configuration: configuration, delegate: self, delegateQueue: delegateQueue)

        // Initialize service and invoke methods
        service = WebRPCService(session: session, baseURL: ViewController.baseURL!)

        func validate(condition: Bool, error: NSError?, cell: UITableViewCell) {
            if (condition) {
                cell.accessoryType = UITableViewCellAccessoryType.Checkmark
            } else {
                cell.textLabel!.textColor = UIColor.redColor()

                if (error != nil) {
                    println(error!.description)
                }
            }
        }

        service.invoke("add", withArguments: ["a": 2, "b": 4]) {(result, error) in
            validate(result as? Int == 6, error, self.addCell)
        }

        service.invoke("addArray", withArguments: ["values": [1, 2, 3, 4]]) {(result, error) in
            validate(result as? Int == 10, error, self.addArrayCell)
        }

        service.invoke("addVarargs", withArguments: ["values": [1, 3, 5, 7, 9]]) {(result, error) in
            validate(result as? Int == 25, error, self.addVarargsCell)
        }

        service.invoke("getCharacters", withArguments: ["text": "Hello, World!"]) {(result, error) in
            validate(result as? NSArray == ["H", "e", "l", "l", "o", ",", " ", "W", "o", "r", "l", "d", "!"], error, self.getCharactersCell)
        }

        service.invoke("getSelection", withArguments: ["items": ["a", "b", "c", "d"]]) {(result, error) in
            validate(result as? String == "a, b, c, d", error, self.getSelectionCell)
        }

        service.invoke("getStatistics", withArguments: ["values": [1, 3, 5]]) {(result, error) in
            validate(result as? NSDictionary == ["count": 3, "average": 3.0, "sum": 9.0], error, self.getStatisticsCell)

            let statistics = Statistics(dictionary: result as! [String : AnyObject])
            println("count = \(statistics.count), sum = \(statistics.sum), average = \(statistics.average)")
        }

        service.invoke("getTestData") {(result, error) in
            validate(result as? NSArray == [
                ["a": "hello", "b": 1, "c": 2.0],
                ["a": "goodbye", "b": 2,"c": 4.0]
            ], error, self.getTestDataCell)
        }

        service.invoke("getVoid") {(result, error) in
            validate(result == nil, error, self.getVoidCell)
        }

        service.invoke("getNull") {(result, error) in
            validate(result as? NSNull != nil, error, self.getNullCell)
        }

        service.invoke("getLocaleCode") {(result, error) in
            validate(result as? String == NSLocale.currentLocale().localeIdentifier, error, self.getLocaleCodeCell)
        }

        service.invoke("getUserName") {(result, error) in
            validate(result as? String == "tomcat", error, self.getUserNameCell)
        }

        service.invoke("isUserInRole", withArguments: ["role": "tomcat"]) {(result, error) in
            validate(result as? Bool == true, error, self.isUserInRoleCell)
        }
    }

    // Table view delegate methods
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }

    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return cells.count
    }

    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        return cells[indexPath.row]
    }

    // URL session data delegate methods
    func URLSession(session: NSURLSession, didReceiveChallenge challenge: NSURLAuthenticationChallenge,
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential!) -> Void) {
        // Allow self-signed certificate
        if (challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust) {
            completionHandler(NSURLSessionAuthChallengeDisposition.UseCredential, NSURLCredential(forTrust: challenge.protectionSpace.serverTrust))
        } else {
            completionHandler(NSURLSessionAuthChallengeDisposition.PerformDefaultHandling, nil)
        }
    }

    func URLSession(session: NSURLSession, task: NSURLSessionTask, didReceiveChallenge challenge: NSURLAuthenticationChallenge,
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential!) -> Void) {
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

