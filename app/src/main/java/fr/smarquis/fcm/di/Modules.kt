package fr.smarquis.fcm.di

import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import fr.smarquis.fcm.data.db.AppDatabase
import fr.smarquis.fcm.data.db.MigrateFromSharedPreferences
import fr.smarquis.fcm.data.model.Payload
import fr.smarquis.fcm.data.repository.MessageRepository
import fr.smarquis.fcm.viewmodel.MessagesViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val main = module {
    viewModel { MessagesViewModel(get(), get()) }
    single { MessageRepository(get()) }
}

val database = module {
    single { Room.databaseBuilder(androidContext(), AppDatabase::class.java, "database").addCallback(MigrateFromSharedPreferences(get(), get())).build() }
    single { get<AppDatabase>().dao() }
}

val json = module {
    single {
        Moshi.Builder()
                .add(PolymorphicJsonAdapterFactory.of(Payload::class.java, "type")
                        .withSubtype(Payload.App::class.java, "app")
                        .withSubtype(Payload.Link::class.java, "link")
                        .withSubtype(Payload.Ping::class.java, "ping")
                        .withSubtype(Payload.Raw::class.java, "raw")
                        .withSubtype(Payload.Text::class.java, "text"))
                .add(KotlinJsonAdapterFactory()).build()
    }
    single {
        mapOf(
                "app" to Payload.App::class.java,
                "link" to Payload.Link::class.java,
                "ping" to Payload.Ping::class.java,
                "text" to Payload.Text::class.java
        )
    }
}
