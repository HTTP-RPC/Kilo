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

class UserViewController: UITableViewController {
    let activityIndicatorView = UIActivityIndicatorView(activityIndicatorStyle: UIActivityIndicatorViewStyle.gray)

    var users: [[String: Any]]! = nil

    override func viewDidLoad() {
        super.viewDidLoad()

        title = "Users"

        tableView.estimatedRowHeight = 2
        tableView.backgroundView = activityIndicatorView

        tableView.register(UserCell.self, forCellReuseIdentifier: UserCell.description())
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        if (users == nil) {
            tableView.separatorStyle = UITableViewCellSeparatorStyle.none
            activityIndicatorView.startAnimating()

            AppDelegate.serviceProxy.invoke("GET", path: "/users") { (result: [[String: Any]]?, error) in
                self.tableView.separatorStyle = UITableViewCellSeparatorStyle.singleLine
                self.activityIndicatorView.stopAnimating()

                if (error == nil) {
                    self.users = result

                    self.tableView.reloadData()
                } else {
                    NSLog(error!.localizedDescription)
                }
            }
        }
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return (users == nil) ? 0 : users.count
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: UserCell.description(), for: indexPath) as! UserCell

        let user = users[indexPath.row]

        cell.name = user["name"] as? String
        cell.email = user["email"] as? String

        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let postViewController = PostViewController()
        
        postViewController.userID = users[indexPath.row]["id"] as! Int
        
        navigationController?.pushViewController(postViewController, animated: true)
    }
}
