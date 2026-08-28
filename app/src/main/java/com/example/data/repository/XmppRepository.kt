package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.RoomEntity
import com.example.data.model.ConnectionStatus
import com.example.data.model.DeliveryStatus
import com.example.data.model.MessageType
import com.example.data.model.OccupantAffiliation
import com.example.data.model.OccupantRole
import com.example.data.model.PublicDirectoryRoom
import com.example.data.model.RoomOccupant
import com.example.data.model.StanzaLog
import com.example.data.model.XmppAccount
import com.example.data.model.XmppMessage
import com.example.data.model.XmppRoom
import com.example.data.network.XmppProtocolEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class XmppRepository(
    private val database: AppDatabase,
    private val scope: CoroutineScope
) {
    val engine = XmppProtocolEngine(scope)

    val connectionStatus: StateFlow<ConnectionStatus> = engine.connectionStatus
    val currentAccount: StateFlow<XmppAccount> = engine.currentAccount
    val stanzaLogs: StateFlow<List<StanzaLog>> = engine.stanzaLogs
    val roomOccupants: StateFlow<Map<String, List<RoomOccupant>>> = engine.roomOccupants

    val rooms: Flow<List<XmppRoom>> = database.roomDao().getAllRooms().map { entities ->
        entities.map { it.toModel() }
    }

    init {
        // Collect incoming network messages and store into Room database
        scope.launch {
            try {
                engine.incomingMessages.collect { msg ->
                    try {
                        database.messageDao().insertMessage(msg.toEntity())
                        
                        // Update room last message and unread count
                        val existing = database.roomDao().getRoomByJid(msg.roomJid)
                        if (existing != null) {
                            val updated = existing.copy(
                                lastMessageText = if (msg.messageType == MessageType.WHISPER) "[Bisikan] ${msg.body}" else msg.body,
                                lastMessageTime = msg.timestamp,
                                unreadCount = if (msg.isMine) existing.unreadCount else existing.unreadCount + 1
                            )
                            database.roomDao().insertOrUpdateRoom(updated)
                        }
                    } catch (e: Exception) {
                        // Handled safely
                    }
                }
            } catch (e: Exception) {
                // Handled safely
            }
        }

        // Initialize default rooms if empty
        scope.launch {
            try {
                seedDefaultRoomsIfEmpty()
            } catch (e: Exception) {
                // Handled safely
            }
        }
    }

    private suspend fun seedDefaultRoomsIfEmpty() {
        val count = database.roomDao().getRoomByJid("general@conference.xmpp.today")
        if (count == null) {
            val defaultRooms = listOf(
                RoomEntity(
                    jid = "general@conference.xmpp.today",
                    name = "# Umum (General)",
                    topic = "Ruang obrolan umum multi-user XMPP",
                    myNickname = "AndroidUser",
                    occupantCount = 5,
                    unreadCount = 0,
                    lastMessageText = "Selamat datang di XMPP MultiRoom!",
                    lastMessageTime = System.currentTimeMillis() - 60000,
                    isJoined = true,
                    isBookmarked = true,
                    myRole = OccupantRole.MEMBER,
                    myAffiliation = OccupantAffiliation.MEMBER,
                    colorHex = "#0284C7"
                ),
                RoomEntity(
                    jid = "tech-id@conference.xmpp.today",
                    name = "# Tech Indonesia",
                    topic = "Diskusi teknologi, cloud, server Jabber & modern computing",
                    myNickname = "AndroidUser",
                    occupantCount = 4,
                    unreadCount = 1,
                    lastMessageText = "Prosody 0.12 siap digunakan",
                    lastMessageTime = System.currentTimeMillis() - 120000,
                    isJoined = true,
                    isBookmarked = true,
                    myRole = OccupantRole.MEMBER,
                    myAffiliation = OccupantAffiliation.MEMBER,
                    colorHex = "#0D9488"
                ),
                RoomEntity(
                    jid = "android-kotlin@conference.xmpp.today",
                    name = "# Android & Kotlin",
                    topic = "Seputar Jetpack Compose, Coroutines, dan M3 Design",
                    myNickname = "AndroidUser",
                    occupantCount = 4,
                    unreadCount = 0,
                    lastMessageText = "Room database terintegrasi sempurna",
                    lastMessageTime = System.currentTimeMillis() - 300000,
                    isJoined = true,
                    isBookmarked = true,
                    myRole = OccupantRole.MEMBER,
                    myAffiliation = OccupantAffiliation.MEMBER,
                    colorHex = "#7C3AED"
                ),
                RoomEntity(
                    jid = "random-lounge@conference.xmpp.today",
                    name = "# Santai & Kopi",
                    topic = "Lounge santai, games & ngobrol bebas",
                    myNickname = "AndroidUser",
                    occupantCount = 3,
                    unreadCount = 0,
                    lastMessageText = "Selamat sore semuanya!",
                    lastMessageTime = System.currentTimeMillis() - 500000,
                    isJoined = true,
                    isBookmarked = true,
                    myRole = OccupantRole.MEMBER,
                    myAffiliation = OccupantAffiliation.MEMBER,
                    colorHex = "#D97706"
                )
            )
            database.roomDao().insertRooms(defaultRooms)

            // Seed initial welcome messages
            val welcomeMessages = listOf(
                MessageEntity(
                    id = "init_1",
                    roomJid = "general@conference.xmpp.today",
                    senderJid = "general@conference.xmpp.today/XmppAdmin",
                    senderNickname = "XmppAdmin",
                    body = "Selamat datang di XMPP Multi-Room Chat! Ruang ini mendukung obrolan multi-kanal terdesentralisasi, live stanza XML, dan roster anggota.",
                    timestamp = System.currentTimeMillis() - 100000,
                    isMine = false,
                    messageType = MessageType.GROUPCHAT,
                    status = DeliveryStatus.DELIVERED
                ),
                MessageEntity(
                    id = "init_2",
                    roomJid = "tech-id@conference.xmpp.today",
                    senderJid = "tech-id@conference.xmpp.today/ArchGuru",
                    senderNickname = "ArchGuru",
                    body = "Protokol XEP-0045 memungkinkan pembuatan kamar obrolan publik dan privat dengan pengaturan role moderasi.",
                    timestamp = System.currentTimeMillis() - 120000,
                    isMine = false,
                    messageType = MessageType.GROUPCHAT,
                    status = DeliveryStatus.DELIVERED
                ),
                MessageEntity(
                    id = "init_3",
                    roomJid = "android-kotlin@conference.xmpp.today",
                    senderJid = "android-kotlin@conference.xmpp.today/KotlinMaster",
                    senderNickname = "KotlinMaster",
                    body = "Halo teman-teman! Aplikasi ini dibangun dengan Kotlin & Jetpack Compose modern.",
                    timestamp = System.currentTimeMillis() - 300000,
                    isMine = false,
                    messageType = MessageType.GROUPCHAT,
                    status = DeliveryStatus.DELIVERED
                )
            )
            database.messageDao().insertMessages(welcomeMessages)
        }
    }

    fun getMessagesForRoom(roomJid: String): Flow<List<XmppMessage>> {
        return database.messageDao().getMessagesForRoom(roomJid).map { list ->
            list.map { it.toModel() }
        }
    }

    suspend fun markRoomAsRead(roomJid: String) {
        database.roomDao().markRoomAsRead(roomJid)
    }

    suspend fun connect(account: XmppAccount, password: String = "") {
        database.accountDao().saveAccount(account.toEntity())
        engine.connect(account, password)
    }

    suspend fun disconnect() {
        engine.disconnect()
    }

    suspend fun joinRoom(roomJid: String, name: String, nickname: String, topic: String = "", password: String = "") {
        val existing = database.roomDao().getRoomByJid(roomJid)
        val roomEntity = existing?.copy(
            isJoined = true,
            myNickname = nickname,
            topic = if (topic.isNotBlank()) topic else existing.topic
        ) ?: RoomEntity(
            jid = roomJid,
            name = if (name.isNotBlank()) name else "# " + roomJid.substringBefore("@"),
            topic = topic,
            myNickname = nickname,
            occupantCount = 1,
            unreadCount = 0,
            lastMessageText = "Baru saja bergabung",
            lastMessageTime = System.currentTimeMillis(),
            isJoined = true,
            isBookmarked = true,
            myRole = OccupantRole.MEMBER,
            myAffiliation = OccupantAffiliation.MEMBER,
            colorHex = randomColorForRoom(roomJid)
        )
        database.roomDao().insertOrUpdateRoom(roomEntity)
        engine.joinRoom(roomJid, nickname, password)
    }

    suspend fun leaveRoom(roomJid: String, nickname: String) {
        val existing = database.roomDao().getRoomByJid(roomJid)
        if (existing != null) {
            database.roomDao().insertOrUpdateRoom(existing.copy(isJoined = false))
        }
        engine.leaveRoom(roomJid, nickname)
    }

    suspend fun deleteRoom(roomJid: String) {
        database.roomDao().deleteRoom(roomJid)
        database.messageDao().clearRoomMessages(roomJid)
    }

    suspend fun sendMessage(roomJid: String, text: String, myNickname: String) {
        val msg = engine.sendGroupMessage(roomJid, text, myNickname)
        database.messageDao().insertMessage(msg.toEntity())
        
        val existing = database.roomDao().getRoomByJid(roomJid)
        if (existing != null) {
            database.roomDao().insertOrUpdateRoom(
                existing.copy(
                    lastMessageText = text,
                    lastMessageTime = msg.timestamp
                )
            )
        }
    }

    suspend fun sendWhisper(roomJid: String, targetNickname: String, text: String, myNickname: String) {
        val msg = engine.sendPrivateWhisper(roomJid, targetNickname, text, myNickname)
        database.messageDao().insertMessage(msg.toEntity())
    }

    suspend fun updateRoomTopic(roomJid: String, newTopic: String, myNickname: String) {
        database.roomDao().updateTopic(roomJid, newTopic)
        engine.changeRoomTopic(roomJid, newTopic, myNickname)
    }

    suspend fun queryDirectory(): List<PublicDirectoryRoom> {
        return engine.queryPublicDirectory()
    }

    fun sendRawStanza(xml: String) {
        engine.sendRawStanza(xml)
    }

    private fun randomColorForRoom(roomJid: String): String {
        val colors = listOf("#0284C7", "#0D9488", "#7C3AED", "#D97706", "#DC2626", "#2563EB", "#059669")
        val index = kotlin.math.abs(roomJid.hashCode()) % colors.size
        return colors[index]
    }
}

private fun AccountEntity.toModel(): XmppAccount = XmppAccount(
    jid = jid,
    username = username,
    domain = domain,
    nickname = nickname,
    host = host,
    port = port,
    useTls = useTls,
    isDemoMode = isDemoMode,
    statusMessage = statusMessage
)

private fun XmppAccount.toEntity(): AccountEntity = AccountEntity(
    jid = jid,
    username = username,
    domain = domain,
    nickname = nickname,
    host = host,
    port = port,
    useTls = useTls,
    isDemoMode = isDemoMode,
    statusMessage = statusMessage
)

private fun RoomEntity.toModel(): XmppRoom = XmppRoom(
    jid = jid,
    name = name,
    topic = topic,
    myNickname = myNickname,
    occupantCount = occupantCount,
    unreadCount = unreadCount,
    lastMessageText = lastMessageText,
    lastMessageTime = lastMessageTime,
    isJoined = isJoined,
    isBookmarked = isBookmarked,
    myRole = myRole,
    myAffiliation = myAffiliation,
    colorHex = colorHex
)

private fun MessageEntity.toModel(): XmppMessage = XmppMessage(
    id = id,
    roomJid = roomJid,
    senderJid = senderJid,
    senderNickname = senderNickname,
    body = body,
    timestamp = timestamp,
    isMine = isMine,
    messageType = messageType,
    status = status,
    recipientNickname = recipientNickname
)

private fun XmppMessage.toEntity(): MessageEntity = MessageEntity(
    id = id,
    roomJid = roomJid,
    senderJid = senderJid,
    senderNickname = senderNickname,
    body = body,
    timestamp = timestamp,
    isMine = isMine,
    messageType = messageType,
    status = status,
    recipientNickname = recipientNickname
)
