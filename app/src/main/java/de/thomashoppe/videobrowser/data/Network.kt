package de.thomashoppe.videobrowser.data

import android.content.Context
import android.content.pm.PackageManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import de.thomashoppe.videobrowser.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.security.MessageDigest

object Network {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @Volatile
    private var service: YouTubeApi? = null

    fun api(context: Context): YouTubeApi = service ?: synchronized(this) {
        service ?: createApi(context.applicationContext).also { service = it }
    }

    private fun createApi(context: Context): YouTubeApi {
        val androidCertificate = signingCertificateSha1(context)
        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("x-goog-api-key", BuildConfig.YOUTUBE_API_KEY)
                    .header("X-Android-Package", context.packageName)
                    .header("X-Android-Cert", androidCertificate)
                    .build()
                chain.proceed(request)
            })
            .build()

        return Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YouTubeApi::class.java)
    }

    private fun signingCertificateSha1(context: Context): String {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
        )
        val certificate = packageInfo.signingInfo?.apkContentsSigners?.firstOrNull()
            ?: error("Keine Android-Signatur für diese App gefunden.")
        return MessageDigest.getInstance("SHA-1").digest(certificate.toByteArray())
            // The Cloud Console displays SHA-1 values with colons, but its HTTP API expects raw hex.
            .joinToString(separator = "") { byte -> "%02X".format(byte.toInt() and 0xff) }
    }
}
