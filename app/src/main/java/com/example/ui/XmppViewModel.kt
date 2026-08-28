package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ConnectionStatus
import com.example.data.model.PublicDirectoryRoom
import com.example.data.model.RoomOccupant
import com.example.data.model.StanzaLog
import com.example.data.model.XmppAccount
import com.example.data.model.XmppMessage
import com.example.data.model.XmppRoom
import com.example.data.repository.XmppRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class XmppViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = XmppRepository(database, viewModelScope)

    val connectionStatus: StateFlow<ConnectionStatus> = repository.connectionStatus
    val currentAccount: StateFlow<XmppAccount> = repository.currentAccount
    val stanzaLogs: StateFlow<List<StanzaLog>> = repository.stanzaLogs
    val allRoomOccupants: StateFlow<Map<String, List<RoomOccupant>>> = repository.roomOccupants

    // Search filter query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Raw rooms list
    val allRooms: StateFlow<List<XmppRoom>> = repository.rooms.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered rooms
    val filteredRooms: StateFlow<List<XmppRoom>> = combine(allRooms, _searchQuery) { rooms, query ->
        if (query.isBlank()) {
            rooms
        } else {
            rooms.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.jid.contains(query, ignoreCase = true) ||
                it.topic.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Currently selected / active room JID
    private val _selectedRoomJid = MutableStateFlow<String?>("general@conference.xmpp.today")
    val selectedRoomJid: StateFlow<String?> = _selectedRoomJid.asStateFlow()

    // Active room object
    val activeRoom: StateFlow<XmppRoom?> = combine(allRooms, _selectedRoomJid) { rooms, jid ->
        if (jid != null) {
            rooms.find { it.jid == jid } ?: rooms.firstOrNull()
        } else {
            rooms.firstOrNull()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Messages for active room
    val activeRoomMessages: StateFlow<List<XmppMessage>> = _selectedRoomJid.flatMapLatest { jid ->
        if (jid != null) {
            repository.getMessagesForRoom(jid)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Occupants for active room
    val activeRoomOccupants: StateFlow<List<RoomOccupant>> = combine(allRoomOccupants, _selectedRoomJid) { map, jid ->
        if (jid != null) map[jid] ?: emptyList() else emptyList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Public Directory Rooms
    private val _directoryRooms = MutableStateFlow<List<PublicDirectoryRoom>>(emptyList())
    val directoryRooms: StateFlow<List<PublicDirectoryRoom>> = _directoryRooms.asStateFlow()

    private val _isDirectoryLoading = MutableStateFlow(false)
    val isDirectoryLoading: StateFlow<Boolean> = _isDirectoryLoading.asStateFlow()

    // UI state flags
    val isRosterSheetOpen = MutableStateFlow(false)
    val isTopicModalOpen = MutableStateFlow(false)
    val isJoinCreateDialogOpen = MutableStateFlow(false)
    val isStanzaConsoleOpen = MutableStateFlow(false)
    val whisperTargetOccupant = MutableStateFlow<RoomOccupant?>(null)

    // Active input text
    val currentInputText = MutableStateFlow("")

    init {
        // Auto connect default account to make the app immediately interactive
        viewModelScope.launch {
            repository.connect(XmppAccount())
            loadDirectory()
        }
    }

    fun selectRoom(jid: String) {
        _selectedRoomJid.value = jid
        viewModelScope.launch {
            repository.markRoomAsRead(jid)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        val room = activeRoom.value ?: return
        if (trimmed.isBlank()) return

        val myNick = room.myNickname.ifBlank { currentAccount.value.nickname }

        viewModelScope.launch {
            val whisperTarget = whisperTargetOccupant.value
            if (whisperTarget != null) {
                repository.sendWhisper(room.jid, whisperTarget.nickname, trimmed, myNick)
                whisperTargetOccupant.value = null
            } else {
                repository.sendMessage(room.jid, trimmed, myNick)
            }
            currentInputText.value = ""
        }
    }

    fun joinOrCreateRoom(
        roomJid: String,
        roomName: String,
        nickname: String,
        topic: String = "",
        password: String = ""
    ) {
        viewModelScope.launch {
            val validJid = if (roomJid.contains("@")) roomJid else "$roomJid@conference.xmpp.today"
            val validNick = if (nickname.isNotBlank()) nickname else currentAccount.value.nickname
            repository.joinRoom(validJid, roomName, validNick, topic, password)
            _selectedRoomJid.value = validJid
            isJoinCreateDialogOpen.value = false
        }
    }

    fun leaveRoom(roomJid: String) {
        viewModelScope.launch {
            val room = allRooms.value.find { it.jid == roomJid }
            val nick = room?.myNickname ?: currentAccount.value.nickname
            repository.leaveRoom(roomJid, nick)
            if (_selectedRoomJid.value == roomJid) {
                val other = allRooms.value.firstOrNull { it.jid != roomJid && it.isJoined }
                _selectedRoomJid.value = other?.jid
            }
        }
    }

    fun deleteRoom(roomJid: String) {
        viewModelScope.launch {
            repository.deleteRoom(roomJid)
            if (_selectedRoomJid.value == roomJid) {
                _selectedRoomJid.value = allRooms.value.firstOrNull { it.jid != roomJid }?.jid
            }
        }
    }

    fun updateTopic(newTopic: String) {
        val room = activeRoom.value ?: return
        viewModelScope.launch {
            val myNick = room.myNickname.ifBlank { currentAccount.value.nickname }
            repository.updateRoomTopic(room.jid, newTopic, myNick)
            isTopicModalOpen.value = false
        }
    }

    fun loadDirectory() {
        viewModelScope.launch {
            _isDirectoryLoading.value = true
            try {
                _directoryRooms.value = repository.queryDirectory()
            } finally {
                _isDirectoryLoading.value = false
            }
        }
    }

    fun connectAccount(account: XmppAccount, password: String = "") {
        viewModelScope.launch {
            repository.connect(account, password)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            repository.disconnect()
        }
    }

    fun sendRawXmlStanza(xml: String) {
        repository.sendRawStanza(xml)
    }

    fun setWhisperTarget(occupant: RoomOccupant?) {
        whisperTargetOccupant.value = occupant
        isRosterSheetOpen.value = false
    }
}
