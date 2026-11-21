import UIKit

class ResponseViewController: UIViewController {
    
    @IBOutlet weak var tvRole: UILabel!
    @IBOutlet weak var tvQuestionText: UILabel!
    @IBOutlet weak var tvAnswerAText: UILabel!
    @IBOutlet weak var tvAnswerBText: UILabel!
    @IBOutlet weak var tvAnswerCText: UILabel!
    @IBOutlet weak var tvChaserLocked: UILabel!
    @IBOutlet weak var tvChallengerLocked: UILabel!
    @IBOutlet weak var btAnswerA: UIButton!
    @IBOutlet weak var btAnswerB: UIButton!
    @IBOutlet weak var btAnswerC: UIButton!
    @IBOutlet weak var pbProgressIndicator: UIActivityIndicatorView!

    
    private var role: Role!
    private var client = SocketManager.shared.client

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
        
        client.onMessageReceived = { [weak self] message in
            DispatchQueue.main.async {
                guard let self = self else { return }
                
                if message == "GAME_START" {
                    self.pbProgressIndicator.isHidden = true
                } else if message == "GAME_END" {
                    self.client.disconnect()
                    self.dismiss(animated: true)
                } else if message.starts(with: "QUESTION:") {
                    let payload = message[message.index(after: message.firstIndex(of: ":")!)...]
                    let parts = payload.split(separator: "\\").map(String.init)
                    
                    self.tvQuestionText.text = parts[0]
                    self.tvAnswerAText.text = parts[1]
                    self.tvAnswerBText.text = parts[2]
                    self.tvAnswerCText.text = parts[3]
                    
                    [self.btAnswerA, self.btAnswerB, self.btAnswerC].forEach { button in
                        button?.layer.borderWidth = .zero
                        button?.tintColor = UIColor(named: "color-purple")!
                        button?.isEnabled = true
                    }
                } else if message.starts(with: "REVEAL:") {
                    let payload = message[message.index(after: message.firstIndex(of: ":")!)...]
                    let parts = payload.split(separator: "\\")
                    
                    let targetButton = switch parts[1] {
                        case "A": self.btAnswerA
                        case "B": self.btAnswerB
                        default: self.btAnswerC
                    }
                    
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
            }
        }

        client.onDisconnected = { [weak self] _ in
            DispatchQueue.main.async {
                self?.dismiss(animated: true)
            }
        }
        
        let buttons = ["A": btAnswerA, "B": btAnswerB, "C": btAnswerC]
        
        buttons.forEach {letter, button in
            button?.addAction(UIAction { _ in
                self.client.sendMessage("ANSWER:\(self.role.rawValue)\\\(letter)")
                buttons.forEach {_, bt in bt?.isEnabled = false}
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

