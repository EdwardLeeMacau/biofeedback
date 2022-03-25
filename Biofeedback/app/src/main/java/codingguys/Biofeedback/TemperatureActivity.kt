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
import java.lang.Thread.sleep

class TemperatureActivity : AppCompatActivity() {
    lateinit var image: ImageView
    lateinit var handler: Handler
    private var bluetoothMsg: String = ""
    private var maxTemperature: Float = 0f
    private var previousTemp: Float = 0f
    private var recordTemp: List<Float> = listOf()
    private var recordCond: List<Float> = listOf()
    private var recordHeartRate: List<Float> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temperature)

        setSupportActionBar(findViewById(R.id.toolbar_temperature))
        supportActionBar?.title = "Temperature"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val passingIntent = intent
        maxTemperature = passingIntent.getFloatExtra("maxTemperature", 0f)
        previousTemp = maxTemperature

        image = findViewById(R.id.imageView_hotballoon)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

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

            R.id.bluetooth -> { }
            R.id.disconnectBluetooth -> { }
            else -> { }
        }
        return true
    }

    private fun raiseImage(imageView: ImageView, height: Int){
        // Make sure the image can raise to the top in 3mins.
        imageView.top = imageView.top - height
    }

    private fun charArray2Number(charArray: CharArray): Float{
        return charArray.toString().toFloat()
    }

    private fun raiseImageByBluetooth(bluetoothMsg: CharArray, image: ImageView){
        // Put this function to timer
        // Call readCommand()
        // Call unpack()
        // Call Record()
        // if not null, call activationFunc() and scaling Image
        // Stop it when exit this activity.

        val presentTemp = charArray2Number(bluetoothMsg)
        if (previousTemp < presentTemp) raiseImage(image, 1)

        previousTemp = presentTemp
    }

    fun skip_Temperature(view: View) {
        val intent = Intent(this, RecordActivity::class.java)

        intent.putExtra("QueryRecord", "JustNow")
        intent.putExtra("recordTemp", recordTemp.toFloatArray())
        intent.putExtra("recordCond", recordCond.toFloatArray())
        intent.putExtra("recordHeartRate", recordHeartRate.toFloatArray())
        startActivity(intent)
    }

    // TODO: Implement this runnable Function
    private val runnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, 200)
        }
    }
}