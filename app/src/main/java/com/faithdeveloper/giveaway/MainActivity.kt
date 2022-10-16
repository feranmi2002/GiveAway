package com.faithdeveloper.giveaway

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.fragment.NavHostFragment
import com.faithdeveloper.giveaway.data.Repository
import com.faithdeveloper.giveaway.utils.Extensions.getUserDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var repository: Repository
    private var keepSplashScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().apply {
            setKeepOnScreenCondition { keepSplashScreen }
        }
        setContentView(R.layout.activity_main)
        setUpApp()

    }

    private fun setUpApp() {
        //        db = Room.databaseBuilder(applicationContext, Database::class.java,  "${getString(R.string.app_name)} database").allowMainThreadQueries().build()
        FirebaseStorage.getInstance().maxUploadRetryTimeMillis = 60000
        auth = FirebaseAuth.getInstance()
        repository = Repository(auth, this@MainActivity.applicationContext)
        setNavigationGraph()

    }

    fun getRepository() = repository
    private fun setNavigationGraph() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
        val navController = navHostFragment.navController


        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        val homeDestination = if (intent.data != null) {
            // user is directed from the web either after email verification or change of password
//                findNavController().navigate(SplashScreenDirections.actionSplashScreenToSignIn())
            R.id.signIn
        } else if (repository.checkUserRegistration() == null) {
            // no user is registered on the device
//                findNavController().navigate(SplashScreenDirections.actionSplashScreenToSignUp())
            R.id.signUp
        } else if (repository.checkUserRegistration() != null && getUserDetails()[0] == null) {
            /*there is already a registered user on the app but the user has signed out. When a
            user is signed out, his personal details are deleted but the firebase cached data about the user still remains on the device. So,
            firebase still recognizes that there is a user is on the device, but the personal details have been removed from the device,
            the user has to sign in again so that the personal details are restored.
            * */
//                findNavController().navigate(SplashScreenDirections.actionSplashScreenToSignIn())
            R.id.signIn
        } else {
            // user email hasn't been verified
            if (!repository.emailIsVerified()!!)
                R.id.signIn
            else
            // users is fully registered on the app
                R.id.feed
        }
        navGraph.setStartDestination(homeDestination)
        navController.graph = navGraph
        keepSplashScreen = false
    }
}