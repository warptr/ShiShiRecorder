package com.warptr.ShiShiRecorder

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SimpleMainActivity : AppCompatActivity() {
    private lateinit var catalog: RecordingCatalog
    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var statusText: TextView
    private lateinit var prepareButton: Button
    private lateinit var folderText: TextView
    private lateinit var recordingsPanel: LinearLayout
    private lateinit var bitRateSpinner: Spinner
    private val handler = Handler(Looper.getMainLooper())
    private var prepareAfterPermission = false

    private val refreshTicker = object : Runnable {
        override fun run() {
            refreshState()
            handler.postDelayed(this, 1_000)
        }
    }

    private val projectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, InternalAudioRecordingService::class.java).apply {
                action = InternalAudioRecordingService.ACTION_PREPARE
                putExtra(InternalAudioRecordingService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(InternalAudioRecordingService.EXTRA_PROJECTION_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            toast("未授予内部音频录制权限")
        }
    }

    private val folderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                catalog.setCustomFolder(uri)
                refreshFolder()
                toast("已切换保存目录")
            }.onFailure { toast("无法保存该目录授权") }
        }
    }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            intent.getStringExtra(InternalAudioRecordingService.EXTRA_ERROR)?.let(::toast)
            refreshState()
            refreshRecordings()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        catalog = RecordingCatalog(this)
        projectionManager = getSystemService(MediaProjectionManager::class.java)
        setContentView(createContent())
        registerStateReceiver()
        if (intent.getBooleanExtra(EXTRA_TILE_START, false)) {
            prepareButton.post { prepareOrCancelRecording() }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshFolder()
        refreshState()
        refreshRecordings()
        handler.removeCallbacks(refreshTicker)
        handler.post(refreshTicker)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshTicker)
        super.onPause()
    }

    override fun onDestroy() {
        unregisterReceiver(stateReceiver)
        super.onDestroy()
    }

    private fun createContent(): View {
        val padding = dp(20)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        val scroll = ScrollView(this).apply { addView(content) }
        content.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply {
                text = "柿柿录音"
                textSize = 28f
                setTypeface(typeface, Typeface.BOLD)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(Button(context).apply {
                text = "i"
                textSize = 20f
                contentDescription = "应用使用说明"
                minWidth = dp(48)
                minimumWidth = dp(48)
                minHeight = dp(48)
                minimumHeight = dp(48)
                setOnClickListener { showAppInfo() }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
        }, fullWidth())
        content.addView(TextView(this).apply {
            text = "内部音频录制 · MP3"
            textSize = 15f
            setPadding(0, dp(4), 0, dp(20))
        })
        statusText = TextView(this).apply {
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, dp(22), 0, dp(14))
        }
        content.addView(statusText, fullWidth())
        prepareButton = Button(this).apply {
            textSize = 18f
            setOnClickListener { prepareOrCancelRecording() }
        }
        content.addView(prepareButton, fullWidth())
        content.addView(label("录音设置"))
        content.addView(settingRow("音源", "内部音频"))
        content.addView(settingRow("格式", "MP3"))
        val bitRateRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
            addView(TextView(context).apply { text = "码率" }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        bitRateSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@SimpleMainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                RecordingCatalog.BIT_RATES.map { "$it kbps" },
            )
            setSelection(RecordingCatalog.BIT_RATES.indexOf(catalog.getBitRate()).coerceAtLeast(0), false)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    catalog.setBitRate(RecordingCatalog.BIT_RATES[position])
                }
            }
        }
        bitRateRow.addView(bitRateSpinner)
        content.addView(bitRateRow, fullWidth())
        folderText = TextView(this).apply { setPadding(0, dp(12), 0, dp(4)); textSize = 15f }
        content.addView(folderText, fullWidth())
        content.addView(Button(this).apply {
            text = "选择保存文件夹"
            setOnClickListener { folderLauncher.launch(catalog.getCustomFolder()) }
        }, fullWidth())
        content.addView(Button(this).apply {
            text = "恢复默认目录（Music/MP3）"
            setOnClickListener {
                catalog.setCustomFolder(null)
                refreshFolder()
                toast("已恢复默认目录")
            }
        }, fullWidth())
        content.addView(Button(this).apply {
            text = "设置悬浮窗权限"
            setOnClickListener { openOverlaySettings() }
        }, fullWidth())
        content.addView(label("录音文件"))
        recordingsPanel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(recordingsPanel, fullWidth())
        return scroll
    }

    private fun showAppInfo() {
        val dialogContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), dp(8))
            addView(TextView(context).apply {
                text = """
                    ShiShiRecorder 是一款安卓内部音频 MP3 录音工具，只录制其他应用允许捕获的播放声音，不会录制麦克风或屏幕画面。

                    使用步骤
                    1. 首次使用，点击“设置悬浮窗权限”并允许显示在其他应用上层。
                    2. 返回主界面，选择所需码率和保存目录，然后点击“准备录制”。
                    3. 按系统提示授予录音、通知和内部音频捕获权限。
                    4. 出现悬浮窗后，点击“开始录制”；再次点击即可停止并保存 MP3。
                    5. 录音完成后，可在主界面的录音列表中播放、分享、重命名或删除文件。

                    注意：部分应用或设备不允许捕获内部音频，因此可能无法录到对应声音。默认保存目录为 Music/MP3。
                """.trimIndent()
                textSize = 16f
                setLineSpacing(0f, 1.15f)
            }, fullWidth())
            addView(TextView(context).apply {
                text = "献给我想录音去跳广场舞的母亲"
                textSize = 15f
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.ITALIC)
                setPadding(0, dp(24), 0, dp(8))
            }, fullWidth())
        }
        AlertDialog.Builder(this)
            .setTitle("ShiShiRecorder 使用说明")
            .setView(ScrollView(this).apply { addView(dialogContent) })
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun prepareOrCancelRecording() {
        if (isRecording() || isPrepared()) {
            startService(Intent(this, InternalAudioRecordingService::class.java).setAction(InternalAudioRecordingService.ACTION_STOP))
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            toast("请先允许悬浮窗权限，再准备录制")
            openOverlaySettings()
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            prepareAfterPermission = true
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            prepareAfterPermission = true
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
            return
        }
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun openOverlaySettings() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
            toast("悬浮窗权限已允许")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (prepareAfterPermission) {
            prepareAfterPermission = false
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) prepareOrCancelRecording()
            else toast("未授予所需权限")
        }
    }

    private fun refreshState() {
        when {
            isRecording() -> {
                val startedAt = getSharedPreferences(RecordingCatalog.PREFERENCES, Context.MODE_PRIVATE)
                    .getLong(RecordingCatalog.KEY_STARTED_AT, System.currentTimeMillis())
                statusText.text = "● 正在录音 ${InternalAudioRecordingService.formatDuration(System.currentTimeMillis() - startedAt)}"
                prepareButton.text = "停止录音"
                bitRateSpinner.isEnabled = false
            }
            isPrepared() -> {
                statusText.text = "● 已准备录制\n请点击悬浮窗“开始录制”"
                prepareButton.text = "取消准备"
                bitRateSpinner.isEnabled = false
            }
            else -> {
                statusText.text = "● 未录音"
                prepareButton.text = "准备录制"
                bitRateSpinner.isEnabled = true
            }
        }
    }

    private fun refreshFolder() {
        folderText.text = if (catalog.getCustomFolder() == null) "保存目录：Music/MP3" else "保存目录：自定义文件夹"
    }

    private fun refreshRecordings() {
        recordingsPanel.removeAllViews()
        val entries = catalog.list()
        if (entries.isEmpty()) {
            recordingsPanel.addView(TextView(this).apply { text = "暂无录音文件" })
            return
        }
        entries.forEach { entry -> recordingsPanel.addView(recordingRow(entry), fullWidth()) }
    }

    private fun recordingRow(entry: RecordingEntry): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(6), 0, dp(6))
        addView(Button(context).apply {
            text = "${entry.name}\n${formatDate(entry.createdAt)} · ${InternalAudioRecordingService.formatDuration(entry.durationMs)}"
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setOnClickListener { play(entry) }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(Button(context).apply {
            text = "⋮"
            setOnClickListener { showRecordingMenu(this, entry) }
        })
    }

    private fun showRecordingMenu(anchor: View, entry: RecordingEntry) {
        PopupMenu(this, anchor).apply {
            menu.add("播放").setOnMenuItemClickListener { play(entry); true }
            menu.add("分享").setOnMenuItemClickListener { share(entry); true }
            menu.add("重命名").setOnMenuItemClickListener { rename(entry); true }
            menu.add("删除").setOnMenuItemClickListener { confirmDelete(entry); true }
            show()
        }
    }

    private fun play(entry: RecordingEntry) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(entry.uri), "audio/mpeg")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        }.onFailure { toast("没有可用的播放器") }
    }

    private fun share(entry: RecordingEntry) {
        runCatching {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "audio/mpeg"
                putExtra(Intent.EXTRA_STREAM, Uri.parse(entry.uri))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "分享录音"))
        }.onFailure { toast("无法分享该录音") }
    }

    private fun rename(entry: RecordingEntry) {
        val input = EditText(this).apply { setText(entry.name.removeSuffix(".mp3")); selectAll() }
        AlertDialog.Builder(this)
            .setTitle("重命名录音")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                runCatching { catalog.rename(entry, input.text.toString()) }
                    .onSuccess { refreshRecordings() }
                    .onFailure { toast(it.message ?: "重命名失败") }
            }
            .show()
    }

    private fun confirmDelete(entry: RecordingEntry) {
        AlertDialog.Builder(this)
            .setTitle("删除录音")
            .setMessage("确定删除 ${entry.name} 吗？此操作无法恢复。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                runCatching { catalog.delete(entry) }
                    .onSuccess { refreshRecordings() }
                    .onFailure { toast("删除失败") }
            }
            .show()
    }

    private fun settingRow(name: String, value: String): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(6), 0, dp(6))
        addView(TextView(context).apply { text = name }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(TextView(context).apply { text = value })
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 18f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(24), 0, dp(8))
    }

    private fun fullWidth() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun isRecording() = getSharedPreferences(RecordingCatalog.PREFERENCES, Context.MODE_PRIVATE)
        .getBoolean(RecordingCatalog.KEY_ACTIVE, false)
    private fun isPrepared() = getSharedPreferences(RecordingCatalog.PREFERENCES, Context.MODE_PRIVATE)
        .getBoolean(RecordingCatalog.KEY_PREPARED, false)
    private fun formatDate(value: Long) = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(value))

    private fun registerStateReceiver() {
        val filter = IntentFilter(InternalAudioRecordingService.ACTION_STATE_CHANGED)
        ContextCompat.registerReceiver(this, stateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    companion object {
        const val EXTRA_TILE_START = "tile_start"
        private const val REQUEST_RECORD_AUDIO = 11
        private const val REQUEST_NOTIFICATIONS = 12
    }
}
