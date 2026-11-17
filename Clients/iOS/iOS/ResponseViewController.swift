import UIKit

class ResponseViewController: UIViewController {
    
    private var role: Role!

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
    }


    func playingAs(role: Role) {
        self.role = role
    }
    
}

