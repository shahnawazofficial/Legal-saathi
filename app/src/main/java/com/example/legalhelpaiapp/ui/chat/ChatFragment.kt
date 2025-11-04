package com.example.legalhelpaiapp.ui.chat

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.legalhelpaiapp.Message
import com.example.legalhelpaiapp.MessageAdapter
import com.example.legalhelpaiapp.databinding.FragmentChatBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var messageAdapter: MessageAdapter
    private val messageList = mutableListOf<Message>()
    private val TAG = "LegalHelpAI"

    // Model information for the API call
    private val MODEL_NAME = "gemini-2.5-flash-preview-09-2025"
    private val API_URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models/"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSendButton()

        // Add an initial welcome message from the AI
        addWelcomeMessage()
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messageList)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.chatRecyclerView.adapter = messageAdapter
    }

    private fun setupSendButton() {
        binding.sendButton.setOnClickListener {
            val messageText = binding.chatInputText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                val userMessage = Message(messageText, isUser = true, timestamp = Date())
                messageAdapter.addMessage(userMessage)

                // Clear the input text and scroll to the bottom
                binding.chatInputText.text.clear()
                binding.chatRecyclerView.scrollToPosition(messageAdapter.itemCount - 1)

                // Send message to AI and get a response
                sendToAI(messageText)
            }
        }
    }

    private fun addWelcomeMessage() {
        val welcomeMessage = Message(
            text = "Hello! I am your Legal AI Assistant. How can I help you with accurate legal information and schemes today?",
            isUser = false,
            timestamp = Date()
        )
        messageAdapter.addMessage(welcomeMessage)
    }

    /**
     * Attempts to read the API Key from the AndroidManifest meta-data.
     */
    private fun getApiKeyFromManifest(): String? {
        return try {
            val ai: ApplicationInfo = requireContext().packageManager.getApplicationInfo(
                requireContext().packageName,
                PackageManager.GET_META_DATA
            )
            ai.metaData.getString("com.google.android.gemini.API_KEY")
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving API Key from Manifest: ${e.message}")
            null
        }
    }

    /**
     * Constructs the prompt and payload to send to the Google Generative Language API.
     */
    private fun sendToAI(prompt: String) {
        val apiKey = getApiKeyFromManifest()

        if (apiKey.isNullOrEmpty()) {
            Toast.makeText(
                context,
                "ERROR: AI Key not found in Manifest. Check your Gradle build process.",
                Toast.LENGTH_LONG
            ).show()
            return
        }



        // System Instruction to guide the AI's persona and response structure
        val systemInstruction = """
You are LEGAL SAATHI. Format replies very clean and simple. No numbers, no headings, no paragraphs.

Format every reply like this:

Issue:
• 1–2 line simple explanation of the issue
• Mention relevant IPC or IT Act sections

Steps to take:
• Practical steps user should follow
• If cybercrime → include 1930 helpline
• If needed → visit police station or cyber cell

Court Process:
• What document to submit (very short)
• Where to go
• What happens after filing

Emergency:
• Cyber helpline: 1930
• Police: 112
• Women: 1091 (only if relevant)
• Child: 1098 (only if relevant)

Online Complaint:
• Official GOV portal link ONLY

Rules:
• Use plain text
• Use bullet points only (•)
• No numbering (1,2,3)
• No long paragraphs
• No headings like "1) Issue Brief"
• Always keep very short & clear

Greeting rule:
If user says "hi, hello, good morning etc":
Reply: "Hello! I am Legal Saathi. How can I help you with legal support today?"

Non-legal query rule:
If query is unrelated to Indian law:
Reply: "I can only assist with Indian legal matters and government legal support."

Always obey this format.
""".trimIndent()



        val userQuery = "Provide accurate information for this legal query: '$prompt' also, if relevant, suggest the easiest help method and the official portal URL."

        // The main content payload structure
        val contentsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", userQuery) })
                })
            })
        }

        // Full payload including grounding and system instructions
        val jsonPayload = JSONObject().apply {
            put("contents", contentsArray)
            put("tools", JSONArray().apply {
                put(JSONObject().apply { put("google_search", JSONObject()) })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Construct the full URL with the API Key
                val url = URL("$API_URL_BASE$MODEL_NAME:generateContent?key=$apiKey")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Write the JSON payload
                connection.outputStream.bufferedWriter().use { writer ->
                    writer.write(jsonPayload.toString())
                }

                val responseCode = connection.responseCode
                val aiResponse: String

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val responseString = reader.readText()
                    reader.close()

                    val jsonResult = JSONObject(responseString)
                    aiResponse = parseAiResponse(jsonResult)

                } else {
                    // Handle non-200 responses
                    val errorReader = BufferedReader(InputStreamReader(connection.errorStream))
                    val errorString = errorReader.readText()
                    errorReader.close()
                    Log.e(TAG, "HTTP Error $responseCode: $errorString")
                    aiResponse = "I'm facing a connection issue (Code $responseCode). This often indicates a server-side problem. Please check your network and try again, or visit the Resources tab for immediate help."
                }

                // Update UI on the main thread
                withContext(Dispatchers.Main) {
                    val aiMessage = Message(aiResponse, isUser = false, timestamp = Date())
                    messageAdapter.addMessage(aiMessage)
                    binding.chatRecyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Network or Parsing Exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Connection failed. Please check your network.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Parses the AI response JSON to extract the text and includes any grounding sources.
     */
    private fun parseAiResponse(jsonResult: JSONObject): String {
        return try {
            val candidate = jsonResult.getJSONArray("candidates").getJSONObject(0)
            val text = candidate.getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val sources = candidate.optJSONObject("groundingMetadata")
                ?.optJSONArray("groundingAttributions")

            val sourceText = if (sources != null && sources.length() > 0) {
                val firstSource = sources.getJSONObject(0)
                val uri = firstSource.optJSONObject("web")?.optString("uri")
                val title = firstSource.optJSONObject("web")?.optString("title")
                if (!uri.isNullOrEmpty() && !title.isNullOrEmpty()) {
                    "\n\n**Source:** [$title]($uri)"
                } else ""
            } else ""

            text + sourceText

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI response: ${e.message}", e)
            "I received an unreadable response from the AI. Please try rephrasing your query."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}