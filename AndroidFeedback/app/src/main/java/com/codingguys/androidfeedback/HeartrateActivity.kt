package codingguys.Biofeedback

import android.app.ActionBar
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.codingguys.androidfeedback.R

class HeartrateActivity : AppCompatActivity() {
    // UI
    private var minSize: Float =  400f
    private var maxSize: Float =  1000f
    lateinit var image: ImageView
    lateinit var KanaParams: ViewGroup.LayoutParams
    lateinit var debugText: TextView
    lateinit var sizeText : TextView
    lateinit var HeartMaxMinText: TextView
    // Constant setting
    private var recordTemp: MutableList<Float> = mutableListOf()
    private var recordCond: MutableList<Int> = mutableListOf()
    private var recordHeartRate: MutableList<Int> = mutableListOf()
    private var maxHeartRate: Int = 0
    private var minHeartRate: Int = 0
    private var mean:Int = 0
    private val BallonScaler = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heartrate)

        supportActionBar?.title = "Heart Rate"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Constant setting
        val intent: Intent = getIntent()
        maxHeartRate = intent.getIntExtra("maxHeartrate", 0)
        minHeartRate = intent.getIntExtra("minHeartrate", 0)
        mean = (maxHeartRate+minHeartRate).div(2)
        // UI
        image = findViewById(R.id.imageView_kanahei)
        KanaParams = image.layoutParams
        debugText = findViewById(R.id.heartrate_debug)
        sizeText = findViewById(R.id.sizeText)
        HeartMaxMinText = findViewById(R.id.HeartMaxMin)
        HeartMaxMinText.text = "Max: " + maxHeartRate.toString() + ", Min: " + minHeartRate.toString()
        // TODO: Finally de-comment this function
        taskHandler.removeCallbacksAndMessages(null)
        handlerReceive(bluetoothRunnable)
        handlerReceive(heartRateRunnable)
    }
////////////////////////////////////////// Main Runnable Function ///////////////////////////////////////
    private var heartRateRunnable = object: Runnable {
        override fun run(){
            debugText.text = "I am not loop"
            if (packages.size > 0){
                val (page: Int?, bioSignal: Triple<Int, Int, Float>) = extractPackage(packages.get(0))
                packages.removeAt(0)
                val cond = bioSignal.first
                val hr = bioSignal.second
                val temp = bioSignal.third

                recordCond.add(cond)
                recordHeartRate.add(hr)
                recordTemp.add(temp)

                debugText.text = ("Cond: " + cond.toString() + ", Heart: " + hr.toString() + ",  Temp: " + temp.toString())
                // take care of the Image Rendering
                sizeManager(image,hr)
            }

            taskHandler.postDelayed(this, updatePeriod)
        }
    }
///////////////////////////////////////////// Scaling Control Function //////////////////////////////
    // Controller of the Ballon Size
    private fun sizeManager(image: ImageView, HR : Int){
        if (HR >0){
            var interval = (HR-mean)
            scalingImage(image,(interval.times(BallonScaler) + minSize).toInt(),(interval.times(BallonScaler) + minSize).toInt())
            sizeText.text = "Interval: " + interval.toString() + ", mean = " + mean.toString()
        }

    }
    private fun scalingImage(image: ImageView, width:Int, height: Int){
            image.layoutParams.height = height
            image.layoutParams.width = width
    }
 /////////////////////////////////////////// BLE Data Reader //////////////////////////////////////////
    // TODO: Runnable
    private var bluetoothRunnable = object: Runnable{
        override fun run() {
            connectToBluetooth = true
            readBluetooth = true
            if (readBluetooth && connectToBluetooth) {
                readStr += readCommand()
                if (readStr.length > 0) {

                    val (newPackages, remain) = unpack(readStr, ";")
                    packages.addAll(newPackages)
                    readStr = remain
                }
            }
            taskHandler.postDelayed(this, updatePeriod)
        }
    }
////////////////////////////////////////// UI Stuff /////////////////////////////////////////////////
    // UI
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // UI
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
            else -> {}
        }

        return true
    }

//////////////////////////////////// Leave the current Page ////////////////////////////////////////
    fun skipHeartRate(view: View) {
        // Kill the timer when go to the next page
        handlerClose(taskHandler, heartRateRunnable)
        handlerClose(taskHandler, bluetoothRunnable)
        val intent = Intent(this, RecordActivity::class.java)
        intent.putExtra("QueryRecord", "JustNow")
        intent.putExtra("recordTemp", recordTemp.toFloatArray())
        intent.putExtra("recordCond", recordCond.toIntArray())
        intent.putExtra("recordHeartRate", recordHeartRate.toIntArray())
        startActivity(intent)
        finish()
    }
}