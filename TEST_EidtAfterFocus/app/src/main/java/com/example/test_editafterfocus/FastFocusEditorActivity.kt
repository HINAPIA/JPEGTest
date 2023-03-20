package com.example.test_editafterfocus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.ArrayList


class FastFocusEditorActivity : AppCompatActivity() {

    private var previewByteArrayList : ArrayList<ByteArray> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fast_focus_editor)

//        val intent = intent //전달할 데이터를 받을 Intent
        previewByteArrayList = intent.getSerializableExtra("preview") as ArrayList<ByteArray>
    }
}