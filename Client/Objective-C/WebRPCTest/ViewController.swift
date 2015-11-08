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
    var addValuesCell: UITableViewCell!
    var invertValueCell: UITableViewCell!
    var getCharactersCell: UITableViewCell!
    var getSelectionCell: UITableViewCell!
    var getStatisticsCell: UITableViewCell!
    var getTestDataCell: UITableViewCell!
    var getVoidCell: UITableViewCell!
    var getNullCell: UITableViewCell!
    var getLocaleCodeCell: UITableViewCell!
    var getUserNameCell: UITableViewCell!

    override func loadView() {
        let tableView = UITableView()

        tableView.contentInset = UIEdgeInsets(top: 20, left: 0, bottom: 0, right: 0)
        tableView.dataSource = self

        cells = [];

        addCell = UITableViewCell(style: UITableViewCellStyle.Value1, reuseIdentifier: nil)
        addCell.textLabel!.text = "add()"
        cells.append(addCell)

        addValuesCell = UITableViewCell(style: UITableViewCellStyle.Value1, reuseIdentifier: nil)
        addValuesCell.textLabel!.text = "addArray()"
        cells.append(addValuesCell)

        invertValueCell = UITableViewCell(style: UITableViewCellStyle.Value1, reuseIdentifier: nil)
        invertValueCell.textLabel!.text = "invertValue()"
        cells.append(invertValueCell)

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

        view = tableView
    }

    override func viewDidLoad() {
        // Set invalid user credentials
        let credential = NSURLCredential(user: "tomcatx", password: "tomcat", persistence: NSURLCredentialPersistence.ForSession);
        let protectionSpace = NSURLProtectionSpace(host: "localhost", port: 8443, `protocol`: "https", realm: "tomcat",
            authenticationMethod: NSURLAuthenticationMethodHTTPBasic)

        NSURLCredentialStorage.sharedCredentialStorage().setDefaultCredential(credential, forProtectionSpace: protectionSpace)

        // Configure session
        let configuration = NSURLSessionConfiguration.defaultSessionConfiguration()
        configuration.requestCachePolicy = NSURLRequestCachePolicy.ReloadIgnoringLocalAndRemoteCacheData

        let delegateQueue = NSOperationQueue()
        delegateQueue.maxConcurrentOperationCount = 10

        let session = NSURLSession(configuration: configuration, delegate: self, delegateQueue: delegateQueue)

        // Initialize service and invoke methods
        let baseURL = NSURL(string: "https://localhost:8443/webrpc-test-server/test/")

        let service = WSWebServiceProxy(session: session, baseURL: baseURL!)

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

        service.invoke("add", withArguments: ["a": 2, "b": 4]) {(result, error) in
            validate(result as? Int == 6, error: error, cell: self.addCell)
        }

        service.invoke("addValues", withArguments: ["values": [1, 2, 3, 4]]) {(result, error) in
            validate(result as? Int == 10, error: error, cell: self.addValuesCell)
        }

        service.invoke("invertValue", withArguments: ["value": true]) {(result, error) in
            validate(result as? Bool == false, error: error, cell: self.invertValueCell)
        }

        service.invoke("getCharacters", withArguments: ["text": "Hello, World!"]) {(result, error) in
            validate(result as? NSArray == ["H", "e", "l", "l", "o", ",", " ", "W", "o", "r", "l", "d", "!"], error: error, cell: self.getCharactersCell)
        }

        service.invoke("getSelection", withArguments: ["items": ["a", "b", "c", "d"]]) {(result, error) in
            validate(result as? String == "a, b, c, d", error: error, cell: self.getSelectionCell)
        }

        service.invoke("getStatistics", withArguments: ["values": [1, 3, 5]]) {(result, error) in
            validate(result as? NSDictionary == [
                "count": 3,
                "sum": 9.0,
                "average": 3.0
            ], error: error, cell: self.getStatisticsCell)
        }

        service.invoke("getTestData") {(result, error) in
            validate(result as? NSArray == [
                ["a": "hello", "b": 1, "c": 2.0],
                ["a": "goodbye", "b": 2,"c": 4.0]
            ], error: error, cell: self.getTestDataCell)
        }

        service.invoke("getVoid") {(result, error) in
            validate(result == nil, error: error, cell: self.getVoidCell)
        }

        service.invoke("getNull") {(result, error) in
            validate(result as? NSNull != nil, error: error, cell: self.getNullCell)
        }

        service.invoke("getLocaleCode") {(result, error) in
            validate(result != nil, error: error, cell: self.getLocaleCodeCell)

            self.getLocaleCodeCell.detailTextLabel!.text = result as? String
        }

        service.invoke("getUserName") {(result, error) in
            validate(result as? String == "tomcat", error: error, cell: self.getUserNameCell)
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

