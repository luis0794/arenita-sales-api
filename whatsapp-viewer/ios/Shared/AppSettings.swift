import Foundation

/// Configuración compartida entre la app y el widget vía App Group.
enum AppSettings {
    static let appGroupId = "group.com.waviewer.shared"

    static var defaults: UserDefaults {
        UserDefaults(suiteName: appGroupId) ?? .standard
    }

    static var baseURL: URL? {
        guard let raw = defaults.string(forKey: "backendUrl"), !raw.isEmpty else { return nil }
        return URL(string: raw)
    }

    static var apiKey: String {
        defaults.string(forKey: "apiKey") ?? ""
    }
}
