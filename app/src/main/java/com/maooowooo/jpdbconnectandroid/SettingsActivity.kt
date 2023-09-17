package com.maooowooo.jpdbconnectandroid

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley.newRequestQueue
import com.google.gson.Gson

open class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        val sharedPref = this.getSharedPreferences("jpdb_prefs", Context.MODE_PRIVATE)
        val settingsFragment = SettingsFragment()
        settingsFragment.sharedPref = sharedPref

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commit()
        }

        Log.v("Jpdb", sharedPref.all.toString())
    }


    class SettingsFragment : PreferenceFragmentCompat() {
        lateinit var sharedPref: SharedPreferences

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val apiKeyEditText = findPreference<EditTextPreference>("api_key")

            val storedApiKey = sharedPref.getString("api_key", null)
            if (storedApiKey != null) {
                getDeckList(storedApiKey)
                apiKeyEditText?.text = storedApiKey
            }

            apiKeyEditText?.setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                val newApiKeyString = newValue.toString()
                Log.v("Jpdb", "JPDB NEW VALUE: $newApiKeyString")
                with(sharedPref.edit()) {
                    putString("api_key", newApiKeyString)
                    apply()
                }

                getDeckList(newApiKeyString)
                true
            }
        }

        fun getDeckList(apiKey: String) {
            val queue = newRequestQueue(this.context)
            val endpoint =
                "https://jpdb.io/api/experimental/list_user_decks?fields=name,is_built_in"
            val jsonObjectRequest = object : JsonObjectRequest(
                Method.GET, endpoint, null,
                { response ->
                    Log.v("Jpdb", "Response: %s".format(response.toString()))
                    setDeckListEntries(response.toString())
                },
                { error ->
                    Log.e("Jpdb", error.toString())
                    val toast = Toast.makeText(this.context, "Error: invalid API key.", Toast.LENGTH_LONG)
                    toast.show()
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

        fun setDeckListEntries(deckJsonString: String) {
            data class DeckEntry(val id: Number, val name: String, val is_built_in: Boolean)
            data class DeckListResponse(val list: ArrayList<DeckEntry>)

            val deckListResponse: DeckListResponse =
                Gson().fromJson(deckJsonString, DeckListResponse::class.java)
            val filteredDeckList = deckListResponse.list.filter { deck -> !deck.is_built_in }
            filteredDeckList.forEach { deck ->
                Log.v("Jpdb", "Deck ${deck.id} : ${deck.name}")
            }

            val deckListEntries = filteredDeckList.map { deck -> deck.name }
            val deckListEntryValues = filteredDeckList.map { deck -> deck.id.toString() }

            val deckListPreference = findPreference<ListPreference>("deck_id")
            deckListPreference?.entries = deckListEntries.toTypedArray()
            deckListPreference?.entryValues = deckListEntryValues.toTypedArray()

            deckListPreference?.isEnabled = true

            deckListPreference?.setOnPreferenceChangeListener { _, newValue ->
                Log.v("Jpdb", "Chosen Deck: $newValue")
                if (newValue != null) {
                    deckListPreference.summary =
                        deckListEntries[deckListEntryValues.indexOf(newValue)]
                    with(sharedPref.edit()) {
                        putString("target_deck_id", newValue.toString())
                        apply()
                    }
                }
                true
            }

            val storedDeckId = sharedPref.getString("target_deck_id", null)
            Log.v("Jpdb", "STORED DECK ID: $storedDeckId")
            if (storedDeckId != null) {
                deckListPreference?.value = storedDeckId
                deckListPreference?.summary =
                    deckListEntries[deckListEntryValues.indexOf(storedDeckId)]
            }
        }
    }
}
