//
//  AddViewController.swift
//  HTTPRPC
//
//  Created by Greg Brown on 4/7/16.
//  Copyright © 2016 HTTP-RPC. All rights reserved.
//

import UIKit
import MarkupKit
import HTTPRPC

class AddViewController: UITableViewController {
    override func loadView() {
        view = LMViewBuilder.viewWithName("AddView", owner: self, root: nil)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        title = "Add Note"

        navigationItem.leftBarButtonItem = UIBarButtonItem(barButtonSystemItem: UIBarButtonSystemItem.Cancel,
            target: self, action: #selector(AddViewController.cancel))
        navigationItem.rightBarButtonItem = UIBarButtonItem(barButtonSystemItem: UIBarButtonSystemItem.Done,
            target: self, action: #selector(AddViewController.done))

        edgesForExtendedLayout = UIRectEdge.None
    }

    func cancel() {
        dismissViewControllerAnimated(true, completion: nil)
    }

    func done() {
        // TODO

        dismissViewControllerAnimated(true, completion: nil)
    }
}
