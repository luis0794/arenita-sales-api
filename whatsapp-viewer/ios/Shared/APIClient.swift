import Foundation

enum APIError: LocalizedError {
    case notConfigured
    case badStatus(Int)

    var errorDescription: String? {
        switch self {
        case .notConfigured: return "Configura la URL del backend y la API key en Ajustes."
        case .badStatus(let code): return "El servidor respondió \(code)."
        }
    }
}

struct APIClient {
    static let shared = APIClient()

    private let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        // El backend (Jackson) serializa Instant como ISO-8601, con o sin fracción de segundo
        let withFraction = ISO8601DateFormatter()
        withFraction.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let plain = ISO8601DateFormatter()
        decoder.dateDecodingStrategy = .custom { d in
            let raw = try d.singleValueContainer().decode(String.self)
            if let date = withFraction.date(from: raw) ?? plain.date(from: raw) { return date }
            throw DecodingError.dataCorrupted(.init(
                codingPath: d.codingPath,
                debugDescription: "Fecha inválida: \(raw)"
            ))
        }
        return decoder
    }()

    func conversations(onlyUnread: Bool) async throws -> [Conversation] {
        try await get("api/v1/conversations?onlyUnread=\(onlyUnread)")
    }

    func messages(conversationId: Int64, limit: Int = 50) async throws -> [Message] {
        try await get("api/v1/conversations/\(conversationId)/messages?limit=\(limit)")
    }

    func summary() async throws -> Summary {
        try await get("api/v1/summary")
    }

    /// Visto en el visor: nunca envía confirmación de lectura a WhatsApp.
    func markReadLocal(conversationId: Int64) async throws {
        try await post("api/v1/conversations/\(conversationId)/read-local")
    }

    /// Envía las palomitas azules reales. Solo bajo acción explícita del usuario.
    func markReadUpstream(conversationId: Int64) async throws {
        try await post("api/v1/conversations/\(conversationId)/read-upstream")
    }

    // MARK: - Internos

    private func request(_ path: String, method: String) throws -> URLRequest {
        guard let base = AppSettings.baseURL, !AppSettings.apiKey.isEmpty else {
            throw APIError.notConfigured
        }
        var request = URLRequest(url: base.appending(path: path))
        request.httpMethod = method
        request.setValue(AppSettings.apiKey, forHTTPHeaderField: "X-Api-Key")
        return request
    }

    private func get<T: Decodable>(_ path: String) async throws -> T {
        let (data, response) = try await URLSession.shared.data(for: request(path, method: "GET"))
        try validate(response)
        return try decoder.decode(T.self, from: data)
    }

    private func post(_ path: String) async throws {
        let (_, response) = try await URLSession.shared.data(for: request(path, method: "POST"))
        try validate(response)
    }

    private func validate(_ response: URLResponse) throws {
        guard let http = response as? HTTPURLResponse else { return }
        guard (200..<300).contains(http.statusCode) else { throw APIError.badStatus(http.statusCode) }
    }
}
