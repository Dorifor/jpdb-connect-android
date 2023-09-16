package com.maooowooo.jpdbconnectandroid

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.android.volley.Request.Method
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class JpdbIntentActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var sharedText = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        if (sharedText != null) {
            sharedText = sharedText.split(" [")[0]
        }
        Log.v("Jpdb", "SHARED : $sharedText")

        if (sharedText != null) {
            parseWord(sharedText)
        }

        finish()
    }

    fun parseWord(word: String) {
        val payloadString = "{\n" +
                "\t\"text\": \"$word\",\n" +
                "\t\"position_length_encoding\": \"utf16\",\n" +
                "\t\"token_fields\": [],\n" +
                "\t\"vocabulary_fields\": [\n" +
                "\t\t\"vid\",\n" +
                "\t\t\"sid\"\n" +
                "\t]\n" +
                "}"

        val payloadJson = JSONObject(payloadString)
        Log.v("Jpdb", payloadJson.toString(4))

        sendRequest(Method.POST, "https://jpdb.io/api/v1/parse", payloadJson) { response ->
            val wordVid = ((response["vocabulary"] as JSONArray)[0] as JSONArray)[0]
            val wordSid = ((response["vocabulary"] as JSONArray)[0] as JSONArray)[1]

            addToDeck(wordVid as Number, wordSid as Number)
        }
    }

    fun addToDeck(wordVid: Number, wordSid: Number) {
        val sharedPref = this.getSharedPreferences("jpdb_prefs", Context.MODE_PRIVATE)
        val deckId = sharedPref?.getString("target_deck_id", null)

        if (deckId == null) {
//            TODO: Add toast to display an error like "hey chose a target deck or smth"
        }

        val payloadString = "{\n" +
                "\t\"id\": $deckId,\n" +
                "\t\"vocabulary\": [[$wordVid, $wordSid]]\n" +
                "}"

        val payloadJson = JSONObject(payloadString)
        Log.v("Jpdb", payloadJson.toString(4))

        sendRequest(Method.POST, "https://jpdb.io/api/v1/deck/add-vocabulary", payloadJson) { response ->
            Log.v("Jpdb", "Word uploaded !")
        }
    }

    fun sendRequest(
        method: Int,
        endpoint: String,
        payload: JSONObject? = null,
        onCompleteCallback: ((response: JSONObject) -> Unit)? = null
    ) {
        val sharedPref = this.getSharedPreferences("jpdb_prefs", Context.MODE_PRIVATE)
        val apiKey = sharedPref?.getString("api_key", null)

        if (apiKey == null) {
//            TODO: Add toast to display an error like "hey put your api key or smth"
        }

        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = object : JsonObjectRequest(
            method, endpoint, payload,
            { response -> if (onCompleteCallback != null) onCompleteCallback(response)
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
}