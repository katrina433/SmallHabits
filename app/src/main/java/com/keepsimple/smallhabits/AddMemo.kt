package com.keepsimple.smallhabits

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.add_memo.*

/**
 * The activity to add/edit/delete a memo.
 * A memo can be in the form of an image and/or text.
 * A user can add up to one memo for each task each day.
 */
class AddMemo : AppCompatActivity() {
    private var resultCode = Activity.RESULT_CANCELED
    private var resultIntent = Intent()

    private lateinit var photoPath: String
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val taskManagementDB = getSharedPreferences(
            resources.getString(R.string.task_management_database), Context.MODE_PRIVATE)
        val themeName = taskManagementDB.getString(resources.getString(R.string.theme_key), "BrownTheme")
        val themeId =
            try {
                resources.getIdentifier(themeName, "style", packageName)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                0
            }
        setTheme(themeId)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_memo)

        val request = intent.getIntExtra(resources.getString(R.string.request), resources.getInteger(R.integer.add_memo_request_code))
        if (request == resources.getInteger(R.integer.edit_memo_request_code)) {
            val memoString = intent.getStringExtra(resources.getString(R.string.memo_text))
            val memoImage = intent.getStringExtra(resources.getString(R.string.memo_image))
            displayCurrentMemo(memoString!!, memoImage!!)
        }

        cameraButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
                getCameraPermission()
            } else {
                takePicture()
            }
        }

        galleryButton.setOnClickListener {
            openGallery()
        }

        confirmMemo.setOnClickListener {
            val memoText = memoEditText.text.toString()
            if (!memoText.isBlank() || imageUri != null) {
                val imageUriString = if (imageUri == null) {
                    resources.getString(R.string.empty_string)
                } else {
                    imageUri.toString()
                }
                resultIntent.putExtra(resources.getString(R.string.memo_text), memoText)
                resultIntent.putExtra(resources.getString(R.string.memo_image), imageUriString)
                resultCode = resources.getInteger(R.integer.memo_added_result_code)
                setResult(resultCode, resultIntent)
                finish()
            } else {
                Toast.makeText(this, resources.getString(R.string.memo_input_error), Toast.LENGTH_SHORT).show()
            }
        }

        deleteMemo.setOnClickListener {
            resultCode = resources.getInteger(R.integer.memo_deleted_result_code)
            setResult(resultCode, resultIntent)
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            resources.getInteger(R.integer.camera_permission_request_code) -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture()
                } else {
                    Toast.makeText(this, resources.getString(R.string.no_camera_permission), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                resources.getInteger(R.integer.open_camera_request_code) -> {
                    try {
                        memoImageView.setImageURI(imageUri)
                        memoImageView.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                resources.getInteger(R.integer.open_gallery_request_code) -> {
                    try {
                        imageUri = data!!.data!!
                        memoImageView.setImageURI(imageUri)
                        memoImageView.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun displayCurrentMemo(memoString: String, memoImage: String) {
        if (memoString != resources.getString(R.string.empty_string)) {
            memoEditText.setText(memoString)
        }
        if (memoImage != resources.getString(R.string.empty_string)) {
            imageUri = Uri.parse(memoImage)
            try {
                memoImageView.setImageURI(imageUri)
                memoImageView.visibility = View.VISIBLE
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                imageUri = null
                memoImageView.visibility = View.GONE
            }
        }
    }

    private fun getCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            Array<String>(1) {Manifest.permission.CAMERA},
            resources.getInteger(R.integer.camera_permission_request_code)
        )
    }

   private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_OPEN_DOCUMENT,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI).also {
            it.type = "image/*"
            it.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityForResult(galleryIntent, resources.getInteger(R.integer.open_gallery_request_code))
    }

    private fun takePicture() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).also {
            it.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val name = "${TimeUtil.dateFormat.format(System.currentTimeMillis())}_memo"
        val path = Environment.DIRECTORY_PICTURES

        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, name)
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, path)
        }

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        ActivityCompat.requestPermissions(this, Array<String>(1) {Manifest.permission.CAMERA}, resources.getInteger(R.integer.open_camera_request_code))
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, resources.getInteger(R.integer.open_camera_request_code))
    }

}