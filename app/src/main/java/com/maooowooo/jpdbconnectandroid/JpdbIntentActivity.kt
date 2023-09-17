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

            addToDeck(wordVid as Number, wordSid as Number, word)
        }
    }

    fun addToDeck(wordVid: Number, wordSid: Number, word: String) {
        val sharedPref = this.getSharedPreferences("jpdb_prefs", Context.MODE_PRIVATE)
        val deckId = sharedPref?.getString("target_deck_id", null)

        if (deckId == null) {
            val toast = Toast.makeText(this, "You need to set a target deck in the settings.", Toast.LENGTH_SHORT)
            toast.show()
            return
        }

        val payloadString = "{\n" +
                "\t\"id\": $deckId,\n" +
                "\t\"vocabulary\": [[$wordVid, $wordSid]]\n" +
                "}"

        val payloadJson = JSONObject(payloadString)
        Log.v("Jpdb", payloadJson.toString(4))

        sendRequest(Method.POST, "https://jpdb.io/api/v1/deck/add-vocabulary", payloadJson) { response ->
            val toast = Toast.makeText(this, "「$word」 added to target deck !", Toast.LENGTH_SHORT)
            toast.show()
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
            val toast = Toast.makeText(this, "You need to set your api key in the settings.", Toast.LENGTH_SHORT)
            toast.show()
            return
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