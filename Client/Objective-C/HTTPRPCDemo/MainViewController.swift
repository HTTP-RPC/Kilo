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

    // TODO Move this to AppDelegate; see PDB example
    var serviceProxy: WSWebServiceProxy!

    static let NoteCellIdentifier = "noteCell"

    override func loadView() {
        view = LMViewBuilder.viewWithName("MainView", owner: self, root: nil)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        title = NSBundle.mainBundle().localizedStringForKey("notes", value: nil, table: nil)

        navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: UIBarButtonSystemItem.Add,
            target: self, action: #selector(MainViewController.add))

        edgesForExtendedLayout = UIRectEdge.None

        tableView.dataSource = self
        tableView.delegate = self

        tableView.registerClass(UITableViewCell.self, forCellReuseIdentifier: MainViewController.NoteCellIdentifier)
    }

    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)

        // TODO Refresh list
    }

    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        return tableView.numberOfSections
    }

    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return notes.count
    }

    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCellWithIdentifier(MainViewController.NoteCellIdentifier)!

        cell.textLabel!.text = notes[indexPath.row]["subject"] as? String
        cell.accessoryType = UITableViewCellAccessoryType.DisclosureIndicator

        return cell
    }

    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        // TODO
    }

    func add() {
        presentViewController(UINavigationController(rootViewController:AddNoteViewController()), animated: true, completion: nil)
    }
}
