package com.example.legalhelpaiapp.emergency

import org.json.JSONArray
import org.json.JSONObject

data class EmergencyContact(
    val id: String,
    var name: String,
    var phoneNumber: String,
    var isSelected: Boolean = true
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("phoneNumber", phoneNumber)
            put("isSelected", isSelected)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): EmergencyContact {
            return EmergencyContact(
                id = json.getString("id"),
                name = json.getString("name"),
                phoneNumber = json.getString("phoneNumber"),
                isSelected = json.optBoolean("isSelected", true)
            )
        }

        fun toJsonArray(contacts: List<EmergencyContact>): String {
            val jsonArray = JSONArray()
            contacts.forEach { contact ->
                jsonArray.put(contact.toJson())
            }
            return jsonArray.toString()
        }

        fun fromJsonArray(jsonString: String): List<EmergencyContact> {
            val contacts = mutableListOf<EmergencyContact>()
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    contacts.add(fromJson(jsonArray.getJSONObject(i)))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return contacts
        }
    }
}