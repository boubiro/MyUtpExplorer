package info.matpif.myutbexplorer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainTVActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_tv)

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
