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

class PostViewController: UITableViewController {
    var userID: Int!

    let activityIndicatorView = UIActivityIndicatorView(activityIndicatorStyle: UIActivityIndicatorViewStyle.gray)

    var posts: [[String: Any]]! = nil
    
    override func viewDidLoad() {
        super.viewDidLoad()

        title = "Posts"

        tableView.estimatedRowHeight = 2
        tableView.backgroundView = activityIndicatorView
        
        tableView.register(PostCell.self, forCellReuseIdentifier: PostCell.description())
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        if (posts == nil) {
            tableView.separatorStyle = UITableViewCellSeparatorStyle.none
            activityIndicatorView.startAnimating()

            AppDelegate.serviceProxy.invoke("GET", path: "/posts", arguments: ["userId": userID]) { (result: [[String: Any]]?, error) in
                self.tableView.separatorStyle = UITableViewCellSeparatorStyle.singleLine
                self.activityIndicatorView.stopAnimating()

                if (error == nil) {
                    self.posts = result!

                    self.tableView.reloadData()
                } else {
                    NSLog(error!.localizedDescription)
                }
            }
        }
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return (posts == nil) ? 0 : posts.count
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: PostCell.description(), for: indexPath) as! PostCell

        let post = posts[indexPath.row]

        cell.title = post["title"] as? String
        cell.body = post["body"] as? String

        return cell
    }
}
