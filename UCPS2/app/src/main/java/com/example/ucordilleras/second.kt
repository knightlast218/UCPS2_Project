package com.example.ucordilleras

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.imageclassificationdemo.R

class second : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val button = findViewById<Button>(R.id.mCameraButton)
        button.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)

            startActivity(intent)

            val button2 = findViewById<Button>(R.id.mBack)
            button2.setOnClickListener(){
                onBackPressed()
            }


        }
    }
}