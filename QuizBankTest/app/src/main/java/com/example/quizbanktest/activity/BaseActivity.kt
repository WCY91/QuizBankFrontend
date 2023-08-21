package com.example.quizbanktest.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings.Global.putString
import android.util.Log
import android.widget.*
import android.window.OnBackInvokedDispatcher
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.BuildCompat
import androidx.lifecycle.lifecycleScope
import com.example.introducemyself.utils.ConstantsOcrResults
import com.example.quizbanktest.R
import com.example.quizbanktest.activity.account.AccountSettingActivity
import com.example.quizbanktest.activity.bank.BankActivity
import com.example.quizbanktest.activity.quiz.QuizPage
import com.example.quizbanktest.activity.scan.ScanActivity
import com.example.quizbanktest.activity.scan.ScannerTextWorkSpaceActivity
import com.example.quizbanktest.utils.*
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


open class BaseActivity : AppCompatActivity() {

    private val SAMPLE_CROPPED_IMG_NAME = "CroppedImage.jpg"
    private var onImageSelected: ((Bitmap?) -> Unit)? = null
    private var cameraPhotoUri : Uri ?=null
    private lateinit var mProgressDialog: Dialog
    private lateinit var heartbeatScheduler: ScheduledExecutorService
    private var heartbeatFuture: ScheduledFuture<*>? = null
    private lateinit var showRotateDialog : Dialog
    private lateinit var showAutoCutOptionsDialog : Dialog
    val openGalleryLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult() ){
            result->

        Toast.makeText(this, "gallery!", Toast.LENGTH_SHORT).show()

        if(result.resultCode == RESULT_OK &&result.data!=null){

            val contentURI = result.data!!.data
            try {
                val selectedImageBitmap =
                    BitmapFactory.decodeStream(getContentResolver().openInputStream(contentURI!!))
                if(selectedImageBitmap!=null){
                    onImageSelected?.invoke(selectedImageBitmap)
                }
            } catch (e: IOException) {
                onImageSelected?.invoke(null)
                e.printStackTrace()
                showErrorSnackBar("開啟相簿錯誤")
            }
        }
    }
    val cameraLauncher: ActivityResultLauncher<Intent> =registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){
            result->
        if(result.resultCode == RESULT_OK){
            val thumbnail: Bitmap? = BitmapFactory.decodeStream(getContentResolver().openInputStream(cameraPhotoUri!!))
            lifecycleScope.launch{
                var returnString = saveBitmapFileForPicturesDir(thumbnail!!)
            }
            var base64String = ConstantsFunction.encodeImage(thumbnail!!)
            showProgressDialog("目前正在處理OCR之結果")
            autoOcrAndRotate(base64String!!,0,onSuccess1 = { it1 -> }, onFailure1 = { it1 -> })
        }else if(result.resultCode == RESULT_CANCELED){

        }

    }
    val startScanActivityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = Intent(this@BaseActivity,ScannerTextWorkSpaceActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    fun showProgressDialog(text: String) {
        mProgressDialog = Dialog(this)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        mProgressDialog.setContentView(R.layout.dialog_progress)

        mProgressDialog.findViewById<TextView>(R.id.tv_progressbar_text).text=text

        //Start the dialog and display it on screen.
        mProgressDialog.show()
    }

    fun showErrorSnackBar(message: String) {
        val snackBar =
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(
            ContextCompat.getColor(
                this@BaseActivity,
                R.color.red
            )
        )
        snackBar.show()
    }
    fun hideProgressDialog() {
        mProgressDialog.dismiss()
    }


    suspend fun saveBitmapFileForPicturesDir(mBitmap: Bitmap?): String {
        showProgressDialog("目前正在儲存圖片請稍等")

        var result = ""
        if (mBitmap != null) {
            var base64URL = ConstantsFunction.encodeImage(mBitmap)
        }
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val fileName = "QuizBank_${idImage}.jpg"
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        }
                    }

                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    contentResolver.openOutputStream(uri!!).use { outputStream ->
                        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                    result = uri.toString()

                    runOnUiThread {
                        if (!result.isEmpty()) {
                            Toast.makeText(
                                this@BaseActivity,
                                "success scan",
                                Toast.LENGTH_SHORT
                            ).show()
                            hideProgressDialog()
                        } else {
                            Toast.makeText(
                                this@BaseActivity,
                                "Something went wrong while saving the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                            hideProgressDialog()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                    hideProgressDialog()
                }
            }
        }
        return result
    }



    var idImage = System.currentTimeMillis()/1000

    fun cameraPick(){

        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems =
            arrayOf("掃描", "拍照新增題目","從相簿選擇")
        pictureDialog.setItems(
            pictureDialogItems
        ) { dialog, which ->
            when (which) {
                0-> scanPhoto()
                // Here we have create the methods for image selection from GALLERY
                1 -> takePhotoFromCamera()
                2 -> choosePhotoToOcr(0, onSuccess = { it1 ->  }, onFailure = { it1 -> })
            }
        }
        pictureDialog.show()
    }

    fun scanPhoto(){
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {

                    val scanIntent = Intent(this@BaseActivity, ScanActivity::class.java)
                    startScanActivityForResult.launch(scanIntent)

                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    ConstantsFunction.showRationalDialogForPermissions(this@BaseActivity)
                }
            }).onSameThread()
            .check()
    }
    fun takePhotoFromCamera() {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    val f =
                        File(externalCacheDir?.absoluteFile.toString() + File.separator + "QuizBank_Camera_" + idImage + ".jpg")
                    val uri1 = FileProvider.getUriForFile(this@BaseActivity, "com.example.quizbanktest.fileprovider", f)
                    intent.putExtra(
                        MediaStore.EXTRA_OUTPUT,
                        uri1)
                    cameraPhotoUri = uri1
                    cameraLauncher.launch(intent)
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    ConstantsFunction.showRationalDialogForPermissions(this@BaseActivity)
                }
            }).onSameThread()
            .check()
    }
    fun showAlertFromWorkSpace(onConfirm : () -> Unit) {
        if (ConstantsOcrResults.questionList.isNotEmpty()) {
            val builder = AlertDialog.Builder(this,R.style.CustomAlertDialogStyle)
                .setMessage(" 您確定要離開嗎系統不會保存這次修改喔 ")
                .setTitle("OCR結果")
                .setIcon(R.drawable.baseline_warning_amber_24)
            builder.setPositiveButton("確認") { _, _ -> onConfirm.invoke() }
            builder.setNegativeButton("取消", null)
            builder.show()
        } else {
            onConfirm.invoke()
        }
    }

    fun gotoBankActivity(){
        ConstantsQuestionBankFunction.getAllUserQuestionBanks(this,
            onSuccess = { questionBanks ->
                when (this) {
                    is ScannerTextWorkSpaceActivity -> {
                        showAlertFromWorkSpace {
                            val intent = Intent(this, BankActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }else ->{
                        val intent = Intent(this, BankActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            },
            onFailure = { errorMessage ->
                Toast.makeText(this,"server error",Toast.LENGTH_SHORT).show()
            }
        )
    }

    fun gotoHomeActivity(){
        when (this) {
            is ScannerTextWorkSpaceActivity -> {
                showAlertFromWorkSpace {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }else ->{
                val intent = Intent(this,MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

    }

    fun gotoQuizActivity(){
        when (this) {
            is ScannerTextWorkSpaceActivity -> {
                showAlertFromWorkSpace {
                    val intent = Intent(this, QuizPage::class.java)
                    startActivity(intent)
                    finish()
                }
            }else ->{
            val intent = Intent(this,QuizPage::class.java)
            startActivity(intent)
            finish()
            }
        }

    }

    fun gotoSettingActivity(){
        when (this) {
            is ScannerTextWorkSpaceActivity -> {
                showAlertFromWorkSpace {
                    val intent = Intent(this, AccountSettingActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }else ->{
            val intent = Intent(this,AccountSettingActivity::class.java)
            startActivity(intent)
            finish()
        }
        }

    }

    fun choosePhotoFromGallery(onImageSelected: (Bitmap?) -> Unit) {
        this.onImageSelected = onImageSelected
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    val galleryIntent = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                    openGalleryLauncher.launch(galleryIntent)
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    ConstantsFunction.showRationalDialogForPermissions(this@BaseActivity)
                }
            }).onSameThread()
            .check()
    }

    fun  choosePhotoToOcr(flag : Int,onSuccess: (String) -> Unit, onFailure: (String) -> Unit){ // 0 :普通掃描 1:重新掃描
        choosePhotoFromGallery {
            bitmap ->
            if (bitmap != null) {
                var base64String = ConstantsFunction.encodeImage(bitmap)
                autoOcrAndRotate(base64String!!,flag,
                    onSuccess1 = { it1 ->
                    onSuccess(it1)
                   }
                    ,onFailure1 = { it1 ->

                        onFailure(it1)
                        }
                )
            }else{
                onFailure("can't choose empty photo")
                Toast.makeText(this@BaseActivity,"You can't choose empty photo to ocr",Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun setupNavigationView() {
        val quiz : ImageButton = findViewById(R.id.test)
        quiz.setOnClickListener {
            gotoQuizActivity()
        }
        val bank : ImageButton = findViewById(R.id.bank)
        bank.setOnClickListener{
            gotoBankActivity()
        }

        val homeButton : ImageButton = findViewById(R.id.home)
        homeButton.setOnClickListener{
            gotoHomeActivity()
        }

        val camera : ImageButton = findViewById(R.id.camera)
        camera.setOnClickListener {
            cameraPick()
        }

        val settingButton : ImageButton = findViewById(R.id.setting)
        settingButton.setOnClickListener{
            gotoSettingActivity()
        }

    }

    private var backPressedTime: Long = 0
    private val BACK_PRESS_THRESHOLD = 2000  // 2000 milliseconds = 2 seconds
    @SuppressLint("UnsafeOptInUsageError")
    open fun doubleCheckExit(){
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
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime > BACK_PRESS_THRESHOLD) {
            Toast.makeText(this, "再按一次返回鍵退出", Toast.LENGTH_SHORT).show()
            backPressedTime = currentTime
        } else {
            moveTaskToBack(true)
        }
    }

    fun splitQuestionOptions(text: String): Pair<String, List<String>> {
        val pattern = "\\s*(?:[A-Za-z0-9一二三四五六七八九十]+\\)|[A-Za-z0-9一二三四五六七八九十]+\\.)".toRegex()
        val optionIndices = pattern.findAll(text).map { it.range.first }.toList()

        if (optionIndices.isEmpty()) {
            return Pair(text, emptyList())
        }


        val question = text.substring(0, optionIndices[0]).trim()
        val options = optionIndices.mapIndexed { index, start ->
            val end = optionIndices.getOrNull(index + 1) ?: text.length
            text.substring(start, end).trim()
        }
        for(i in options){
//            Log.e("list options",i)
        }

        return Pair(question, options)
    }

    override fun onBackPressed() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime > BACK_PRESS_THRESHOLD) {
            // If back is pressed beyond the threshold time (2 seconds), show a Toast
            Toast.makeText(this, "再按一次返回鍵退出", Toast.LENGTH_SHORT).show()
            backPressedTime = currentTime
        } else {
            moveTaskToBack(true)
            // If back is pressed within the threshold time, finish the activity
//            super.onBackPressed()
        }
    }

    fun startHeartbeatForCsrf() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor()
        heartbeatFuture = heartbeatScheduler.scheduleAtFixedRate({
            ConstantsAccountServiceFunction.getCsrfToken(this,
                onSuccess = { it1 ->

                    ConstantsAccountServiceFunction.login(this, " ", " ",
                        onSuccess = {   message->

                        },
                        onFailure = { message->

                        })
                },
                onFailure = { it1 ->
                    Log.d("get csrf fail", it1)
                })
        }, 0, 3300, TimeUnit.SECONDS)
    }

    fun stopHeartbeatForCsrf() {
        heartbeatFuture?.cancel(false)
        heartbeatScheduler.shutdown()
    }

     @SuppressLint("CommitPrefEdits")
     fun showAutoRotateDialog(base64String:String) {
         showRotateDialog = Dialog(this)
         showRotateDialog.setContentView(R.layout.dialog_auto_rotate)

         val rotateEnter = showRotateDialog.findViewById<TextView>(R.id.auto_rotate_enter)
         val rotateCancel = showRotateDialog.findViewById<TextView>(R.id.auto_rotate_cancel)
         val checkBox = showRotateDialog.findViewById<CheckBox>(R.id.rotate_check)
         val preferences = getSharedPreferences("settings", MODE_PRIVATE)
         val editor = preferences.edit()
         checkBox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
             editor.putBoolean("doNotShowAgain", isChecked)
             editor.apply()
         }

         rotateEnter.setOnClickListener {
             editor.putBoolean("rotate", true)
             editor.apply()
             showRotateDialog.dismiss()
             showProgressDialog("目前正在處理OCR之結果")
             ConstantsHoughAlgo.imageRotate(
                 base64String,this@BaseActivity,
                 onSuccess = { it1 ->

                     ConstantsScanServiceFunction.scanBase64ToOcrText(it1, this@BaseActivity,1, onSuccess = { it1 ->
                         hideProgressDialog()
                         processScan(it1)
                     }, onFailure = { it1 ->
                         hideProgressDialog()
                         showErrorScan()
                     })
                 }, onFailure = { it1 ->

                     ConstantsScanServiceFunction.scanBase64ToOcrText(base64String, this@BaseActivity,1, onSuccess = { it1 ->
                         hideProgressDialog()
                         processScan(it1)
                     }, onFailure = { it1 ->
                         hideProgressDialog()
                         showErrorScan()
                     })
                 } )

         }

         rotateCancel.setOnClickListener {
             editor.putBoolean("rotate", false)
             editor.apply()
             showRotateDialog.dismiss()

             showProgressDialog("目前正在處理OCR之結果")
             ConstantsScanServiceFunction.scanBase64ToOcrText(base64String!!, this@BaseActivity,1, onSuccess = { it1 -> hideProgressDialog() }, onFailure = { it1 -> hideProgressDialog()})
         }
         showRotateDialog.show()
         showRotateDialog.setOnDismissListener{

         }

     }


    @SuppressLint("CommitPrefEdits")
    fun showAutoCutOptionsDialog() {
        showAutoCutOptionsDialog = Dialog(this)
        showAutoCutOptionsDialog.setContentView(R.layout.dialog_auto_cut_options)

        val cutOptionsEnter = showAutoCutOptionsDialog.findViewById<TextView>(R.id.auto_cut_options_enter)
        val cutOptionsCancel =showAutoCutOptionsDialog.findViewById<TextView>(R.id.auto_cut_options_cancel)
        val checkBox = showAutoCutOptionsDialog.findViewById<CheckBox>(R.id.cut_options_check)
        val preferences = getSharedPreferences("settings", MODE_PRIVATE)
        val editor = preferences.edit()
        checkBox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            editor.putBoolean("doNotShowAgainForCutOptions", isChecked)
            editor.apply()
        }

        cutOptionsEnter.setOnClickListener {
            editor.putBoolean("cut_options", true)
            editor.apply()
            showAutoCutOptionsDialog.dismiss()
            Toast.makeText(this,"dialog to do cut options",Toast.LENGTH_SHORT).show()
        }

        cutOptionsCancel.setOnClickListener {
            editor.putBoolean("cut_options", false)
            editor.apply()
            showAutoCutOptionsDialog.dismiss()
            Toast.makeText(this,"dialog not to do cut options",Toast.LENGTH_SHORT).show()
        }
        showAutoCutOptionsDialog.show()
    }
    fun getRotateOrNot() : Int {
        val preferences = getSharedPreferences("settings", MODE_PRIVATE)
        val doNotShowAgain = preferences.getBoolean("doNotShowAgain", false)
        val rotate = preferences.getBoolean("rotate", false)
        if (!doNotShowAgain) {
            // 沒有讀到設定或使用者未選擇「不再顯示」，所以顯示對話框
            return -1
        } else {
            if (rotate) {
                return 1
            } else {
                return 0
            }
        }

    }
    fun getCutOptionsOrNot() : Int {
        val preferences = getSharedPreferences("settings", MODE_PRIVATE)
        val doNotShowAgainForCutOptions = preferences.getBoolean("doNotShowAgainForCutOptions", false)
        val rotate = preferences.getBoolean("cut_options", false)
        if (!doNotShowAgainForCutOptions) {
            // 沒有讀到設定或使用者未選擇「不再顯示」，所以顯示對話框
            return -1
        } else {
            if (rotate) {
                return 1
            } else {
                return 0
            }
        }

    }
    fun autoOcrAndRotate(base64String : String,flag: Int,onSuccess1: (String) -> Unit, onFailure1: (String) -> Unit){

        if(flag == 1){

            showProgressDialog("重新掃描中")
            ConstantsScanServiceFunction.scanBase64ToOcrText(base64String, this@BaseActivity,1, onSuccess = { it1 ->
                ConstantsOcrResults.getOcrResult()[ConstantsOcrResults.rescanPosition].description = it1
                hideProgressDialog()
                onSuccess1("success")
            }, onFailure = { it1 ->
                hideProgressDialog()
                showErrorScan()
                onFailure1("error")
            })
        }else{

            if(getRotateOrNot() == -1){

                showAutoRotateDialog(base64String)
                onSuccess1("success")
            }
            else if(getRotateOrNot() == 1){

                showProgressDialog("目前正在處理OCR之結果")
                ConstantsHoughAlgo.imageRotate(
                    base64String,this@BaseActivity,
                    onSuccess = { it1 ->
                        ConstantsScanServiceFunction.scanBase64ToOcrText(it1, this@BaseActivity,1, onSuccess = { it1 ->
                            processScan(it1)
                            onSuccess1("success")
                            hideProgressDialog()
                        }, onFailure = { it1 ->
                            showErrorScan()
                            onFailure1("error")
                            hideProgressDialog()
                        })
                    }, onFailure = { it1 ->
                        ConstantsScanServiceFunction.scanBase64ToOcrText(base64String, this@BaseActivity,1, onSuccess = { it1 ->

                            processScan(it1)
                            onSuccess1("success")
                            hideProgressDialog()
                        }, onFailure = { it1 ->
                            onFailure1("error")
                            hideProgressDialog()
                        })
                    } )
            }else{
                showProgressDialog("目前正在處理OCR之結果")
                ConstantsScanServiceFunction.scanBase64ToOcrText(base64String!!, this@BaseActivity,1, onSuccess = { it1 -> hideProgressDialog() }, onFailure = { it1 -> hideProgressDialog()})
            }
        }

    }

    fun autoCutOptions():ArrayList<String>{
        var cut_options : ArrayList<String> = ArrayList()
        if(getCutOptionsOrNot() == -1){
            showAutoCutOptionsDialog()
        }
        else if(getCutOptionsOrNot() == 1){
            Toast.makeText(this,"setting to do cut options",Toast.LENGTH_SHORT).show()

        }else{
            Toast.makeText(this,"setting not to do cut options",Toast.LENGTH_SHORT).show()
        }
        return cut_options
    }

    fun processScan(it1 : String){
        ConstantsOcrResults.setOcrResult(it1)
        val result_auto_cut_choice = splitQuestionOptions(it1)
        Log.e("choice", result_auto_cut_choice.toString())
        hideProgressDialog()
        val intent = Intent(this, ScannerTextWorkSpaceActivity::class.java)
        intent.putExtra("ocrText", it1)
        val bundle = Bundle().apply {
            putString("first", result_auto_cut_choice?.first)
            putStringArrayList("second", ArrayList(result_auto_cut_choice?.second))
        }
        intent.putExtra("ocrText", it1)
        intent.putExtra("options", bundle)
        startActivity(intent)
        finish()
    }

    fun showErrorScan(){
        showErrorSnackBar("辨識不出來目前的圖片請重新上傳")
        hideProgressDialog()
    }

}