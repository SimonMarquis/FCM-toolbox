package fr.smarquis.fcm

import android.app.Application
import fr.smarquis.fcm.di.database
import fr.smarquis.fcm.di.firebase
import fr.smarquis.fcm.di.json
import fr.smarquis.fcm.di.main
import fr.smarquis.fcm.di.token
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin


@Suppress("unused")
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(listOf(main, token, json, database, firebase))
        }
    }

}
