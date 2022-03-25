package codingguys.Biofeedback

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView

class MindfulnessActivity : AppCompatActivity() {
    private var maxHeartrate: Int = 0
    private var minHeartrate: Int = 0
    private var maxTemperature: Float = 0f

    lateinit var debugText: TextView
    lateinit var mode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mindfulness)

        setSupportActionBar(findViewById(R.id.toolbar_mindfulness))
        supportActionBar?.title = "Mindfulness"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val intent: Intent = getIntent()
        mode = intent.getStringExtra("Mode")

        debugText = findViewById(R.id.mindfulness_showbluetooth)
    }

    // TODO: Update the function to runnable
    private fun showBluetoothMsg(bluetoothMsg: CharArray) {
        val result = maxHeartrate.toString() + ", " +  minHeartrate.toString() + ", " + maxTemperature.toString()
        debugText.text = result
    }

    private fun skip() {
        when (mode) {
            "HeartRate" -> {
                val intent = Intent(this, HeartrateActivity::class.java)
                intent.putExtra("maxHeartrate", maxHeartrate)
                intent.putExtra("minHeartrate", minHeartrate)
                intent.putExtra("maxTemperature", maxTemperature)
                startActivity(intent)
            }
            "Temperature" -> {
                val intent = Intent(this, TemperatureActivity::class.java)
                intent.putExtra("maxHeartrate", maxHeartrate)
                intent.putExtra("minHeartrate", minHeartrate)
                intent.putExtra("maxTemperature", maxTemperature)
                startActivity(intent)
            }
            else -> {}
        }
    }

    fun pressSkipButton(view: View){
        skip()
    }

    // TODO: Make a runnable function that refresh the bioSignal and the TextView(show the baseline)
    private var runnable = object: Runnable {
        override fun run() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}