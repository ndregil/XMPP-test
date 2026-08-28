package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.RoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY lastActiveTime DESC LIMIT 1")
    fun getActiveAccount(): Flow<AccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAccount(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()
}

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms ORDER BY lastMessageTime DESC")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE jid = :roomJid")
    suspend fun getRoomByJid(roomJid: String): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRoom(room: RoomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<RoomEntity>)

    @Query("UPDATE rooms SET unreadCount = 0 WHERE jid = :roomJid")
    suspend fun markRoomAsRead(roomJid: String)

    @Query("UPDATE rooms SET topic = :newTopic WHERE jid = :roomJid")
    suspend fun updateTopic(roomJid: String, newTopic: String)

    @Query("DELETE FROM rooms WHERE jid = :roomJid")
    suspend fun deleteRoom(roomJid: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE roomJid = :roomJid ORDER BY timestamp ASC")
    fun getMessagesForRoom(roomJid: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE roomJid = :roomJid")
    suspend fun clearRoomMessages(roomJid: String)
}
