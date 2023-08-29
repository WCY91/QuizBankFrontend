package com.example.quizbanktest.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.example.quizbanktest.network.RealEsrganService
import com.google.gson.Gson
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.ResponseBody
import okio.Buffer
import okio.BufferedSource
import retrofit.Callback
import retrofit.GsonConverterFactory
import retrofit.Response
import retrofit.Retrofit
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit


object ConstantsRealESRGAN {
    fun realEsrgan(base64String: String, activity: Activity, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        if (Constants.isNetworkAvailable(activity)) {
            val client = OkHttpClient()
            client.setConnectTimeout(560, TimeUnit.SECONDS) // 設定連接超時時間

            client.setReadTimeout(560, TimeUnit.SECONDS) // 設定讀取超時時間

            client.setWriteTimeout(560, TimeUnit.SECONDS) // 設定寫入超時時間

            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
            val api = retrofit.create(RealEsrganService::class.java)
            val compressBase64 = base64String
//            copyToClipboard(activity, base64String!!)
            val body =RealEsrganService.PostBody(compressBase64!!)

            //TODO 拿到csrf token access token
            Log.e("access in scan ", Constants.accessToken)
            Log.e("COOKIE in scan ", Constants.COOKIE)
            val call = api.realesrgan(
                Constants.COOKIE,
                Constants.csrfToken,
                Constants.accessToken,
                Constants.refreshToken,
                body
            )

            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(response: Response<ResponseBody>?, retrofit: Retrofit?) {
                    val source: BufferedSource? = response?.body()?.source()
                    source?.request(Long.MAX_VALUE) // Buffer the entire body.

                    val buffer: Buffer? = source?.buffer()
                    val UTF8: Charset = Charset.forName("UTF-8")
                    buffer?.clone()?.readString(UTF8)?.let { Log.d("REQUEST_JSON", it) }
                    if (response!!.isSuccess) {
                        val gson = Gson()
                        val esrganResponse = gson.fromJson(
                            response.body().charStream(),
                            EsrganResponse::class.java
                        )
                        Log.e("Response Result", esrganResponse.image)

                        onSuccess(esrganResponse.image)

                    } else {

                        val sc = response.code()


                        when (sc) {
                            400 -> {

                                Log.e(
                                    "Error 400", "Bad Re" +
                                            "" +
                                            "quest"
                                )
                                onFailure("Request failed with status code $sc")
                            }
                            404 -> {

                                Log.e("Error 404", "Not Found")
                                onFailure("Request failed with status code $sc")
                            }
                            else -> {
                                Log.e("Error","esrgan error")
                                onFailure("Request failed with status code $sc")
                            }

                        }
                    }
                }

                override fun onFailure(t: Throwable?) {

                    Log.e("in esrgan Errorrrrr", t?.message.toString())
                    onFailure("Request failed with status code")
                }
            })
        } else {

            Toast.makeText(
                activity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }

    }
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("base64", text)
        clipboard.setPrimaryClip(clip)
    }
//    fun compressBase64(base64Str: String, quality: Int): String {
//        // 解碼Base64字符串到位圖
//        val decodedBytes = Base64.decode(base64Str, 0)
//        val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
//
//        // 壓縮位圖
//        val byteArrayOutputStream = ByteArrayOutputStream()
//        decodedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
//        val compressedBytes = byteArrayOutputStream.toByteArray()
//
//        Log.e("esrgan image ",
//            ConstantsFunction.estimateBase64SizeFromBase64String( Base64.encodeToString(compressedBytes, Base64.NO_WRAP))
//                .toString()
//        )
//        // 重新編碼為Base64
//        return Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
//    }
    data class EsrganResponse(val image: String)

}