package com.faithdeveloper.giveaway

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.faithdeveloper.giveaway.data.Repository
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var repository: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth = FirebaseAuth.getInstance()
        repository = Repository(auth, this)
    }

    fun getRepository() = repository
}