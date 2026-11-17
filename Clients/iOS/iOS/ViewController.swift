import UIKit

class ViewController: UIViewController {
    
    @IBOutlet weak var etEndpoint: UITextField!
    @IBOutlet weak var rgRoleSelect: UISegmentedControl!
    @IBOutlet weak var btConnect: UIButton!
    @IBOutlet weak var pbProgressIndicator: UIActivityIndicatorView!
    
    private var connecting = false;
    private let client = SocketManager.shared.client

        override func viewDidLoad() {
            super.viewDidLoad()
            styleBorder(view: etEndpoint, color: UIColor(named: "border-blue")!)
            styleBorder(view: rgRoleSelect, color: UIColor(named: "border-blue")!)
        }

        override func viewWillAppear(_ animated: Bool) {
            super.viewWillAppear(animated)
            // Re-sync UI if user navigates back from Host/Response screen
            let connected = SocketManager.shared.isConnected
            setInputsEnabled(!connected)
            btConnect.setTitle(connected ? "Disconnect" : "Connect", for: .normal)
            if !connected {
                styleBorder(view: etEndpoint, color: UIColor(named: "border-blue")!)
                styleBorder(view: rgRoleSelect, color: UIColor(named: "border-blue")!)
            }
        }
    
    @IBAction func onConnect() {
        
    }

    private func setInputsEnabled(_ enabled: Bool) {
            etEndpoint.isEnabled = enabled
            rgRoleSelect.isEnabled = enabled
        }

        private func styleBorder(view: UIView, color: UIColor) {
            view.layer.borderWidth = 1.5
            view.layer.borderColor = color.cgColor
            view.layer.cornerRadius = 8.0
        }

        private func showAlert(title: String, message: String) {
            let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .default))
            present(alert, animated: true)
        }
    
}

