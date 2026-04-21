package com.example.semestra

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class UploadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.upload_title)
    }
}
