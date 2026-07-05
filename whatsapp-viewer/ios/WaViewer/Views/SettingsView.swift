import SwiftUI

struct SettingsView: View {
    let isOnboarding: Bool

    @AppStorage("backendUrl", store: AppSettings.defaults) private var backendUrl = ""
    @AppStorage("apiKey", store: AppSettings.defaults) private var apiKey = ""
    @AppStorage("configured", store: AppSettings.defaults) private var configured = false
    @State private var testResult: String?

    var body: some View {
        Form {
            Section("Backend") {
                TextField("https://waviewer.tudominio.com", text: $backendUrl)
                    .keyboardType(.URL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                SecureField("API key", text: $apiKey)
            }
            Section {
                Button("Probar conexión") { Task { await testConnection() } }
                if let testResult { Text(testResult) }
            }
            if isOnboarding {
                Section {
                    Button("Comenzar") { configured = true }
                        .disabled(backendUrl.isEmpty || apiKey.isEmpty)
                }
            }
            Section {
                Text("Los mensajes se leen desde tu propio backend (webhooks de la Cloud API de Meta). Verlos aquí nunca envía confirmaciones de lectura a WhatsApp.")
                    .font(.footnote).foregroundStyle(.secondary)
            }
        }
        .navigationTitle("Ajustes")
    }

    private func testConnection() async {
        do {
            let summary = try await APIClient.shared.summary()
            testResult = "✅ Conectado: \(summary.totalUnreadMessages) mensajes sin leer"
        } catch {
            testResult = "❌ \(error.localizedDescription)"
        }
    }
}
