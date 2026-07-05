import WidgetKit
import SwiftUI

/// Widget de pantalla de inicio: contador de mensajes sin leer y últimos remitentes.
/// Consulta /api/v1/summary del backend usando la configuración compartida por App Group.
@main
struct WaViewerWidgetBundle: WidgetBundle {
    var body: some Widget {
        WaViewerWidget()
    }
}

struct SummaryEntry: TimelineEntry {
    let date: Date
    let totalUnread: Int
    let conversations: [TopConversation]
    let error: Bool

    struct TopConversation: Identifiable {
        let id: Int64
        let title: String
        let preview: String
        let unread: Int
    }

    static let placeholder = SummaryEntry(
        date: .now,
        totalUnread: 3,
        conversations: [
            .init(id: 1, title: "Juan Pérez", preview: "¿Me confirmas la cotización?", unread: 2),
            .init(id: 2, title: "María López", preview: "Gracias!", unread: 1),
        ],
        error: false
    )
}

struct SummaryProvider: TimelineProvider {
    func placeholder(in context: Context) -> SummaryEntry { .placeholder }

    func getSnapshot(in context: Context, completion: @escaping (SummaryEntry) -> Void) {
        if context.isPreview { return completion(.placeholder) }
        Task { completion(await fetch()) }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SummaryEntry>) -> Void) {
        Task {
            let entry = await fetch()
            // Refresco cada 15 min; la app también fuerza recarga al sincronizar
            let next = Calendar.current.date(byAdding: .minute, value: 15, to: .now)!
            completion(Timeline(entries: [entry], policy: .after(next)))
        }
    }

    private func fetch() async -> SummaryEntry {
        do {
            let summary = try await APIClient.shared.summary()
            return SummaryEntry(
                date: .now,
                totalUnread: summary.totalUnreadMessages,
                conversations: summary.topConversations.map {
                    .init(id: $0.id, title: $0.title, preview: $0.lastMessagePreview ?? "", unread: $0.unreadCount)
                },
                error: false
            )
        } catch {
            return SummaryEntry(date: .now, totalUnread: 0, conversations: [], error: true)
        }
    }
}

struct WaViewerWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "WaViewerWidget", provider: SummaryProvider()) { entry in
            WidgetView(entry: entry)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Mensajes sin leer")
        .description("Tus mensajes de WhatsApp Business sin marcar como leídos.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct WidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: SummaryEntry

    var body: some View {
        if entry.error {
            Label("Sin conexión", systemImage: "wifi.slash").font(.caption)
        } else if family == .systemSmall {
            VStack(spacing: 4) {
                Text("\(entry.totalUnread)")
                    .font(.system(size: 40, weight: .bold))
                Text("sin leer")
                    .font(.caption).foregroundStyle(.secondary)
            }
        } else {
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Image(systemName: "bubble.left.and.bubble.right.fill").foregroundStyle(.green)
                    Text("\(entry.totalUnread) sin leer").font(.headline)
                }
                ForEach(entry.conversations.prefix(3)) { convo in
                    HStack {
                        Text(convo.title).font(.caption.bold()).lineLimit(1)
                        Text(convo.preview).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                        Spacer()
                        Text("\(convo.unread)").font(.caption2.bold()).foregroundStyle(.green)
                    }
                }
                if entry.conversations.isEmpty {
                    Text("Todo al día ✅").font(.caption).foregroundStyle(.secondary)
                }
            }
        }
    }
}
