package com.example.test_objectremove

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Thread {
            API() // network 동작, 인터넷에서 xml을 받아오는 코드
        }.start()
    }

    private fun API() {
        var YOUR_API_KEY =
            "e0fd90801f129d924e869d612e7a55b3b619c9334362ef938f5aee946434949f8956c078afe082e8075773c47ed33ce4"

        val photo = "/data/data/com.example.test_objectremove/photo.jpg"
        val mask = "/data/data/com.example.test_objectremove/mask.png"
        // this example uses the OkHttp library
        // https://square.github.io/okhttp/
        val client = OkHttpClient()

        val requestBody =
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image_file",
                    "image.jpg",
                    File(photo).asRequestBody("image/jpeg".toMediaType())
                )
                .addFormDataPart(
                    "mask_file",
                    "mask.png",
                    File(mask).asRequestBody("image/png".toMediaType())
                )
                .build()

        val request =
            Request.Builder()
                .header("x-api-key", YOUR_API_KEY)
                .url("https://clipdrop-api.co/cleanup/v1")
                .post(requestBody)
                .build()

        val imageview : ImageView = findViewById(R.id.imageView)

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val bitmap = byteArrayToBitmap(response.body!!.bytes())
            runOnUiThread {
                imageview.setImageBitmap(bitmap)
            }
            // response.body().bytes() here is a byte array of the returned image
        }
    }
    fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
        return bitmap
    }

}