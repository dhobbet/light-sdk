package com.thelightphone.sdk.server.backup

import com.thelightphone.backup.RemoteBackupProvider
import com.thelightphone.sdk.server.toolmanager.FormDataTree
import com.thelightphone.toolmanager.FormItem
import com.thelightphone.toolmanager.FormSpec
import com.thelightphone.toolmanager.LeafView

private const val FIELD_SERVER_URL = "server_url"
private const val FIELD_USERNAME = "username"
private const val FIELD_PASSWORD = "password"
private const val FIELD_ALLOW_HTTP = "allow_http"

// tool manager leaf for linking a WebDAV server (Nextcloud, Synology, rclone serve webdav, etc.)
// as the backup provider. Credentials aren't checked against the server here, since the device
// may not be on a network that can reach it yet - a bad login surfaces on the first backup run.
fun webDavBackupLeafView(
    credentialStorage: WebDavCredentialStorage,
    formSpec: FormSpec = FormSpec(
        "WebDAV Backup",
        "webdav",
        items = listOf(
            FormItem(FIELD_SERVER_URL, "Server URL", "https://example.com/remote.php/dav/files/me", required = true),
            FormItem(FIELD_USERNAME, "Username", required = true),
            FormItem(FIELD_PASSWORD, "Password", hidden = true, required = true),
            FormItem(FIELD_ALLOW_HTTP, "Allow plain HTTP? (yes/no)", "no"),
        ),
        headerText = "Back up to your own WebDAV server. Most servers expect an app-specific password " +
                "rather than your account password.\n\nPlain http:// URLs send your password unencrypted " +
                "and are refused unless you answer \"yes\" to allowing plain HTTP.",
        buttonText = "Link Server"
    ),
    onLinked: suspend (accountType: RemoteBackupProvider) -> Unit = {},
): LeafView = LeafView(formSpec, object : FormDataTree() {
    override suspend fun onSubmit(responses: Map<String, String>): Result<Unit> = runCatching {
        val credentials = StoredWebDavCredentials(
            serverUrl = responses[FIELD_SERVER_URL].orEmpty().trim(),
            username = responses[FIELD_USERNAME].orEmpty().trim(),
            password = responses[FIELD_PASSWORD].orEmpty(),
            allowInsecureHttp = responses[FIELD_ALLOW_HTTP].orEmpty().trim().lowercase() in setOf("yes", "y", "true"),
        )
        require(credentials.username.isNotEmpty() && credentials.password.isNotEmpty()) {
            "username and password are required"
        }

        // Offline sanity check only, mirroring what WebDavRemoteBackup's constructor enforces - an
        // URL it would reject means BackupWorker silently can't build a backup at all.
        val scheme = credentials.serverUrl.substringBefore("://", missingDelimiterValue = "").lowercase()
        require(scheme == "https" || scheme == "http") { "server URL must start with https:// or http://" }
        require(scheme == "https" || credentials.allowInsecureHttp) {
            "plain http server URLs require allowing plain HTTP"
        }

        credentialStorage.saveWebDavCredentials(credentials)
        onLinked(RemoteBackupProvider.WebDav)
    }
})
