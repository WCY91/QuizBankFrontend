package com.example.quizbanktest.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.introducemyself.utils.ConstantsOcrResults
import com.example.quizbanktest.models.QuestionBankModel
import com.example.quizbanktest.network.QuestionBankService
import com.example.quizbanktest.network.QuestionService
import com.google.gson.Gson
import com.squareup.okhttp.ResponseBody
import retrofit.Callback
import retrofit.GsonConverterFactory
import retrofit.Response
import retrofit.Retrofit

object ConstantsQuestionBankFunction {
    var allBanksReturnResponse : AllQuestionBanksResponse?= null
    var questionBankList : ArrayList<QuestionBankModel> = ArrayList()


    fun getAllUserQuestionBanks(context: Context,onSuccess: (ArrayList<QuestionBankModel>) -> Unit, onFailure: (String) -> Unit) {
        if (Constants.isNetworkAvailable(context)) {
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val api = retrofit.create(QuestionBankService::class.java)
            //TODO 拿到csrf token access token
            Log.e("access in scan ", Constants.accessToken)
            Log.e("COOKIE in scan ", Constants.COOKIE)
            val call = api.getAllQuestionBanks(
                Constants.COOKIE,
                Constants.csrfToken,
                Constants.session,
                "single"
            )

            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(response: Response<ResponseBody>?, retrofit: Retrofit?) {
                    if (response!!.isSuccess) {
                        val gson = Gson()
                        val allBanksResponse = gson.fromJson(
                            response.body().charStream(),
                            AllQuestionBanksResponse::class.java
                        )
                        Log.e("Response Result", allBanksResponse.questionBanks[0].toString())
                        allBanksReturnResponse = allBanksResponse
                        questionBankList.clear()
                        questionBankList = allBanksResponse.questionBanks
                        onSuccess(allBanksResponse.questionBanks)
                    } else {
                        val sc = response.code()
                        when (sc) {
                            400 -> {
                                Log.e(
                                    "Error 400", "Bad Re" +
                                            "" +
                                            "quest"
                                )
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error", "in get all banks Generic Error")
                            }
                        }
                        onFailure("Request failed with status code $sc")
                    }
                }

                override fun onFailure(t: Throwable?) {
                    onFailure("Request failed with status code ")
                    Log.e("in get all banks Errorrrrr", t?.message.toString())
                }
            })
        } else {
            Toast.makeText(
                context,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun postQuestionBank(questionBank: QuestionBankModel, activity: AppCompatActivity, onSuccess: (ArrayList<QuestionBankModel>) -> Unit, onFailure: (String) -> Unit) {
        if (Constants.isNetworkAvailable(activity)) {
            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val api = retrofit.create(QuestionBankService::class.java)
//            data class PostQuestionBody(val title: String,val number: String,val description: String,val options: ArrayList<String>,val questionType:String,val bankType:String,val questionBank:String,val answerOptions:ArrayList<String>,val answerDescription:String,val provider:String,val originateFrom:String,val createdDate:String,val image : String,val tag:ArrayList<String>)

            val body = QuestionBankService.PostQuestionBankBody(questionBank.title!!,questionBank.questionBankType!!,questionBank.createdDate,questionBank.members!!,questionBank.originateFrom!!)

            //TODO 拿到csrf token access token
            Log.e("access in scan ", Constants.accessToken)
            Log.e("COOKIE in scan ", Constants.COOKIE)
            val call = api.postQuestionBank(
                Constants.COOKIE,
                Constants.csrfToken,
                Constants.accessToken,
                Constants.refreshToken,
                body
            )

            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(response: Response<ResponseBody>?, retrofit: Retrofit?) {
                    if (response!!.isSuccess) {
                        Log.e("Response Result","success post question")
                        Toast.makeText(activity,"upload successfully",Toast.LENGTH_SHORT).show()
//                        ConstantsOcrResults.questionList.removeAt(ConstantsQuestionFunction.postQuestionPosition)

                        onSuccess("upload success")
                    } else {

                        val sc = response.code()
                        when (sc) {
                            400 -> {
                                Log.e(
                                    "Error 400", "Bad Re" +
                                            "" +
                                            "quest"+response.toString()
                                )
                                Toast.makeText(activity,"Error 400",Toast.LENGTH_SHORT).show()
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                                Toast.makeText(activity,"Not Found 400",Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                Log.e("Error", "in post question Error")
                                Toast.makeText(activity,"error",Toast.LENGTH_SHORT).show()
                            }
                        }
                        onFailure("bad request")
                    }
                }
                override fun onFailure(t: Throwable?) {
                    onFailure("bad request")
                    Log.e("in post question Errorrrrr", t?.message.toString())
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

    data class AllQuestionBanksResponse(val questionBanks:ArrayList<QuestionBankModel>)
}