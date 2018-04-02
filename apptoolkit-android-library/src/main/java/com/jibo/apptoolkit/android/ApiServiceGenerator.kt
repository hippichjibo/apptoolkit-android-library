package com.jibo.apptoolkit.android

import com.google.gson.*
import com.jibo.apptoolkit.protocol.api.*
import com.jibo.apptoolkit.android.model.api.Certificates
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.lang.reflect.Type

/**
 * Created by alexz on 26.10.17.
 */
internal object ApiServiceGenerator {

    private val gson = GsonBuilder()
            .registerTypeAdapter(UserInfo::class.java, DataDeserializer<UserInfo>())
            .registerTypeAdapter(CertData::class.java, DataDeserializer<CertData>())
            .registerTypeAdapter(Certificates::class.java, DataDeserializer<Certificates>())
            .create()

    private val logging = HttpLoggingInterceptor().setLevel(
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE)

    fun createGuestROMApiService(baseUrl: String, errorInterceptor: Interceptor?): ROMApiService {
        val httpClient = OkHttpClient.Builder()
        val builder = Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create(gson))

        val client = httpClient.build()
        httpClient.addInterceptor(logging)

        if (errorInterceptor != null) httpClient.addInterceptor(errorInterceptor)

        val retrofit = builder.client(client).build()
        return retrofit.create(ROMApiService::class.java)
    }

    fun getROMApiService(baseUrl: String, authInterceptor: Interceptor?,
                         tokenRefresher: Authenticator?, errorInterceptor: Interceptor?): ROMApiService {
        val httpClient = OkHttpClient.Builder()
        val builder = Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create(gson))

        httpClient.addInterceptor(logging)

        if (errorInterceptor != null) httpClient.addInterceptor(errorInterceptor)
        if (authInterceptor != null) httpClient.addInterceptor(authInterceptor)
        if (tokenRefresher != null) httpClient.authenticator(tokenRefresher)

        builder.client(httpClient.build())
        val retrofit = builder.build()
        return retrofit.create(ROMApiService::class.java)
    }

    interface ROMApiService {

        @get:GET("/rom/v1/info")
        val userInfo: Call<UserInfo>

        @get:GET("/rom/v1/robots")
        val robots: Call<RobotData>

        @FormUrlEncoded
        @POST("/token")
        fun getToken(@Field("grant_type") grant_type: String,
                     @Field("client_id") client_id: String,
                     @Field("client_secret") client_secret: String,
                     @Field("redirect_uri") redirect_uri: String,
                     @Field("code") code: String): Call<Token>

        @FormUrlEncoded
        @POST("/token")
        fun refreshToken(@Field("grant_type") grant_type: String,
                         @Field("client_id") client_id: String,
                         @Field("client_secret") client_secret: String,
                         @Field("refresh_token") refresh_token: String): Call<Token>

        @POST("/rom/v1/certificates")
        fun createCertificates(@Body request: Request.CertificatesRequest): Call<CertData>

        @GET("/rom/v1/certificates/client")
        fun getClientCertificates(@Query("friendlyId") friendlyId: String): Call<Certificates>
    }

    internal class DataDeserializer<T> : JsonDeserializer<T> {

        @Throws(JsonParseException::class)
        override fun deserialize(jsonElement: JsonElement, type: Type, jsonDeserializationContext: JsonDeserializationContext): T? {

            var content = jsonElement.asJsonObject.get("data")
            if (content.isJsonArray) {
                content = content.asJsonArray
            }

            return Gson().fromJson<T>(content, type)
        }
    }
}
