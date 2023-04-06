package com.example.test_editafterfocus

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.test_editafterfocus.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var cameraFragment : CameraFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding!!.root)
        cameraFragment = CameraFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment, cameraFragment!!)
            .commit()
    }
}