package com.chiclaim.android.retrofit_sample

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_file_load_activity.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File


/*

    https://futurestud.io/tutorials/retrofit-2-how-to-upload-files-to-server

 */
class FileUploadActivity : BaseActivity() {

    companion object {
        const val PICK_FILE_RESULT_CODE = 1
        const val MAX_FILE_NUMBER = 1

        // set your base url
        const val API_URL = "http://10.1.81.240:8080/"

    }

    private val files = hashMapOf<String, Uri>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_load_activity)

        btn_choose_file.setOnClickListener {
            startChooseFile()
        }


        btn_file_upload.setOnClickListener {
            if (files.isEmpty()) {
                Toast.makeText(this, "请选择文件", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            files.forEach {
                Log.e("FileUploadActivity", it.value.path)
                uploadFile(files[it.key]!!)
            }
        }
    }

    private fun startChooseFile() {
        var chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        chooseFile.type = "*/*"
        chooseFile = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(chooseFile, PICK_FILE_RESULT_CODE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        when (requestCode) {
            PICK_FILE_RESULT_CODE -> {
                val fileUri = data?.data
                if (fileUri == null) {
                    Toast.makeText(this, "fileUri is null", Toast.LENGTH_SHORT).show()
                    return
                }
                val filePath = fileUri.path
                filePath?.let {
                    if (files.size >= MAX_FILE_NUMBER) {
                        Toast.makeText(this, "最多只能上传 $MAX_FILE_NUMBER 个文件", Toast.LENGTH_SHORT).show()
                        return
                    }
                    if (!files.contains(filePath)) {
                        files[filePath] = fileUri
                        text_file_path.append("\n")
                        text_file_path.append(filePath)
                    } else {
                        Toast.makeText(this, "文件已经存在", Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }
    }

    private fun getMimeType(uri: Uri) = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        contentResolver.getType(uri)
    } else {
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase())
    }


    private fun uploadFile(fileUri: Uri) {

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service: FileUploadService = retrofit.create(FileUploadService::class.java)


        val file = File(fileUri.path)

        val mineType = getMimeType(fileUri) ?: return

        val fileRequestBody = RequestBody.create(MediaType.parse(mineType), file)

        // 相当于 HTML 页面里面的 文件选择控件： <input type="file" name="file_1"/>
        val filePart = MultipartBody.Part.createFormData("file_1", file.name, fileRequestBody)

        // 创建 form 字段，相当于 HTML 页面里面的 <input type="text" name="description"/>
        val formFieldPart = RequestBody.create(MultipartBody.FORM, "这是文件描述")

        val call = service.upload(formFieldPart, filePart)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                Toast.makeText(applicationContext, "upload success", Toast.LENGTH_SHORT).show()

                fileUri.path?.let {
                    files.remove(it)
                    text_file_path.setText(R.string.file_upload_prefix)
                }
            }

            override fun onFailure(call: Call<ResponseBody?>?, t: Throwable) {
                Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }


    interface FileUploadService {
        @Multipart
        @POST("fileUpload")
        fun upload(
            @Part("description") description: RequestBody,
            @Part file: MultipartBody.Part
        ): Call<ResponseBody>
    }


}