import SwiftUI
import WidgetKit

struct ConversationListView: View {
    @State private var conversations: [Conversation] = []
    @State private var onlyUnread = true
    @State private var error: String?
    @State private var loading = false

    var body: some View {
        List {
            if let error {
                Text(error).foregroundStyle(.red)
            }
            Section {
                ForEach(conversations) { convo in
                    NavigationLink(value: convo) {
                        ConversationRow(conversation: convo)
                    }
                }
            } footer: {
                Text("Leer aquí NO marca los mensajes como leídos en WhatsApp: el remitente no verá las palomitas azules.")
            }
        }
        .navigationTitle("Sin leer")
        .navigationDestination(for: Conversation.self) { convo in
            ConversationDetailView(conversation: convo)
        }
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                NavigationLink("Ajustes") { SettingsView(isOnboarding: false) }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Toggle("Solo sin leer", isOn: $onlyUnread)
                    .toggleStyle(.button)
            }
        }
        .overlay {
            if conversations.isEmpty && !loading && error == nil {
                ContentUnavailableView(
                    "Todo al día",
                    systemImage: "checkmark.bubble",
                    description: Text("No tienes mensajes sin leer.")
                )
            }
        }
        .refreshable { await load() }
        .task(id: onlyUnread) { await load() }
    }

    private func load() async {
        loading = true
        defer { loading = false }
        do {
            conversations = try await APIClient.shared.conversations(onlyUnread: onlyUnread)
            error = nil
            WidgetCenter.shared.reloadAllTimelines()
        } catch {
            self.error = error.localizedDescription
        }
    }
}

struct ConversationRow: View {
    let conversation: Conversation

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(conversation.title).font(.headline)
                if let preview = conversation.lastMessagePreview {
                    Text(preview).font(.subheadline).foregroundStyle(.secondary).lineLimit(2)
                }
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text(conversation.lastMessageAt, style: .time)
                    .font(.caption).foregroundStyle(.secondary)
                if conversation.unreadCount > 0 {
                    Text("\(conversation.unreadCount)")
                        .font(.caption.bold())
                        .padding(.horizontal, 8).padding(.vertical, 3)
                        .background(.green, in: Capsule())
                        .foregroundStyle(.white)
                }
            }
        }
    }
}
