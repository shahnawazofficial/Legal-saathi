package com.example.legalhelpaiapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.legalhelpaiapp.databinding.ItemMessageAiBinding
import com.example.legalhelpaiapp.databinding.ItemMessageUserBinding
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter(private val messages: MutableList<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val binding = ItemMessageUserBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            UserMessageViewHolder(binding)
        } else {
            val binding = ItemMessageAiBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            AiMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is UserMessageViewHolder) {
            holder.bind(message)
        } else if (holder is AiMessageViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun updateLastMessage(message: Message) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1
            messages[lastIndex] = message
            notifyItemChanged(lastIndex)
        }
    }

    fun clearMessages() {
        val size = messages.size
        messages.clear()
        notifyItemRangeRemoved(0, size)
    }

    // User Message ViewHolder
    inner class UserMessageViewHolder(private val binding: ItemMessageUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.userMessageText.text = message.text
            binding.userMessageTime.text = formatTime(message.timestamp)

            // Update message status icon
            if (message.isRead) {
                binding.messageStatusIcon.setImageResource(R.drawable.ic_check_double)
                binding.messageStatusIcon.setColorFilter(
                    binding.root.context.getColor(android.R.color.holo_blue_light)
                )
            } else if (message.isDelivered) {
                binding.messageStatusIcon.setImageResource(R.drawable.ic_check_double)
            } else if (message.isSent) {
                binding.messageStatusIcon.setImageResource(R.drawable.ic_check)
            } else {
                binding.messageStatusIcon.setImageResource(R.drawable.ic_clock)
            }

            // Long click to show options
            binding.userMessageCard.setOnLongClickListener {
                showMessageOptions(it.context, message)
                true
            }
        }
    }

    // AI Message ViewHolder
    inner class AiMessageViewHolder(private val binding: ItemMessageAiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.aiMessageText.text = message.text
            binding.aiMessageTime.text = formatTime(message.timestamp)

            // Enable clickable links in AI messages
            binding.aiMessageText.movementMethod = LinkMovementMethod.getInstance()

            // Show/hide action buttons on click
            var actionsVisible = false
            binding.aiMessageCard.setOnClickListener {
                actionsVisible = !actionsVisible
                binding.messageActionsLayout.visibility =
                    if (actionsVisible) View.VISIBLE else View.GONE
            }

            // Copy button
            binding.copyButton.setOnClickListener {
                copyToClipboard(it.context, message.text)
            }

            // Share button
            binding.shareButton.setOnClickListener {
                shareMessage(it.context, message.text)
            }

            // Speak button (Text-to-Speech)
            binding.speakButton.setOnClickListener {
                // TODO: Implement Text-to-Speech
                Toast.makeText(it.context, "Text-to-Speech - Coming Soon", Toast.LENGTH_SHORT).show()
            }

            // Long click to show options
            binding.aiMessageCard.setOnLongClickListener {
                showMessageOptions(it.context, message)
                true
            }
        }
    }

    // Helper Functions

    private fun formatTime(date: java.util.Date): String {
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return formatter.format(date)
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("message", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun shareMessage(context: Context, text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share message via"))
    }

    private fun showMessageOptions(context: Context, message: Message) {
        val options = arrayOf("Copy", "Share", "Delete")
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Message Options")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> copyToClipboard(context, message.text)
                1 -> shareMessage(context, message.text)
                2 -> {
                    val position = messages.indexOf(message)
                    if (position != -1) {
                        messages.removeAt(position)
                        notifyItemRemoved(position)
                        Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        builder.show()
    }
}