package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionStatus
import com.example.data.model.XmppAccount
import com.example.ui.theme.AwayAmber
import com.example.ui.theme.DndRed
import com.example.ui.theme.OnlineGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountConnectionScreen(
    currentAccount: XmppAccount,
    connectionStatus: ConnectionStatus,
    onConnect: (XmppAccount, String) -> Unit,
    onDisconnect: () -> Unit
) {
    var jidInput by remember(currentAccount) { mutableStateOf(currentAccount.jid) }
    var passwordInput by remember { mutableStateOf("") }
    var nicknameInput by remember(currentAccount) { mutableStateOf(currentAccount.nickname) }
    var hostInput by remember(currentAccount) { mutableStateOf(currentAccount.host) }
    var portInput by remember(currentAccount) { mutableStateOf(currentAccount.port.toString()) }
    var useTls by remember(currentAccount) { mutableStateOf(currentAccount.useTls) }
    var isDemoMode by remember(currentAccount) { mutableStateOf(currentAccount.isDemoMode) }
    var statusMsgInput by remember(currentAccount) { mutableStateOf(currentAccount.statusMessage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pengaturan Akun & Server XMPP")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> OnlineGreen.copy(alpha = 0.12f)
                        ConnectionStatus.CONNECTING, ConnectionStatus.AUTHENTICATING -> AwayAmber.copy(alpha = 0.12f)
                        ConnectionStatus.ERROR -> DndRed.copy(alpha = 0.12f)
                        ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> OnlineGreen
                        ConnectionStatus.CONNECTING, ConnectionStatus.AUTHENTICATING -> AwayAmber
                        ConnectionStatus.ERROR -> DndRed
                        ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = OnlineGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        ConnectionStatus.CONNECTING, ConnectionStatus.AUTHENTICATING -> CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = AwayAmber,
                            strokeWidth = 3.dp
                        )
                        ConnectionStatus.ERROR -> Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = DndRed,
                            modifier = Modifier.size(32.dp)
                        )
                        ConnectionStatus.DISCONNECTED -> Icon(
                            imageVector = Icons.Default.LinkOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (connectionStatus) {
                                ConnectionStatus.CONNECTED -> "Terhubung (Online)"
                                ConnectionStatus.CONNECTING -> "Menghubungkan ke Stream..."
                                ConnectionStatus.AUTHENTICATING -> "Mengotentikasi SASL..."
                                ConnectionStatus.ERROR -> "Koneksi Bermasalah"
                                ConnectionStatus.DISCONNECTED -> "Terputus (Offline)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (connectionStatus == ConnectionStatus.CONNECTED)
                                "JID Aktif: ${currentAccount.jid} | Host: ${currentAccount.host}"
                            else
                                "Klik tombol 'Sambungkan' untuk membuka sesi XMPP stream",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Presets
            Text(
                text = "Preset Server XMPP Publik Cepat:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = jidInput.endsWith("@xmpp.chat.org"),
                    onClick = {
                        jidInput = "guest_${(100..999).random()}@xmpp.chat.org"
                        hostInput = "xmpp.chat.org"
                        portInput = "5222"
                        isDemoMode = true
                    },
                    label = { Text("Sandbox Server", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = jidInput.endsWith("@conversations.im"),
                    onClick = {
                        jidInput = "user@conversations.im"
                        hostInput = "conversations.im"
                        portInput = "5222"
                        isDemoMode = false
                    },
                    label = { Text("conversations.im", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = jidInput.endsWith("@jabber.at"),
                    onClick = {
                        jidInput = "user@jabber.at"
                        hostInput = "jabber.at"
                        portInput = "5222"
                        isDemoMode = false
                    },
                    label = { Text("jabber.at", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Form inputs
            OutlinedTextField(
                value = jidInput,
                onValueChange = {
                    jidInput = it
                    if (it.contains("@") && hostInput == "xmpp.chat.org") {
                        hostInput = it.substringAfter("@")
                    }
                },
                label = { Text("Jabber ID (JID)") },
                placeholder = { Text("username@domain.org") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_account_jid"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Kata Sandi Akun") },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_account_password"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = nicknameInput,
                onValueChange = { nicknameInput = it },
                label = { Text("Nama Panggilan MUC (Nickname)") },
                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it },
                    label = { Text("Server Host") },
                    leadingIcon = { Icon(Icons.Default.Lan, contentDescription = null) },
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = portInput,
                    onValueChange = { portInput = it },
                    label = { Text("Port") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = statusMsgInput,
                onValueChange = { statusMsgInput = it },
                label = { Text("Pesan Status Presence") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Toggles
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enkripsi TLS/SSL (StartTLS)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Amankan transmisi data ke port 5222 / 5223", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = useTls, onCheckedChange = { useTls = it })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mode Multi-Room Sandbox Live", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Aktifkan simulasi multi-bot cerdas & multi-room responsif untuk pengujian kapan saja", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isDemoMode, onCheckedChange = { isDemoMode = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (connectionStatus == ConnectionStatus.CONNECTED) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_disconnect"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Putuskan Koneksi")
                    }
                }

                Button(
                    onClick = {
                        val parsedPort = portInput.toIntOrNull() ?: 5222
                        val domain = if (jidInput.contains("@")) jidInput.substringAfter("@") else hostInput
                        val username = if (jidInput.contains("@")) jidInput.substringBefore("@") else jidInput
                        val updatedAccount = XmppAccount(
                            jid = jidInput.trim(),
                            username = username.trim(),
                            domain = domain.trim(),
                            nickname = nicknameInput.trim().ifBlank { username },
                            host = hostInput.trim().ifBlank { domain },
                            port = parsedPort,
                            useTls = useTls,
                            isDemoMode = isDemoMode,
                            statusMessage = statusMsgInput.trim()
                        )
                        onConnect(updatedAccount, passwordInput)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_connect"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (connectionStatus == ConnectionStatus.CONNECTED) "Perbarui & Reconnect" else "Sambungkan")
                }
            }
        }
    }
}
