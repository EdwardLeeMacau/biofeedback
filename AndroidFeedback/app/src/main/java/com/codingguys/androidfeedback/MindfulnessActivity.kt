package codingguys.Biofeedback

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.TextView
import com.codingguys.androidfeedback.R
import java.lang.Exception
import java.util.logging.Logger

class MindfulnessActivity : AppCompatActivity() {
    // Constant setting
    private var maxHeartrate: Int = Int.MIN_VALUE
    private var minHeartrate: Int = Int.MAX_VALUE
    private var stableTemperature: Float = 0f
    private val stableTempCondition = 3
    private var stableTempCount = 0
    lateinit var mode: String
    var outstring = ""

    // UI setting
    lateinit var debugText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mindfulness)

        supportActionBar?.title = "Mindfulness"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Constant setting
        val intent: Intent = getIntent()
        mode = intent.getStringExtra("Mode")
        debugText = findViewById(R.id.mindfulness_showbluetooth)
        debugText.maxLines = Int.MAX_VALUE

        handlerReceive(bluetoothRunnable)
        handlerReceive(baselineRunnable)
    }

    // UI, Execute every updateperiod
    private fun getBaseline(pack: String){
        val (page: Int?, bioSignal: Triple<Int, Int, Float>) = extractPackage(pack)
        val hr = bioSignal.second
        val temp = bioSignal.third

        if (hr > maxHeartrate){
            maxHeartrate = hr
        }
        if (hr < minHeartrate && hr >0) {
            minHeartrate = hr
        }
        if (temp < stableTemperature + 0.2 && temp>stableTemperature+0.2){
            stableTempCount = 0
            stableTemperature = temp
        }
        else{
            stableTempCount = stableTempCount+1
        }
    }

    // UI, Execute every updateperiod
    private fun showBaseline(textView: TextView, others: String = "") {
        outstring = (maxHeartrate.toString() + ", " +  minHeartrate.toString() + ", " + stableTemperature.toString() + ", " + stableTempCount.toString()+  "\n" + others)
        when(mode){
            "HeartRate" -> {
                if((maxHeartrate - minHeartrate) > 10){
                    outstring = outstring + " Welcome to HeartRate Chanllenge !!"
                }
            }
            "Temperature" -> {
                if (stableTempCount > stableTempCondition) {
                    outstring = outstring + " Welcome to Temp Chanllenge !!"
                }
            }
        }
        debugText.text = outstring
    }

    // Call by pressSkipButton only
    private fun skip() {
        when (mode) {
            "HeartRate" -> {
                // Kill the timer when go to the next page
                handlerClose(taskHandler, baselineRunnable)
                handlerClose(taskHandler, bluetoothRunnable)
                val intent = Intent(this, HeartrateActivity::class.java)
                intent.putExtra("maxHeartrate", maxHeartrate)
                intent.putExtra("minHeartrate", minHeartrate)
                intent.putExtra("stableTemperature", stableTemperature)
                startActivity(intent)
            }
            "Temperature" -> {
                // Kill the timer when go to the next page
                handlerClose(taskHandler, baselineRunnable)
                handlerClose(taskHandler, bluetoothRunnable)
                val intent = Intent(this, TemperatureActivity::class.java)
                intent.putExtra("maxHeartrate", maxHeartrate)
                intent.putExtra("minHeartrate", minHeartrate)
                intent.putExtra("stableTemperature", stableTemperature)
                startActivity(intent)
            }
            else -> {}
        }
    }

    fun pressSkipButton(view: View){
        skip()
    }

    private var baselineRunnable = object: Runnable {
        override fun run() {
            if (packages.size > 0){
                val pack = packages.get(0)
                packages.removeAt(0)

                // TODO: find the reason that error occurs here
                if (pack != "") {
                    try {
                        getBaseline(pack)
                        showBaseline(debugText, pack)
                    } catch (e: Exception) {
                        debugText.text = e.toString()
                    }
                }
            }
            taskHandler.postDelayed(this, updatePeriod)
        }
    }

    // Hard copy from MainActivity, not the public function
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

}