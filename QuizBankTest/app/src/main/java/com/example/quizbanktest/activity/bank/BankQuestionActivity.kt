package com.example.quizbanktest.activity.bank

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.text.CaseMap.Title
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import android.window.OnBackInvokedDispatcher
import androidx.core.os.BuildCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quizbanktest.R
import com.example.quizbanktest.activity.BaseActivity
import com.example.quizbanktest.activity.group.GroupListActivity
import com.example.quizbanktest.adapters.bank.QuestionRecyclerViewAdapter
import com.example.quizbanktest.fragment.interfaces.RecyclerViewInterface
import com.example.quizbanktest.models.QuestionBankModel
import com.example.quizbanktest.models.QuestionModel
import com.example.quizbanktest.utils.ConstantsQuestionFunction
import kotlin.math.sin

class BankQuestionActivity : BaseActivity(), RecyclerViewInterface {
    // View variable
    private lateinit var tvTitle: TextView
    private lateinit var backArrowBtn: ImageButton
    private lateinit var questionRecyclerView: RecyclerView
    private lateinit var questionAdapter: QuestionRecyclerViewAdapter

    // Question variable
    private var questionModels = ArrayList<QuestionModel>()

    // Variable
    private lateinit var bankTitle: String
    private lateinit var bankId: String
    private var toast: Toast? = null
    private lateinit var newQuestionTitle: String
    private lateinit var newQuestionType: String
    private lateinit var newQuestionDate: String
    private var isModified: Boolean = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank_question)
//        setupNavigationView()
        doubleCheckExit()

        init()


        pullExit()
    }
    private fun setupQuestionModel() {
        Log.e("BankQuestionActivity", "set up question model")
        questionRecyclerView = findViewById(R.id.questionRecyclerView)
        questionAdapter = QuestionRecyclerViewAdapter(this, this, questionModels, this)

        questionRecyclerView.adapter = questionAdapter
        questionRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        questionRecyclerView.layoutManager = LinearLayoutManager(this)

        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(questionRecyclerView) {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {
                var buttons = listOf<UnderlayButton>()
                val deleteButton = deleteButton(position)
                val settingButton = settingButton(position)
                buttons = listOf(deleteButton, settingButton)
//                val archiveButton = archiveButton(position)
//                when (position) {
//                    1 -> buttons = listOf(deleteButton)
//                    2 -> buttons = listOf(deleteButton, markAsUnreadButton)
//                    3 -> buttons = listOf(deleteButton, markAsUnreadButton, archiveButton)
//                    else -> Unit
//                }
                return buttons
            }
        })

        itemTouchHelper.attachToRecyclerView(questionRecyclerView)
    }

    private fun toast(text: String) {
        toast?.cancel()
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT)
        toast?.show()
    }

    private fun deleteButton(position: Int): SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            this,
            "刪除",
            14.0f,
            android.R.color.holo_red_light,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    val deleteWarningDialog = Dialog(this@BankQuestionActivity)
                    deleteWarningDialog.setContentView(R.layout.dialog_delete_warning)
                    deleteWarningDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    deleteWarningDialog.window?.setGravity(Gravity.CENTER)
                    deleteWarningDialog.show()

                    val btnConfirm = deleteWarningDialog.findViewById<TextView>(R.id.btn_confirm)
                    val btnCancel = deleteWarningDialog.findViewById<TextView>(R.id.btn_cancel)

                    btnConfirm.setOnClickListener {
                    questionAdapter.deleteItem(position)
                        toast("Deleted item $position")
                        deleteWarningDialog.dismiss()
                    }
                    btnCancel.setOnClickListener {
                        deleteWarningDialog.dismiss()
                    }
                }
            })
    }

    private fun settingButton(position: Int) : SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            this,
            "編輯",
            14.0f,
            android.R.color.holo_green_light,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    toast("Setting item $position")
                    editQuestion(position)
                }
            })
    }

    private fun editQuestion(position: Int) {
        val editQuestionDialog = Dialog(this)
        editQuestionDialog.setContentView(R.layout.dialog_question_card)
        editQuestionDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        editQuestionDialog.window?.setGravity(Gravity.CENTER)
        editQuestionDialog.show()

        val etQuestionTitle = editQuestionDialog.findViewById<EditText>(R.id.question_title)
        val etQuestionType = editQuestionDialog.findViewById<EditText>(R.id.question_type)
        val etQuestionDate = editQuestionDialog.findViewById<EditText>(R.id.question_createdDate)

        newQuestionTitle = questionModels[position].title.toString()
        newQuestionType = questionModels[position].questionType.toString()
        newQuestionDate = questionModels[position].createdDate

        etQuestionTitle.setText(newQuestionTitle)
        etQuestionType.setText(newQuestionType)
        etQuestionDate.setText(newQuestionDate)

        etQuestionTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                newQuestionTitle = s.toString()
                isModified = true
            }
        })

        etQuestionType.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                newQuestionType = s.toString()
                isModified = true
            }
        })

        etQuestionDate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                newQuestionDate = s.toString()
                isModified = true
            }
        })

        editQuestionDialog.setOnDismissListener {
            if (isModified) {
                val data = QuestionModel(questionModels[position]._id, newQuestionTitle,
                    questionModels[position].number, questionModels[position].description,
                    questionModels[position].options, newQuestionType,
                    questionModels[position].bankType, questionModels[position].questionBank,
                    questionModels[position].answerOptions, questionModels[position].answerDescription,
                    questionModels[position].originateFrom, newQuestionDate,
                    questionModels[position].image, questionModels[position].answerImage,
                    questionModels[position].tag)
                Log.e("BankActivity", "newdata = \n$data")
                questionAdapter.setItem(position, data)
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun pullExit() {
        if (BuildCompat.isAtLeastT()) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                doubleBackToExit()
            }
        }
    }

    private var doubleBackToExitPressedOnce = false
    private fun doubleBackToExit() {
        finish()
    }

    fun backToPreviousPage(view: View?) {
        finish()
    }

    override fun onBackPressed() {
        finish()
    }

    fun init() {
        bankTitle = intent.getStringExtra("BankTitle").toString()
        bankId = intent.getStringExtra("BankId").toString()
        Log.e("BankQuestionActivity", "Bank title: $bankTitle, Bank id: $bankId")

        backArrowBtn = findViewById(R.id.btn_back_arrow)
        tvTitle = findViewById(R.id.title)

        tvTitle.text = bankTitle

        showProgressDialog("取得資料中")
        ConstantsQuestionFunction.getQuestion(this, bankId,
            onSuccess = { questionList ->
                if (questionList.isEmpty()) {
                    findViewById<ImageView>(R.id.img_empty).visibility = View.VISIBLE
                    showErrorSnackBar("裡面目前沒有題目喔")
                    hideProgressDialog()
                } else {
//                    Log.e("BankQuestionActivity", "$questionList")
                    try {
                        for (item in questionList) {
                            if (item.image == null) {
                                val image : ArrayList<String> = ArrayList()
                                item.image = image
                            }

                            if (item.answerImage == null) {
                                val answerImage : ArrayList<String> = ArrayList()
                                item.answerImage = answerImage
                            }
                            val questionModel = QuestionModel(
                                item._id,
                                item.title,
                                item.number,
                                item.description,
                                item.options,
                                item.questionType,
                                item.bankType,
                                item.questionBank,
                                item.answerOptions,
                                item.answerDescription,
                                item.originateFrom,
                                item.createdDate,
                                item.image,
                                item.answerImage,
                                item.tag
                            )
                            questionModels.add(questionModel)
                        }
                        Log.e("BankQuestionActivity", "set question model finish")
                        setupQuestionModel()
                        hideProgressDialog()
                    } catch (e: Exception) {
                        Log.e("BankQuestionActivity", "error message: $e")
                    }
                }
            },
            onFailure = {
                findViewById<ImageView>(R.id.img_empty).visibility = View.VISIBLE
                showErrorSnackBar("無法取得資料")
                hideProgressDialog()
            }
        )
    }

    override fun onItemClick(position: Int) {
        val QuestionDetailActivity = Intent(this, BankQuestionDetailActivity::class.java)

        QuestionDetailActivity.putExtra("id", questionModels[position]._id)
        QuestionDetailActivity.putExtra("title", questionModels[position].title)
        QuestionDetailActivity.putExtra("number", questionModels[position].number)
        QuestionDetailActivity.putExtra("description", questionModels[position].description)
        QuestionDetailActivity.putExtra("options", questionModels[position].options)
        QuestionDetailActivity.putExtra("type", questionModels[position].questionType)
        QuestionDetailActivity.putExtra("answerOptions", questionModels[position].answerOptions)
        QuestionDetailActivity.putExtra("answerDescription", questionModels[position].answerDescription)
        QuestionDetailActivity.putExtra("source", questionModels[position].originateFrom)
        QuestionDetailActivity.putExtra("createdDate", questionModels[position].createdDate)
        QuestionDetailActivity.putExtra("image", questionModels[position].image)
//        QuestionDetailActivity.putExtra("answerImage", questionModels[position].answerImage)
        QuestionDetailActivity.putStringArrayListExtra("answerImage", questionModels[position].answerImage)
        QuestionDetailActivity.putExtra("tag", questionModels[position].tag)

        startActivity(QuestionDetailActivity)
    }

}