import UIKit
import AVFoundation

class HostViewController: UIViewController {
    
    private let role: Role = .host
    
    @IBOutlet weak var btEndGame: UIButton!
    @IBOutlet weak var lbChallengerStatus: UILabel!
    @IBOutlet weak var lbChaserStatus: UILabel!
    @IBOutlet weak var pvLockoutTimer: UIProgressView!
    @IBOutlet weak var lbQuestionText: UILabel!
    @IBOutlet weak var lbAnswerA: UILabel!
    @IBOutlet weak var lbAnswerB: UILabel!
    @IBOutlet weak var lbAnswerC: UILabel!
    @IBOutlet weak var btRevealChallenger: UIButton!
    @IBOutlet weak var btRevealCorrect: UIButton!
    @IBOutlet weak var btRevealChaser: UIButton!
    @IBOutlet weak var btNextQuestion: UIButton!
    
    private var challengerLocked = false
    private var chaserLocked = false
    private var timerExpired = false
    private var audioPlayer: AVAudioPlayer?
    
    private var optionFillColors = [
        "A": UIColor(named: "color-purple")!,
        "B": UIColor(named: "color-purple")!,
        "C": UIColor(named: "color-purple")!
    ]
    private var optionHasRedBorder = [
        "A": false,
        "B": false,
        "C": false
    ]
    private let client = SocketManager.shared.client

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        
        let optionViews = [
            "A": lbAnswerA,
            "B": lbAnswerB,
            "C": lbAnswerC
        ]
        
        optionViews.forEach({ letter, view in
            if let label = view {
                renderOptionCard(letter: letter, view: label)
            }
        })
        
        if let soundURL = Bundle.main.url(forResource: "timer", withExtension: "mp3") {
                    audioPlayer = try? AVAudioPlayer(contentsOf: soundURL)
                    audioPlayer?.prepareToPlay()
        }
        
        client.onMessageReceived = { [weak self] message in
            DispatchQueue.main.async {
                guard let self = self else { return }
                if(message == "GAME_END") {
                    self.stopCountdown()
                    self.client.disconnect()
                    self.dismiss(animated: true)
                }
                else if(message.starts(with: "QUESTION:")) {
                    self.stopCountdown()
                    self.challengerLocked = false
                    self.chaserLocked = false
                    self.timerExpired = false
                    
                    let parts = String(message[message.index(after: message.firstIndex(of: ":")!)...]).split(separator: "\\").map(String.init)
                    self.lbQuestionText.text = parts[0]
                    self.lbAnswerA.text = "A: \(parts[1])"
                    self.lbAnswerB.text = "B: \(parts[2])"
                    self.lbAnswerC.text = "C: \(parts[3])"
                    
                    self.lbChallengerStatus.text = "Challenger: Waiting"
                    self.lbChallengerStatus.backgroundColor = .white
                    self.lbChallengerStatus.textColor = .black
                    self.lbChaserStatus.text = "Chaser: Waiting"
                    self.lbChaserStatus.backgroundColor = .white
                    self.lbChaserStatus.textColor = .black
                    
                    self.btRevealChallenger.isEnabled = false
                    self.btRevealCorrect.isEnabled = false
                    self.btRevealChaser.isEnabled = false
                    self.btNextQuestion.isEnabled = false
                    
                    self.optionFillColors.forEach({ letter, _ in
                        self.optionFillColors[letter] = UIColor(named: "color-purple")!
                        self.optionHasRedBorder[letter] = false
                        self.renderOptionCard(letter: letter, view: optionViews[letter]!!)
                    })
                }
                else if(message.starts(with: "LOCKED:")) {
                    let lockedRole = Role(rawValue: message.components(separatedBy: ":")[1])
                    if(lockedRole == .challenger) {
                        self.challengerLocked = true
                        self.lbChallengerStatus.text = "Challenger: Locked"
                        self.lbChallengerStatus.backgroundColor = UIColor(named: "color-blue")!
                        self.lbChallengerStatus.textColor = .white
                        self.btRevealChallenger.isEnabled = true
                    }
                    else if(lockedRole == .chaser) {
                        self.chaserLocked = true
                        self.lbChaserStatus.text = "Chaser: Locked"
                        self.lbChaserStatus.backgroundColor = UIColor(named: "color-red")!
                        self.lbChaserStatus.textColor = .white
                        self.btRevealChaser.isEnabled = true
                    }
                    self.updateCorrectButtonState()
                }
                else if(message == "TIMER_START") {
                    self.startCountdown()
                }
                else if(message == "TIMER_STOP") {
                    self.stopCountdown()
                }
                else if(message == "TIMER_UP") {
                    self.stopCountdown()
                    self.timerExpired = true
                    if(!self.challengerLocked) {
                        self.lbChallengerStatus.text = "Challenger: Time Up"
                    }
                    if(!self.chaserLocked) {
                        self.lbChaserStatus.text = "Chaser: Time Up"
                    }
                    self.updateCorrectButtonState()
                }
                else if(message.starts(with: "REVEAL:")) {
                    let parts = String(message[message.index(after: message.firstIndex(of: ":")!)...]).split(separator: "\\").map(String.init)
                    let roleType = parts[0]
                    let letter = parts[1].trimmingCharacters(in: .whitespacesAndNewlines)
                    guard let targetView = optionViews[letter] ?? nil else { return }
                    if(roleType == "CHALLENGER") {
                        self.optionFillColors[letter] = UIColor(named:"color-blue")!
                    }
                    else if(roleType == "CHASER") {
                        self.optionHasRedBorder[letter] = true
                    }
                    else if(roleType == "CORRECT") {
                        self.optionFillColors[letter] = UIColor(named: "color-green")!
                    }
                    self.renderOptionCard(letter: letter, view: targetView)
                }
            }
        }
        
        client.onDisconnected = { [weak self] _ in
            DispatchQueue.main.async {
                guard let self = self else { return }
                self.stopCountdown()
                SocketManager.shared.isConnected = false
                self.dismiss(animated: true)
            }
        }
        
        btRevealChallenger.addAction(UIAction { [weak self] _ in
            guard let self = self else { return }
            self.client.sendMessage("REVEAL:CHALLENGER")
            self.btRevealChallenger.isEnabled = false
        }, for: .touchUpInside)
    
        btRevealChaser.addAction(UIAction { [weak self] _ in
            guard let self = self else { return }
            self.client.sendMessage("REVEAL:CHASER")
            self.btRevealChaser.isEnabled = false
        }, for: .touchUpInside)
        
        btRevealCorrect.addAction(UIAction { [weak self] _ in
            guard let self = self else { return }
            self.client.sendMessage("REVEAL:CORRECT")
            self.btRevealCorrect.isEnabled = false
            self.btNextQuestion.isEnabled = true
        }, for: .touchUpInside)
        
        btNextQuestion.addAction(UIAction { [weak self] _ in
            guard let self = self else { return }
            self.client.sendMessage("NEXT")
            self.btNextQuestion.isEnabled = false
        }, for: .touchUpInside)
        
        btEndGame.addAction(UIAction { [weak self] _ in
            guard let self = self else { return }
            let controller = UIAlertController(title: "End Game?", message: "This will disconnect all stations and end the current session", preferredStyle: .alert)
            controller.addAction(UIAlertAction(title: "Cancel", style: .cancel))
            controller.addAction(UIAlertAction(title: "End Game", style: .destructive, handler: { _ in
                self.client.sendMessage("END")
            }))
            self.present(controller, animated: true)
        }, for: .touchUpInside)
        
    }
    
    private func updateCorrectButtonState() {
        if((challengerLocked && chaserLocked) || timerExpired) {
            btRevealCorrect.isEnabled = true
        }
    }
    
    private func startCountdown() {
        stopCountdown()
        
        pvLockoutTimer.isHidden = false
        pvLockoutTimer.setProgress(1.0, animated: false)
        UIView.animate(withDuration: 5.0, delay: 0, options: .curveLinear) {
            self.pvLockoutTimer?.setProgress(0.0, animated: true)
        }
        audioPlayer?.currentTime = 0
        audioPlayer?.play()
    }
    
    private func stopCountdown() {
        pvLockoutTimer.layer.removeAllAnimations()
        pvLockoutTimer.isHidden = true
        if audioPlayer?.isPlaying == true {
            audioPlayer?.stop()
            audioPlayer?.currentTime = 0
        }
    }
    
    private func renderOptionCard(letter: String, view: UILabel) {
        let fillColor = optionFillColors[letter] ?? UIColor(named: "color-purple")!
        view.layer.backgroundColor = fillColor.cgColor
        view.layer.cornerRadius = 10
        view.layer.masksToBounds = true
    
        if optionHasRedBorder[letter] == true {
            view.layer.borderWidth = 4
            view.layer.borderColor = UIColor(named: "color-red")?.cgColor
        } else {
            view.layer.borderWidth = 0
            view.layer.borderColor = UIColor.clear.cgColor
        }
    }
}

