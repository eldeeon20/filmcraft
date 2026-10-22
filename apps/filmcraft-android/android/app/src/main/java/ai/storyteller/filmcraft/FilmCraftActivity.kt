package ai.storyteller.filmcraft

import android.Manifest
import android.app.AlertDialog
import android.app.NativeActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

/**
 * NativeActivity with a thin Kotlin shell: immersive full screen and the "All files access"
 * prompt on launch, which is the only way an app may reach shared storage on Android 11+.
 * The UI itself is still the original egui one; nothing here draws over it.
 */
class FilmCraftActivity : NativeActivity() {

    private var prompting = false
    private var declined = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askForStorage()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        // Back from Settings without the grant: ask again instead of leaving the in-app browser
        // stuck on app-private storage.
        askForStorage()
    }

    /// Whether shared storage is reachable: the special grant on 11+, the runtime one on 9.
    private fun hasStorage(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
        Build.VERSION.SDK_INT == Build.VERSION_CODES.P ->
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        else -> true
    }

    private fun askForStorage() {
        if (prompting || declined || hasStorage()) return
        prompting = true
        AlertDialog.Builder(this)
            .setTitle("Storage access")
            .setMessage(
                "FilmCraft needs access to your files to open and save videos outside its own " +
                    "folder.\n\nAndroid only grants this in Settings: open it and switch on " +
                    "\"Allow access to all files\" for FilmCraft."
            )
            .setPositiveButton("Open settings") { _, _ -> requestStorage() }
            .setNegativeButton("Not now") { _, _ -> declined = true }
            .setOnDismissListener { prompting = false }
            .show()
    }

    private fun requestStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:$packageName"),
            )
            try {
                startActivity(intent)
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ),
                REQ_STORAGE,
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == REQ_STORAGE && (results.isEmpty() || results.any { it != PackageManager.PERMISSION_GRANTED })) {
            askForStorage()
        }
    }

    /// Immersive mode: the timeline owns the screen; a swipe from the edge brings the bars back.
    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }
    }

    private companion object {
        const val REQ_STORAGE = 1
    }
}
