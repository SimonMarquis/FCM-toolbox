package fr.smarquis.fcm.di

import androidx.room.Room
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import fr.smarquis.fcm.data.db.AppDatabase
import fr.smarquis.fcm.data.model.Payload
import fr.smarquis.fcm.data.repository.InMemoryTokenRepository
import fr.smarquis.fcm.data.repository.MessageRepository
import fr.smarquis.fcm.data.repository.TokenRepository
import fr.smarquis.fcm.usecase.GetTokenUseCase
import fr.smarquis.fcm.usecase.ResetTokenUseCase
import fr.smarquis.fcm.usecase.UpdateTokenUseCase
import fr.smarquis.fcm.viewmodel.MainViewModel
import fr.smarquis.fcm.viewmodel.PresenceLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val main = module {
    viewModel { MainViewModel(get(), get(), get(), get()) }
    single { MessageRepository(get()) }
    single { PresenceLiveData(get(), get(), get(), scope = CoroutineScope(SupervisorJob() + Default)) }
}

val token = module {
    single<TokenRepository> { InMemoryTokenRepository(get(), scope = CoroutineScope(SupervisorJob() + Default)) }
    single { GetTokenUseCase(get()) }
    single { UpdateTokenUseCase(get()) }
    single { ResetTokenUseCase(get()) }
}

val firebase = module {
    single { FirebaseMessaging.getInstance() }
    single { FirebaseDatabase.getInstance() }
}

val database = module {
    single { Room.databaseBuilder(androidContext(), AppDatabase::class.java, "database").build() }
    single { get<AppDatabase>().dao() }
}

val json = module {
    single {
        Moshi.Builder()
            .add(
                PolymorphicJsonAdapterFactory.of(Payload::class.java, "type")
                    .withSubtype(Payload.App::class.java, "app")
                    .withSubtype(Payload.Link::class.java, "link")
                    .withSubtype(Payload.Ping::class.java, "ping")
                    .withSubtype(Payload.Raw::class.java, "raw")
                    .withSubtype(Payload.Text::class.java, "text"),
            )
            .add(KotlinJsonAdapterFactory()).build()
    }
    single {
        mapOf(
            "app" to Payload.App::class.java,
            "link" to Payload.Link::class.java,
            "ping" to Payload.Ping::class.java,
            "text" to Payload.Text::class.java,
        )
    }
}
