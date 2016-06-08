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

class MainViewController: UITableViewController {
    var noteList: [[String: AnyObject]] = []

    static let NoteCellIdentifier = "noteCell"

    override func viewDidLoad() {
        super.viewDidLoad()

        title = NSBundle.mainBundle().localizedStringForKey("notes", value: nil, table: nil)

        navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: UIBarButtonSystemItem.Add,
            target: self, action: #selector(MainViewController.add))

        tableView.estimatedRowHeight = 2
    }

    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)

        // TODO Use GET method, "notes" path
        AppDelegate.serviceProxy.invoke("GET", path: "listNotes") {(result, error) in
            if (error == nil) {
                self.noteList = result as! [[String: AnyObject]]

                self.tableView.reloadData()
            } else {
                NSLog(error!.localizedDescription)
            }
        }
    }

    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }

    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return noteList.count
    }

    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let message = noteList[indexPath.row]["message"] as? String
        let date = NSDate(timeIntervalSince1970: noteList[indexPath.row]["date"] as! Double / 1000)

        var cell: UITableViewCell! = tableView.dequeueReusableCellWithIdentifier(MainViewController.NoteCellIdentifier)

        if (cell == nil) {
            cell = UITableViewCell(style: .Subtitle, reuseIdentifier: MainViewController.NoteCellIdentifier)
        }

        cell.textLabel!.text = message
        cell.textLabel!.numberOfLines = 0

        cell.detailTextLabel!.text = NSDateFormatter.localizedStringFromDate(date, dateStyle: .ShortStyle, timeStyle: .MediumStyle)

        return cell
    }

    override func tableView(tableView: UITableView, commitEditingStyle editingStyle: UITableViewCellEditingStyle, forRowAtIndexPath indexPath:NSIndexPath) {
        if (editingStyle == .Delete) {
            let id = noteList[indexPath.row]["id"] as! Int

            // TODO Use DELETE method, "notes" path
            AppDelegate.serviceProxy.invoke("POST", path: "deleteNote", arguments: ["id": id]) {(result, error) in
                if (error == nil) {
                    self.noteList.removeAtIndex(indexPath.row)

                    tableView.beginUpdates()
                    tableView.deleteRowsAtIndexPaths([indexPath], withRowAnimation: .Automatic)
                    tableView.endUpdates()
                } else {
                    NSLog(error!.localizedDescription)
                }
            }
        }
    }

    func add() {
        presentViewController(UINavigationController(rootViewController:AddNoteViewController()), animated: true, completion: nil)
    }
}
