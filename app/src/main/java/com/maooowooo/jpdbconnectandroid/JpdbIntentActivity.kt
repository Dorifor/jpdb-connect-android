package com.maooowooo.jpdbconnectandroid

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.android.volley.Request.Method
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject

data class ParsePayload(
    val text: String,
    val position_length_encoding: String = "utf16",
    val token_fields: List<String> = emptyList(),
    val vocabulary_fields: List<String> = listOf("vid", "sid")
)

data class AddToDeckPayload(
    val id: Int,
    val vocabulary: List<List<Long>>
)

class JpdbIntentActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var textToAdd = ""

        if (intent.action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: return
            textToAdd = sharedText.split(" [")[0]
        } else if (intent.action == Intent.ACTION_PROCESS_TEXT) {
            textToAdd = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT).toString()
        }

        parseText(textToAdd)
        finish()
    }

    private fun parseText(text: String) {
        val payload = ParsePayload(text)
        val payloadJson = JSONObject(Gson().toJson(payload))
        Log.v("Jpdb", payloadJson.toString(4))

        sendRequest(Method.POST, "https://jpdb.io/api/v1/parse", payloadJson) { response ->
            val parsedWords = response["vocabulary"] as JSONArray
            addToDeck(parsedWords, text)
        }
    }

    private fun addToDeck(vocabularyList: JSONArray, text: String) {
        val sharedPref = this.getSharedPreferences("jpdb_prefs", Context.MODE_PRIVATE)
        val deckId = sharedPref?.getString("target_deck_id", null)

        if (deckId == null) {
            showToast("You need to set a target deck in the settings.", Toast.LENGTH_LONG)
            return
        }

        val parseVocabList = arrayListOf<List<Long>>()

        (0 until vocabularyList.length()).forEach { i ->
            val parsedWordEntry = vocabularyList[i] as JSONArray
            val wordVid = parsedWordEntry[0].toString().toLong()
            val wordSid = parsedWordEntry[1].toString().toLong()
            parseVocabList.add(listOf(wordVid, wordSid))
        }

        val payload = AddToDeckPayload(deckId.toInt(), parseVocabList)
        val payloadJson = JSONObject(Gson().toJson(payload))

        Log.v("Jpdb", payloadJson.toString(4))

        sendRequest(
            Method.POST,
            "https://jpdb.io/api/v1/deck/add-vocabulary",
            payloadJson
        ) {
            val toastText = if (text.length > 10) text.substring(0..10) + "..." else text
            showToast("「$toastText」 added to target deck !")
        }
    }

    private fun sendRequest(
        method: Int,
        endpoint: String,
        payload: JSONObject? = null,
        onCompleteCallback: ((response: JSONObject) -> Unit)? = null
    ) {
        val sharedPref = this.getSharedPreferences("jpdb_prefs", Context.MODE_PRIVATE)
        val apiKey = sharedPref?.getString("api_key", null)

        if (apiKey == null) {
            showToast("You need to set your api key in the settings.", Toast.LENGTH_LONG)
            return
        }

        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = object : JsonObjectRequest(
            method, endpoint, payload,
            { response ->
                if (onCompleteCallback != null) onCompleteCallback(response)
            },
            { error ->
                Log.e("Jpdb", error.toString())
            }
        ) {
            @Override
            override fun getHeaders(): Map<String, String> {

                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] = "Bearer $apiKey"

                return headers
            }
        }
        queue.add(jsonObjectRequest)
    }

    private fun showToast(text: String, length: Int = Toast.LENGTH_SHORT) {
        val toast = Toast.makeText(this, text, length)
        toast.show()
    }
}