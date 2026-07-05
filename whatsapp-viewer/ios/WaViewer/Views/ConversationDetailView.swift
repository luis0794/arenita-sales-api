import SwiftUI

struct ConversationDetailView: View {
    let conversation: Conversation
    @State private var messages: [Message] = []
    @State private var error: String?
    @State private var confirmBlueTicks = false

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 8) {
                if let error {
                    Text(error).foregroundStyle(.red)
                }
                // El backend devuelve descendente; se muestra ascendente
                ForEach(messages.reversed()) { message in
                    MessageBubble(message: message)
                }
            }
            .padding()
        }
        .navigationTitle(conversation.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Marcar leído en WhatsApp", systemImage: "checkmark.circle") {
                    confirmBlueTicks = true
                }
            }
        }
        .confirmationDialog(
            "¿Enviar confirmación de lectura?",
            isPresented: $confirmBlueTicks,
            titleVisibility: .visible
        ) {
            Button("Sí, enviar palomitas azules", role: .destructive) {
                Task { try? await APIClient.shared.markReadUpstream(conversationId: conversation.id) }
            }
            Button("Cancelar", role: .cancel) {}
        } message: {
            Text("El remitente verá que leíste sus mensajes. Esta acción no se puede deshacer.")
        }
        .task {
            do {
                messages = try await APIClient.shared.messages(conversationId: conversation.id)
                // Visto en el visor: solo actualiza nuestra BD, WhatsApp no se entera
                try await APIClient.shared.markReadLocal(conversationId: conversation.id)
            } catch {
                self.error = error.localizedDescription
            }
        }
    }
}

struct MessageBubble: View {
    let message: Message

    var body: some View {
        HStack {
            if !message.isInbound { Spacer(minLength: 40) }
            VStack(alignment: .leading, spacing: 4) {
                Text(message.body ?? "[\(message.type)]")
                Text(message.timestamp, format: .dateTime.hour().minute())
                    .font(.caption2).foregroundStyle(.secondary)
            }
            .padding(10)
            .background(
                message.isInbound ? Color(.systemGray5) : Color.green.opacity(0.25),
                in: RoundedRectangle(cornerRadius: 12)
            )
            if message.isInbound { Spacer(minLength: 40) }
        }
    }
}
