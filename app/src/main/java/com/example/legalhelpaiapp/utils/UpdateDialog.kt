package com.example.legalhelpaiapp.utils

import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.example.legalhelpaiapp.R
import com.google.android.material.button.MaterialButton

/**
 * UpdateDialog - Beautiful dialog for showing update prompts
 *
 * Features:
 * - Shows version info and release notes
 * - Download progress indicator
 * - Force update mode (no skip button)
 * - Optional update mode (can skip)
 */
class UpdateDialog(
    private val context: Context,
    private val updateInfo: UpdateInfo,
    private val onUpdateClick: () -> Unit,
    private val onLaterClick: () -> Unit
) {

    private var dialog: Dialog? = null
    private var progressBar: ProgressBar? = null
    private var progressText: TextView? = null
    private var percentageText: TextView? = null
    private var buttonLayout: View? = null
    private var progressLayout: View? = null

    fun show() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update, null)

        dialog = Dialog(context, android.R.style.Theme_Material_Dialog_NoActionBar).apply {
            setContentView(dialogView)
            setCancelable(!updateInfo.forceUpdate) // Can't dismiss if force update
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        // Setup views
        setupViews(dialogView)

        // Show dialog
        dialog?.show()
    }

    private fun setupViews(view: View) {
        // Title and message
        view.findViewById<TextView>(R.id.update_title)?.text = updateInfo.updateTitle
        view.findViewById<TextView>(R.id.update_message)?.text = updateInfo.updateMessage

        // Version info
        val updateChecker = UpdateChecker(context)
        view.findViewById<TextView>(R.id.current_version)?.text = updateChecker.getCurrentVersionName()
        view.findViewById<TextView>(R.id.new_version)?.text = updateInfo.versionName
        view.findViewById<TextView>(R.id.file_size)?.text = "(${updateInfo.fileSize})"

        // Release notes
        view.findViewById<TextView>(R.id.release_notes)?.text = updateInfo.releaseNotes

        // Progress views
        progressLayout = view.findViewById(R.id.download_progress_layout)
        progressBar = view.findViewById(R.id.download_progress_bar)
        progressText = view.findViewById(R.id.download_status_text)
        percentageText = view.findViewById(R.id.download_percentage)
        buttonLayout = view.findViewById(R.id.button_layout)

        // Force update indicator
        val forceUpdateLayout = view.findViewById<View>(R.id.force_update_layout)
        if (updateInfo.forceUpdate) {
            forceUpdateLayout?.visibility = View.VISIBLE
        }

        // Setup buttons
        val btnUpdate = view.findViewById<MaterialButton>(R.id.btn_update)
        val btnLater = view.findViewById<MaterialButton>(R.id.btn_later)

        btnUpdate?.setOnClickListener {
            startDownload()
            onUpdateClick()
        }

        btnLater?.setOnClickListener {
            dismiss()
            onLaterClick()
        }

        // Hide "Later" button for force updates
        if (updateInfo.forceUpdate) {
            btnLater?.visibility = View.GONE
        }
    }

    private fun startDownload() {
        // Show progress, hide buttons
        progressLayout?.visibility = View.VISIBLE
        buttonLayout?.visibility = View.GONE

        // Start updating progress
        updateProgress()
    }

    private fun updateProgress() {
        val updateManager = UpdateManager(context)
        val handler = Handler(Looper.getMainLooper())

        val runnable = object : Runnable {
            override fun run() {
                val progress = updateManager.getDownloadProgress()

                progressBar?.progress = progress
                percentageText?.text = "$progress%"

                if (progress < 100) {
                    handler.postDelayed(this, 500) // Update every 500ms
                } else {
                    progressText?.text = "Installing update..."
                }
            }
        }

        handler.post(runnable)
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    fun isShowing(): Boolean {
        return dialog?.isShowing == true
    }
}