package fr.smarquis.fcm.di

import android.net.Uri
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson


class UriJsonAdapter {

    @ToJson
    fun toJson(value: Uri): String {
        return value.toString()
    }

    @FromJson
    fun fromJson(value: String?): Uri {
        return Uri.parse(value)
    }
}
