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
    var noteList: [[String: Any]] = []

    let noteCellIdentifier = "noteCell"

    override func viewDidLoad() {
        super.viewDidLoad()

        title = Bundle.main.localizedString(forKey: "notes", value: nil, table: nil)

        navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: UIBarButtonSystemItem.add,
            target: self, action: #selector(MainViewController.add))

        tableView.estimatedRowHeight = 2
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        AppDelegate.serviceProxy.invoke("GET", path: "/httprpc-server/notes") { result, error in
            if (error == nil) {
                self.noteList = result as! [[String: Any]]

                self.tableView.reloadData()
            } else {
                NSLog(error!.localizedDescription)
            }
        }
    }

    override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return noteList.count
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let message = noteList[indexPath.row]["message"] as? String
        let date = Date(timeIntervalSince1970: noteList[indexPath.row]["date"] as! Double / 1000)

        var cell: UITableViewCell! = tableView.dequeueReusableCell(withIdentifier: noteCellIdentifier)

        if (cell == nil) {
            cell = UITableViewCell(style: .subtitle, reuseIdentifier: noteCellIdentifier)
        }

        cell.textLabel!.text = message
        cell.textLabel!.numberOfLines = 0

        cell.detailTextLabel!.text = DateFormatter.localizedString(from: date, dateStyle: .short, timeStyle: .medium)

        return cell
    }

    override func tableView(_ tableView: UITableView, commit editingStyle: UITableViewCellEditingStyle, forRowAt indexPath:IndexPath) {
        if (editingStyle == .delete) {
            let id = noteList[indexPath.row]["id"] as! Int

            AppDelegate.serviceProxy.invoke("DELETE", path: "/httprpc-server/notes", arguments: ["id": id]) { result, error in
                if (error == nil) {
                    self.noteList.remove(at: indexPath.row)

                    tableView.beginUpdates()
                    tableView.deleteRows(at: [indexPath], with: .automatic)
                    tableView.endUpdates()
                } else {
                    NSLog(error!.localizedDescription)
                }
            }
        }
    }

    @objc func add() {
        present(UINavigationController(rootViewController:AddNoteViewController()), animated: true)
    }
}
