package com.example.data.model

import java.util.UUID

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    AUTHENTICATING,
    CONNECTED,
    ERROR
}

enum class MessageType {
    GROUPCHAT,
    WHISPER,
    SYSTEM_JOIN,
    SYSTEM_LEAVE,
    SYSTEM_TOPIC,
    ACTION
}

enum class DeliveryStatus {
    SENDING,
    SENT,
    DELIVERED,
    FAILED
}

enum class OccupantRole {
    OWNER,
    ADMIN,
    MEMBER,
    VISITOR,
    NONE
}

enum class OccupantAffiliation {
    OWNER,
    ADMIN,
    MEMBER,
    OUTCAST,
    NONE
}

enum class UserPresence {
    ONLINE,
    CHAT,
    AWAY,
    DND,
    OFFLINE
}

enum class StanzaDirection {
    INCOMING,
    OUTGOING
}

data class XmppAccount(
    val jid: String = "guest_user@xmpp.chat.org",
    val username: String = "guest_user",
    val domain: String = "xmpp.chat.org",
    val nickname: String = "AndroidExplorer",
    val host: String = "xmpp.chat.org",
    val port: Int = 5222,
    val useTls: Boolean = true,
    val isDemoMode: Boolean = true,
    val statusMessage: String = "Active on XMPP MultiRoom"
)

data class XmppRoom(
    val jid: String,
    val name: String,
    val topic: String = "",
    val myNickname: String = "AndroidUser",
    val occupantCount: Int = 1,
    val unreadCount: Int = 0,
    val lastMessageText: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val isJoined: Boolean = true,
    val isBookmarked: Boolean = true,
    val myRole: OccupantRole = OccupantRole.MEMBER,
    val myAffiliation: OccupantAffiliation = OccupantAffiliation.MEMBER,
    val colorHex: String = "#0284C7"
)

data class XmppMessage(
    val id: String = UUID.randomUUID().toString(),
    val roomJid: String,
    val senderJid: String,
    val senderNickname: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isMine: Boolean = false,
    val messageType: MessageType = MessageType.GROUPCHAT,
    val status: DeliveryStatus = DeliveryStatus.SENT,
    val recipientNickname: String? = null // For 1-on-1 private whisper in MUC
)

data class RoomOccupant(
    val nickname: String,
    val jid: String = "",
    val role: OccupantRole = OccupantRole.MEMBER,
    val affiliation: OccupantAffiliation = OccupantAffiliation.MEMBER,
    val presence: UserPresence = UserPresence.ONLINE,
    val statusText: String = "",
    val isMe: Boolean = false
)

data class StanzaLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val direction: StanzaDirection,
    val rawXml: String,
    val stanzaType: String = "MESSAGE",
    val roomJid: String? = null
)

data class PublicDirectoryRoom(
    val jid: String,
    val name: String,
    val description: String,
    val occupantsCount: Int,
    val category: String = "General"
)
