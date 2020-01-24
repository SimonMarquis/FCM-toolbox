package fr.smarquis.fcm

import androidx.multidex.MultiDexApplication
import fr.smarquis.fcm.di.database
import fr.smarquis.fcm.di.json
import fr.smarquis.fcm.di.main
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin


@Suppress("unused")
class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(listOf(main, json, database))
        }
    }

}
