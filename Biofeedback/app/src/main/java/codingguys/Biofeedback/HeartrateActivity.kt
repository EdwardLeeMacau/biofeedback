package codingguys.Biofeedback

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView

class HeartrateActivity : AppCompatActivity() {
    lateinit var image: ImageView

    private var recordTemp: List<Float> = listOf()
    private var recordCond: List<Float> = listOf()
    private var recordHeartRate: List<Float> = listOf()

    private var maxHeartRate: Int = 0
    private var minHeartRate: Int = 0
    private var minSize: Float = 0.75f
    private var maxSize: Float = 1.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heartrate)

        setSupportActionBar(findViewById(R.id.toolbar_heartrate))
        supportActionBar?.title = "Heart Rate"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val passingIntent = intent
        maxHeartRate = passingIntent.getIntExtra("maxHeartrate", 0)
        minHeartRate = passingIntent.getIntExtra("minHeartrate", 0)

        image = findViewById(R.id.imageView_kanahei)

        // TODO: Finally de-comment this function
        // handlerReceive(runnable)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // TODO: Copy the complete function from MainActivity.kt
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.debug -> {
                if (item.isChecked) {
                    item.setChecked(false)
                }
                else {
                    item.setChecked(true)
                }
            }
            R.id.disconnectBluetooth -> { }
        }

        return true
    }

    // TODO: Extend this function by the function activeScaling
    private fun activationFunc(heartRate: Int): Float{
        val xRegion: Float = (maxHeartRate - minHeartRate).toFloat() / 3
        val yRegion: Float = (maxSize - minSize) / 4
        val k1: Float = yRegion / xRegion
        val k2: Float = 2f * yRegion / xRegion
        val k0: Float = 0.33f * k1

        return when (heartRate) {
            in 0..minHeartRate -> {
                minSize + k0 * (heartRate - minHeartRate)
            }
            in minHeartRate..(minHeartRate + xRegion.toInt()) -> {
                minSize + k1 * (heartRate - minHeartRate)
            }
            in (minHeartRate + xRegion.toInt()..minHeartRate + 2 * xRegion.toInt()) -> {
                minSize + yRegion + k2 * (heartRate - minHeartRate)
            }
            in (minHeartRate + 2 * xRegion.toInt()..minHeartRate + 3 * xRegion.toInt()) -> {
                minSize + 2 * yRegion + k1 * (heartRate - minHeartRate)
            }
            else -> {
                maxSize + k0 * (heartRate - minHeartRate)
            }
        }
    }

    private fun scalingImage(image: ImageView, size: Int){
        // Put this function to timer
        // Call readCommand()
        // Call unpack()
        // Call record
        // if not null, call activationFunc() and scaling Image
        // Stop it when exit this activity.
    }

    // TODO: Implement this function, and scaling the ImageView by the returned FloatArray
    private fun activeScaling(start: Float, end: Float, seperate: Int): FloatArray{
        val floatArray: FloatArray = floatArrayOf()
        return floatArray
    }

    fun skip_HeartRate(view: View) {
        val intent = Intent(this, RecordActivity::class.java)
        intent.putExtra("QueryRecord", "JustNow")
        intent.putExtra("recordTemp", recordTemp.toFloatArray())
        intent.putExtra("recordCond", recordCond.toFloatArray())
        intent.putExtra("recordHeartRate", recordHeartRate.toFloatArray())
        startActivity(intent)
    }

    // TODO: Finish this runnable function
    private var runnable = object: Runnable {
        override fun run(){

        }
    }
}