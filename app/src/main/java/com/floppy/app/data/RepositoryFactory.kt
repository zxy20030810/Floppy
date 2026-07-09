package com.floppy.app.data

import android.content.Context
import com.floppy.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RepositoryFactory {
    fun create(context: Context): FloppyRepository {
        val profileStorage = LocalProfileStorage(context.applicationContext)
        val savedProfile = profileStorage.readProfile()
        val userId = profileStorage.getOrCreateUserId()

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // WebSocket 心跳：死链（通话/流式识别）能被及时发现，而不是永远挂着
            .pingInterval(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
        val api = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FloppyApi::class.java)
        val streamingSpeechClient = StreamingSpeechClient(client, BuildConfig.API_BASE_URL)
        val realtimeCallClient = RealtimeCallClient(client, BuildConfig.API_BASE_URL)

        if (BuildConfig.USE_MOCK_API) {
            return MockFloppyRepository(
                initialProfile = savedProfile,
                onProfileSaved = profileStorage::saveProfile,
                textIntentClient = DemoTextIntentClient(api, BuildConfig.API_BASE_URL)
            )
        }

        return RemoteFloppyRepository(
            context = context.applicationContext,
            api = api,
            streamingSpeechClient = streamingSpeechClient,
            realtimeCallClient = realtimeCallClient,
            userId = userId,
            baseUrl = BuildConfig.API_BASE_URL,
            initialProfile = savedProfile,
            onProfileSaved = profileStorage::saveProfile
        )
    }
}
