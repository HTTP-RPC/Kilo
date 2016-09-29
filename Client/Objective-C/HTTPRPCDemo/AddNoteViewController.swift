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

class AddNoteViewController: UITableViewController {
    var messageTextView: UITextView!
    
    override func loadView() {
        view = LMViewBuilder.view(withName: "AddNoteViewController", owner: self, root: nil)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        title = Bundle.main.localizedString(forKey: "addNote", value: nil, table: nil)

        navigationItem.leftBarButtonItem = UIBarButtonItem(barButtonSystemItem: UIBarButtonSystemItem.cancel,
            target: self, action: #selector(AddNoteViewController.cancel))
        navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: UIBarButtonSystemItem.done,
            target: self, action: #selector(AddNoteViewController.done))
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        messageTextView.becomeFirstResponder()
    }

    func cancel() {
        dismiss(animated: true, completion: nil)
    }

    func done() {
        AppDelegate.serviceProxy.invoke("POST", path: "/httprpc-server-demo/notes", arguments: ["message": messageTextView.text]) { result, error in
            if (error == nil) {
                self.dismiss(animated: true, completion: nil)
            } else {
                NSLog(error!.localizedDescription)
            }
        }
    }
}
