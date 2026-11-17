import Foundation

final class SocketManager {
    static let shared = SocketManager()
    let client = GameSocketClient()
    var isConnected = false
    
    private init() {}
}
