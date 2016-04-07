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
import HTTPRPC

class ViewController: UITableViewController {
    override func viewDidLoad() {
        super.viewDidLoad()

        title = "Notes" // TODO i81n

        navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: UIBarButtonSystemItem.Add,
            target: self, action: #selector(ViewController.add))

        edgesForExtendedLayout = UIRectEdge.None
    }

    override func viewWillAppear(animated: Bool) {
        super.viewWillAppear(animated)

        // TODO Refresh list
    }

    func add() {
        presentViewController(UINavigationController(rootViewController:AddViewController()), animated: true, completion: nil)
    }

    override func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        // TODO
        return 0
    }

    override func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        // TODO
        return 0
    }

    override func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        // TODO Reuse identifier
        let cell = tableView.dequeueReusableCellWithIdentifier("reuseIdentifier", forIndexPath: indexPath)

        // TODO

        return cell
    }

    override func tableView(tableView: UITableView, didSelectRowAtIndexPath indexPath: NSIndexPath) {
        // TODO
    }
}
