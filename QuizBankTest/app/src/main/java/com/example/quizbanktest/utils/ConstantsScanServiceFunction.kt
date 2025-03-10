package com.example.quizbanktest.utils

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.introducemyself.utils.ConstantsOcrResults
import com.example.quizbanktest.activity.BaseActivity
import com.example.quizbanktest.activity.IntroActivity
import com.example.quizbanktest.activity.scan.ScannerTextWorkSpaceActivity
import com.example.quizbanktest.models.QuestionBankModel
import com.example.quizbanktest.network.ScanImageService
import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

//import com.squareup.okhttp.ResponseBody
//import retrofit.Callback
//import retrofit.GsonConverterFactory
//import retrofit.Response
//import retrofit.Retrofit

object ConstantsScanServiceFunction {
    fun scanBase64ToOcrText(base64String: String, activity:BaseActivity, flag:Int,onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        if (Constants.isNetworkAvailable(activity)) {
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val api = retrofit.create(ScanImageService::class.java)
            val body = ScanImageService.PostBody(base64String)

            //TODO 拿到csrf token access token
            Log.e("access in scan ", Constants.accessToken)
            Log.e("COOKIE in scan ", Constants.COOKIE)
            val call = api.scanBase64(
                Constants.COOKIE,
                Constants.csrfToken,
                Constants.accessToken,
                Constants.refreshToken,
                body
            )

            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>,
                                        response: Response<ResponseBody>) {
                    if (response!!.isSuccessful) {
                        val gson = Gson()
                        val ocrResponse = gson.fromJson(
                            response.body()?.charStream(),
                            OCRResponse::class.java
                        )
//                        Log.e("Response Result", ocrResponse.text)
                        if(flag==0){
                           //不用換頁因為是答案ocr
                            onSuccess(ocrResponse.text)
                        }
                        else{
                            if(!ocrResponse.text.equals("")){
                                onSuccess(ocrResponse.text)
                            }else{
                                onFailure("辨識不出來目前的圖片請重新上傳")
                            }

                        }


                    } else {

                        val sc = response.code()
                        activity.hideProgressDialog()

                        when (sc) {
                            400 -> {
                                activity.showErrorSnackBar("發生了錯誤(BAD REQUEST)")
                                Log.e(
                                    "Error 400", "Bad Re" +
                                            "" +
                                            "quest"
                                )
                                onFailure("Request failed with status code $sc")
                            }
                            404 -> {
                                activity.showErrorSnackBar("系統找不到")
                                Log.e("Error 404", "Not Found")
                                onFailure("Request failed with status code $sc")
                            }
                            401 -> {
                                val intent = Intent(activity, IntroActivity::class.java)
                                activity.startActivity(intent)
                            }
                            else -> {
                                Log.e("Error", "in scan Generic Error")
                                onFailure("Request failed with status code $sc")
                            }

                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    activity.showErrorSnackBar("掃描發生錯誤")
                    activity.hideProgressDialog()
                    Log.e("in scan Errorrrrr", t?.message.toString())
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

    fun scanPhotoBase64ToOcrText(base64String: String, activity: AppCompatActivity, flag:Int,onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        if (Constants.isNetworkAvailable(activity)) {
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val api = retrofit.create(ScanImageService::class.java)
            val body = ScanImageService.PostBody(base64String)

            //TODO 拿到csrf token access token
            Log.e("access in scan ", Constants.accessToken)
            Log.e("COOKIE in scan ", Constants.COOKIE)
            val call = api.scanBase64(
                Constants.COOKIE,
                Constants.csrfToken,
                Constants.accessToken,
                Constants.refreshToken,
                body
            )

            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>,
                                        response: Response<ResponseBody>) {
                    if (response!!.isSuccessful) {
                        val gson = Gson()
                        val ocrResponse = gson.fromJson(
                            response.body()?.charStream(),
                            OCRResponse::class.java
                        )
//                        Log.e("Response Result", ocrResponse.text)
                        if(flag==0){
                            //不用換頁因為是答案ocr
                            onSuccess(ocrResponse.text)
                        }
                        else{
                            if(!ocrResponse.text.equals("")){
                                onSuccess(ocrResponse.text)
                            }else{
                                onFailure("辨識不出來目前的圖片請重新上傳")
                            }

                        }


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
                            401 -> {
                                val intent = Intent(activity, IntroActivity::class.java)
                                activity.startActivity(intent)
                            }
                            else -> {
                                Log.e("Error", "in scan Generic Error")
                                onFailure("Request failed with status code $sc")
                            }

                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

                    Log.e("in scan Errorrrrr", t?.message.toString())
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
    data class OCRResponse(val text: String)

}