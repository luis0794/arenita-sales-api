import Foundation

struct Conversation: Codable, Identifiable, Hashable {
    let id: Int64
    let waContactId: String
    let displayName: String?
    let lastMessageAt: Date
    let lastMessagePreview: String?
    let unreadCount: Int

    var title: String { displayName ?? "+\(waContactId)" }
}

struct Message: Codable, Identifiable, Hashable {
    let id: Int64
    let waMessageId: String
    let direction: String
    let type: String
    let body: String?
    let timestamp: Date

    var isInbound: Bool { direction == "INBOUND" }
}

struct Summary: Codable {
    let totalUnreadMessages: Int
    let unreadConversations: Int
    let topConversations: [Conversation]
}
