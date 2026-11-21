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
    }


    func playingAs(role: Role) {
        self.role = role
    }
    
    @IBAction func registerAnswer(sender: UIButton!) {
        
    }
    
}

