package com.example.quizbanktest.network

import com.squareup.okhttp.ResponseBody
import retrofit.Call
import retrofit.http.Body
import retrofit.http.DELETE
import retrofit.http.GET
import retrofit.http.Header
import retrofit.http.Query

interface quizRecordService {

    @GET("/allQuizRecords")
    fun getAllQuizRecords(
        @Header("Cookie") cookie:String,
        @Header("X-CSRF-Token") csrfToken: String,
        @Header("accessToken") accessToken: String,
        @Header("refreshToken") refreshToken: String,
        @Header("Session-Token")session : String,
        @Query("quizRecordType") quizRecordType: String
    ): Call<ResponseBody>

    @GET("/quizRecord")
    fun getQuizRecordWithQuestion(
        @Header("Cookie") cookie:String,
        @Header("X-CSRF-Token") csrfToken: String,
        @Header("accessToken") accessToken: String,
        @Header("refreshToken") refreshToken: String,
        @Header("Session-Token")session : String,
        @Query("quizRecordId") quizRecordId: String
    ): Call<ResponseBody>

    @DELETE("/quizRecord")
    fun postQuizRecord(
        @Header("Cookie") cookie:String,
        @Header("X-CSRF-Token") csrfToken: String,
        @Header("accessToken") accessToken: String,
        @Header("refreshToken") refreshToken: String,
        @Header("Session-Token")session : String,
        @Body body: PostQuestionRecord
    ): Call<ResponseBody>

    @DELETE("/quizRecord")
    fun deleteQuizRecord(
        @Header("Cookie") cookie:String,
        @Header("X-CSRF-Token") csrfToken: String,
        @Header("accessToken") accessToken: String,
        @Header("refreshToken") refreshToken: String,
        @Header("Session-Token")session : String,
        @Query("quizRecordId") quizRecordId: String
    ): Call<ResponseBody>
    data class QuestionInPostQuizRecord(val title: String, val number: String, val description: String,
                                 val options: ArrayList<String>, val questionType: String, val bankType: String,
                                 val questionBank: String, val answerOptions: ArrayList<String>,
                                 val answerDescription: String,  val provider: String, val originateFrom: String,
                                 val createdDate: String, val answerImage: ArrayList<String>,
                                 val questionImage: ArrayList<String>, val tag: ArrayList<String>)
    data class QuestionRecordInPostQuizRecord(val user: String, val userAnswerOptions: ArrayList<String>, val userAnswerDescription: String,
                                              val correct: Boolean, val date: String, val question: QuestionInPostQuizRecord)
    data class PostQuestionRecord(val title: String, val quizId: String, val type: String,
                                  val totalScore: Int, val duringTime: Int, val startDateTime: String, val endDateTime: String,
                                  val members: ArrayList<String>, val questionRecords: ArrayList<QuestionRecordInPostQuizRecord>)
}