import SwiftUI

@main
struct WaViewerApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: View {
    @AppStorage("configured", store: AppSettings.defaults) private var configured = false

    var body: some View {
        NavigationStack {
            if configured {
                ConversationListView()
            } else {
                SettingsView(isOnboarding: true)
            }
        }
    }
}
