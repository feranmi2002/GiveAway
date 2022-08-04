package com.faithdeveloper.giveaway

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.data.room.Database
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var repository: Repository
    private lateinit var db: Database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        db = Room.databaseBuilder(applicationContext, Database::class.java,  "${getString(R.string.app_name)} database").allowMainThreadQueries().build()
        auth = FirebaseAuth.getInstance()
        repository = Repository(auth, this.applicationContext)
    }


    fun getRepository() = repository
}