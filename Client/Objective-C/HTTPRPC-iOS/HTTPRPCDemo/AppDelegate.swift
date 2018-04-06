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

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {
    private(set) static var serviceProxy: WSWebServiceProxy!

    var window: UIWindow?

    func application(_ application: UIApplication, willFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {
        AppDelegate.serviceProxy = WSWebServiceProxy(session: URLSession.shared, serverURL: URL(string: "http://localhost:8080")!)
        AppDelegate.serviceProxy.encoding = WSApplicationXWWWFormURLEncoded

        return true
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {
        window = UIWindow()

        window?.rootViewController = UINavigationController(rootViewController: MainViewController())

        window?.backgroundColor = UIColor.white
        window?.frame = UIScreen.main.bounds

        window?.makeKeyAndVisible()

        return true
    }
}
