import Foundation

final class GameSocketClient: NSObject, URLSessionWebSocketDelegate {
    var onConnected: (() -> Void)?
    var onMessageReceived: ((String) -> Void)?
    var onDisconnected: ((String) -> Void)?
    
    private var session: URLSession?
    private var webSocketTask: URLSessionWebSocketTask?
    private var isManualDisconnect = false
    
    func connect(urlString: String) {
        guard let url = URL(string: urlString) else {
            onDisconnected?("Invalid WebSocket URL")
            return
        }
        
        isManualDisconnect = false
        let configuration = URLSessionConfiguration.default
        session = URLSession(configuration: configuration, delegate: self, delegateQueue: OperationQueue.main)
        webSocketTask = session?.webSocketTask(with: url)
        webSocketTask?.resume()
    }
    
    func sendMessage(_ message: String) {
        webSocketTask?.send(.string(message)) { [weak self] error in
            if let error = error {
                self?.handleDisconnect(reason: error.localizedDescription)
            }
        }
    }
    
    func disconnect() {
        isManualDisconnect = true
        webSocketTask?.cancel(with: .normalClosure, reason: "Client closed connection".data(using: .utf8))
        handleDisconnect(reason: "Client closed connection")
    }
    
    func registerRole(_ role: Role) {
        sendMessage("REGISTER:\(role.rawValue)")
    }
    
    // MARK: - Private Receive Loop
    
    private func receiveNextMessage() {
        webSocketTask?.receive { [weak self] result in
            guard let self = self else { return }
            
            switch result {
            case .success(let message):
                switch message {
                case .string(let text):
                    DispatchQueue.main.async {
                        self.onMessageReceived?(text)
                    }
                case .data(let data):
                    if let text = String(data: data, encoding: .utf8) {
                        DispatchQueue.main.async {
                            self.onMessageReceived?(text)
                        }
                    }
                @unknown default:
                    break
                }
                // Keep listening for the next frame
                self.receiveNextMessage()
                
            case .failure(let error):
                if !self.isManualDisconnect {
                    self.handleDisconnect(reason: error.localizedDescription)
                }
            }
        }
    }
    
    private func handleDisconnect(reason: String) {
        webSocketTask = nil
        session?.invalidateAndCancel()
        session = nil
        
        DispatchQueue.main.async {
            SocketManager.shared.isConnected = false
            self.onDisconnected?(reason)
        }
    }
    
    // MARK: - URLSessionWebSocketDelegate
    
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didOpenWithProtocol protocol: String?) {
        receiveNextMessage()
        DispatchQueue.main.async {
            SocketManager.shared.isConnected = true
            self.onConnected?()
        }
    }
    
    func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didCloseWith closeCode: URLSessionWebSocketTask.CloseCode, reason: Data?) {
        let reasonText = reason.flatMap { String(data: $0, encoding: .utf8) } ?? "Connection closed"
        handleDisconnect(reason: reasonText)
    }
    
    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        if let error = error, !isManualDisconnect {
            handleDisconnect(reason: error.localizedDescription)
        }
    }
}
