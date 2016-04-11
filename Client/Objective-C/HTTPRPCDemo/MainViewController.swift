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
    var notes: [[String: AnyObject]] = []

    static let NoteCellIdentifier = "noteCell"

    override func viewDidLoad() {
        super.viewDidLoad()

        title = NSBundle.mainBundle().localizedStringForKey("notes", value: nil, table: nil)

        navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: UIBarButtonSystemItem.Add,
            target: self, action: #selector(MainViewController.add))
    }

    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)

        AppDelegate.serviceProxy.invoke("listNotes") {(result, error) in
            // TODO If the controller has been dismissed, ignore

            if (error == nil) {
                self.notes = result as! [[String: AnyObject]]

                self.tableView.reloadData()
            } else {
                // TODO Handle error
            }
        }
    }

    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return 1
    }

    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return notes.count
    }

    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let message = notes[indexPath.row]["message"] as? String
        let date = NSDate(timeIntervalSince1970: notes[indexPath.row]["date"] as! Double / 1000)

        var cell: UITableViewCell! = tableView.dequeueReusableCellWithIdentifier(MainViewController.NoteCellIdentifier)

        if (cell == nil) {
            cell = UITableViewCell(style: .Subtitle, reuseIdentifier: MainViewController.NoteCellIdentifier)
        }

        cell.textLabel!.text = message
        cell.detailTextLabel!.text = NSDateFormatter.localizedStringFromDate(date, dateStyle: .ShortStyle, timeStyle: .MediumStyle)

        return cell
    }

    override func tableView(tableView: UITableView, commitEditingStyle editingStyle: UITableViewCellEditingStyle, forRowAtIndexPath indexPath:NSIndexPath) {
        if (editingStyle == .Delete) {
            let id = notes[indexPath.row]["id"] as! Int

            AppDelegate.serviceProxy.invoke("deleteNote", withArguments: ["id": id]) {(result, error) in
                // TODO If the controller has been dismissed, ignore

                if (error == nil) {
                    self.notes.removeAtIndex(indexPath.row)

                    tableView.beginUpdates()
                    tableView.deleteRowsAtIndexPaths([indexPath], withRowAnimation: .Automatic)
                    tableView.endUpdates()
                } else {
                    // TODO Handle error
                }
            }
        }
    }

    func add() {
        presentViewController(UINavigationController(rootViewController:AddNoteViewController()), animated: true, completion: nil)
    }
}
