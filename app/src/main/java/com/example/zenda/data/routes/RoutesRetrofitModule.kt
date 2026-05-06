package com.example.zenda.data.routes

import com.example.zenda.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RoutesRetrofitModule {

    /**
     * FieldMask mínimo para navegación in-app: distancia/tiempo, polyline y pasos.
     * Puedes extenderlo después (tráfico, alternativas, etc.).
     */
    private const val FIELD_MASK =
        "routes.distanceMeters," +
            "routes.duration," +
            "routes.polyline.encodedPolyline," +
            "routes.legs.distanceMeters," +
            "routes.legs.duration," +
            "routes.legs.steps.navigationInstruction"

    private val headersInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .header("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
            .header("X-Goog-FieldMask", FIELD_MASK)
            .header("Content-Type", "application/json")
            .build()
        chain.proceed(req)
    }

    private val okHttp: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(headersInterceptor)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
        }
        builder.build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://routes.googleapis.com/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: RoutesApiService by lazy {
        retrofit.create(RoutesApiService::class.java)
    }
}

