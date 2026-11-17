import UIKit

class ViewController: UIViewController {
    
    @IBOutlet weak var etEndpoint: UITextField!
    @IBOutlet weak var rgRoleSelect: UISegmentedControl!
    @IBOutlet weak var btConnect: UIButton!
    @IBOutlet weak var pbProgressIndicator: UIActivityIndicatorView!
    
    private var connecting = false
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
        styleBorder(view: etEndpoint, color: UIColor(named: "border-blue")!)
        styleBorder(view: rgRoleSelect, color: UIColor(named: "border-blue")!)
        
        if !SocketManager.shared.isConnected && !connecting {
            connecting = true
            setInputsEnabled(false)
            
            let selectedRole: Role =
            switch rgRoleSelect.selectedSegmentIndex {
                case 0: .challenger
                case 1: .chaser
                default: .host
            }
            
            client.onConnected = { [weak self] in
                guard let self = self else { return }
                self.connecting = false
                self.btConnect.setTitle("Disconnect", for: .normal)
                self.styleBorder(view: self.etEndpoint, color: UIColor(named: "border-green")!)
                self.styleBorder(view: self.rgRoleSelect, color: UIColor(named: "border-green")!)
                
                // Register role only after handshake succeeds
                self.client.registerRole(selectedRole)
            }
            
            client.onMessageReceived = { [weak self] message in
                guard let self = self else { return }
                print("QuizGame Received: \(message)")
                
                if message == "REGISTERED:\(selectedRole.rawValue)" {
                    // Detach listener before transitioning so next VC takes over cleanly
                    self.client.onMessageReceived = nil
                    //self.navigateToGameScreen(for: selectedRole)
                } else if message == "ERROR:ROLE_TAKEN" {
                    self.client.disconnect()
                    self.styleBorder(view: self.rgRoleSelect, color: UIColor(named: "border-red")!)
                    self.showAlert(title: "Role Taken", message: "The \(selectedRole.rawValue) station is already connected.")
                }
            }
            
            client.onDisconnected = { [weak self] error in
                guard let self = self else { return }
                self.connecting = false
                self.setInputsEnabled(true)
                self.btConnect.setTitle("Connect", for: .normal)
                
                if error != "Client closed connection" {
                    self.styleBorder(view: self.etEndpoint, color: UIColor(named: "border-red")!)
                    self.styleBorder(view: self.rgRoleSelect, color: UIColor(named: "border-red")!)
                    self.showAlert(title: "Disconnected", message: error)
                }
            }
            
            let endpoint = etEndpoint.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            client.connect(urlString: endpoint)
            
        } else if !SocketManager.shared.isConnected {
            showAlert(title: "Please Wait", message: "Connection in progress...")
        } else {
            client.disconnect()
        }
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

