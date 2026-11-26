import UIKit

class ResponseViewController: UIViewController {
    
    @IBOutlet weak var tvRole: UILabel!
    @IBOutlet weak var tvQuestionText: UILabel!
    @IBOutlet weak var tvAnswerAText: UILabel!
    @IBOutlet weak var tvAnswerBText: UILabel!
    @IBOutlet weak var tvAnswerCText: UILabel!
    @IBOutlet weak var tvChaserLocked: UILabel!
    @IBOutlet weak var tvChallengerLocked: UILabel!
    @IBOutlet weak var tvTimeUp: UILabel!
    @IBOutlet weak var btAnswerA: UIButton!
    @IBOutlet weak var btAnswerB: UIButton!
    @IBOutlet weak var btAnswerC: UIButton!
    @IBOutlet weak var pbProgressIndicator: UIActivityIndicatorView!

    
    private var role: Role!
    private var client = SocketManager.shared.client
    private var buttons: [String: UIButton] = [:]

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        tvRole.text = "My Role: \(role.rawValue.lowercased().capitalized)"
        if(role == .challenger) {
            tvRole.textColor = UIColor(named: "color-blue")!
        }
        else {
            tvRole.textColor = UIColor(named: "color-red")!
        }
        
        buttons = ["A": btAnswerA, "B": btAnswerB, "C": btAnswerC]
        
        client.onMessageReceived = { [weak self] message in
            DispatchQueue.main.async {
                guard let self = self else { return }
                
                if message == "GAME_START" {
                    self.pbProgressIndicator.isHidden = true
                } else if message == "GAME_END" {
                    self.client.disconnect()
                    SocketManager.shared.isConnected = false
                    self.dismiss(animated: true)
                } else if message.starts(with: "QUESTION:") {
                    let payload = message[message.index(after: message.firstIndex(of: ":")!)...]
                    let parts = payload.split(separator: "\\").map(String.init)
                    
                    self.tvQuestionText.text = parts[0]
                    self.tvAnswerAText.text = parts[1]
                    self.tvAnswerBText.text = parts[2]
                    self.tvAnswerCText.text = parts[3]
                    
                    self.buttons.forEach { _, button in
                        button.layer.borderWidth = .zero
                        button.tintColor = UIColor(named: "color-purple")!
                        button.isEnabled = true
                    }
                    
                    self.tvChallengerLocked.isHidden = true
                    self.tvChaserLocked.isHidden = true
                    self.tvTimeUp.isHidden = true
                    
                } else if message.starts(with: "REVEAL:") {
                    let payload = message[message.index(after: message.firstIndex(of: ":")!)...]
                    let parts = payload.split(separator: "\\").map(String.init)
                    
                    let targetButton = self.buttons[parts[1]]
                    
                    switch parts[0] {
                        case "CHALLENGER":
                            targetButton?.tintColor = UIColor(named: "color-blue")
                        case "CHASER":
                            if let btn = targetButton {
                                self.styleBorder(view: btn, color: UIColor(named: "color-red")!)
                            }
                        default:
                            targetButton?.tintColor = UIColor(named: "color-green")
                    }
                }
                else if(message.starts(with: "LOCKED:")) {
                    let locked = Role(rawValue: String(message[message.index(after: message.firstIndex(of: ":")!)...]))
                    if locked == .challenger {
                        self.tvChallengerLocked.isHidden = false
                    }
                    else {
                        self.tvChaserLocked.isHidden = false
                    }
                }
                else if(message == "TIMER_START") {
                    
                }
                else if(message == "TIME_UP") {
                    self.tvTimeUp.isHidden = false
                    self.buttons.forEach { (_, button) in button.isEnabled = false }
                }
            }
        }

        client.onDisconnected = { [weak self] _ in
            DispatchQueue.main.async {
                SocketManager.shared.isConnected = false
                self?.dismiss(animated: true)
            }
        }
        
        
        buttons.forEach {letter, button in
            button.addAction(UIAction { [weak self] _ in
                guard let self = self else { return }
                self.client.sendMessage("ANSWER:\(self.role.rawValue)\\\(letter)")
                self.buttons.forEach {_, bt in bt.isEnabled = false}
            }, for: .touchUpInside)
        }
    }


    func playingAs(role: Role) {
        self.role = role
    }
    
    private func styleBorder(view: UIView, color: UIColor) {
        view.layer.borderWidth = 1.5
        view.layer.borderColor = color.cgColor
        view.layer.cornerRadius = 8.0
    }
}

