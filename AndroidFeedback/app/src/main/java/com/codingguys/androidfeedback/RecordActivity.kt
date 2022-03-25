package codingguys.Biofeedback

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.support.v7.widget.PopupMenu
import android.widget.*
import com.codingguys.androidfeedback.R
import kotlin.math.ceil
import kotlin.math.floor

class RecordActivity : AppCompatActivity(){
    // UI drawing and interaction
    private lateinit var button: Button
    private lateinit var imageView: ImageView
    private lateinit var debugText: TextView

    // Data Setting
    private var width: Float = 0f
    private var height: Float = 0f
    private lateinit var recordTemp: FloatArray
    private lateinit var recordCond: IntArray
    private lateinit var recordHeartRate: IntArray
    private lateinit var xArray: FloatArray

    // Paint and Canva Setting
    private var canvas: Canvas? = null
    private lateinit var bitmap: Bitmap
    private var paint: Paint = Paint()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        supportActionBar?.title = "Record"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Constance setting
        imageView = findViewById(R.id.imageView_canvas)
        button = findViewById(R.id.button_biosignal)
        debugText = findViewById(R.id.record_debug)

        val intent: Intent = getIntent()
        recordTemp = intent.getFloatArrayExtra("recordTemp")
        recordCond = intent.getIntArrayExtra("recordCond")
        recordHeartRate = intent.getIntArrayExtra("recordHeartRate")

        // Initialized xArray
        for (i in 0..(recordTemp.size - 1)){
            xArray[i] = width / recordTemp.size
        }

        paint.strokeWidth = 5f
        paint.textSize = 36f
        paint.color = Color.GREEN
    }

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
                } else {
                    item.setChecked(true)
                }
            }
            R.id.disconnectBluetooth -> {   // ToolBar Menu controlling bluetooth
                connectToBluetooth = false
                readBluetooth = false
                // bleThread?.cancel()
            }
            R.id.connectBluetooth -> { }
            else -> {}
        }
        return true
    }

    // UI
    fun chooseBioSignal(view: View){
        val menu = PopupMenu(this, view)
        width = imageView.width.toFloat()
        height = imageView.height.toFloat()

        bitmap = Bitmap.createBitmap(imageView.width, imageView.height, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)

        menu.inflate(R.menu.menu_biosignal)
        menu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->
            when (item!!.itemId) {
                R.id.heartrate -> {
                    button.text = "HEARTRATE"
                    canvas = drawGrid(canvas, paint)
                    canvas = drawCurve(xArray, recordHeartRate)
                }
                R.id.temperature -> {
                    button.text = "TEMPERATURE"
                    canvas = drawGrid(canvas, paint)
                    canvas = drawCurve(xArray, recordTemp)
                }
                R.id.conductance -> {
                    button.text = "CONDUCTANCE"
                    canvas = drawGrid(canvas, paint)
                    canvas = drawCurve(xArray, recordCond)
                }
                else -> {}
            }
            imageView.setImageBitmap(bitmap)
            true
        })
        menu.show()
    }

    // Logic
    private fun percent2pixel(percent: Float, maximum: Float): Float {
        return percent * maximum / 100
    }

    // Logic
    private fun reverseTopBotton(value: Float, minimum: Float, maximum: Float, range: Float): Float{
        return (value - minimum) / (maximum - minimum) * range
    }

    // Logic
    private fun reverseTopBotton(value: Int, minimum: Float, maximum: Float, range: Float): Float{
        return (value - minimum.toInt()) / (maximum - minimum) * range
    }

    //  UI
    private fun drawGrid(c: Canvas? = canvas, p: Paint = paint): Canvas {
        c!!.drawColor(Color.BLACK)
        c.drawLine(percent2pixel(5f, width), 0f, percent2pixel(5f, width), height, p)
        c.drawLine(0f, percent2pixel(90f, height), width, percent2pixel(90f, height), p)
        c.drawText("0", percent2pixel(6f, width), percent2pixel(96f, height), p)

        return c
    }

    // Not Implemented
    private fun drawGrid(canvas: Canvas): Canvas {
        canvas.drawColor(Color.BLACK)
        canvas.drawLine(percent2pixel(5f, width), 0f, percent2pixel(5f, width), height, paint)
        canvas.drawLine(0f, percent2pixel(90f, height), width, percent2pixel(90f, height), paint)
        canvas.drawText("0", percent2pixel(2f, width), percent2pixel(95f, height), paint)

        return canvas
    }

    // Polymorphism
    private fun drawCurve(x: FloatArray, y: IntArray, c: Canvas? = canvas, p: Paint? = paint): Canvas{
        val numItems = x.size
        val maxY = ceil(y.max()!!.toFloat())
        val minY = floor(y.min()!!.toFloat())
        var points: FloatArray = floatArrayOf()

        for (i in 0..(numItems - 1)) {
            if ((i != 0) and (i != (numItems - 1))) {
                points += x[i]
                points += (reverseTopBotton(y[i], minY, maxY, percent2pixel(90f, height)))
                points += x[i]
                points += (reverseTopBotton(y[i], minY, maxY, percent2pixel(90f, height)))
            } else {
                points += x[i]
                points += (reverseTopBotton(y[i], minY, maxY, percent2pixel(90f, height)))
            }
        }

        c!!.drawLines(points, p)
        c.drawText(maxY.toString(), percent2pixel(0.5f, width), percent2pixel(6f, height), p)
        c.drawText(minY.toString(), percent2pixel(0.5f, width), percent2pixel(87f, height), p)

        return c
    }

    // Polymorphism
    private fun drawCurve(x: FloatArray, y: FloatArray, c: Canvas? = canvas, p: Paint? = paint): Canvas{
        val numItems = x.size
        val maxY = ceil(y.max()!!.toFloat())
        val minY = floor(y.min()!!.toFloat())
        var points: FloatArray = floatArrayOf()

        for (i in 0..(numItems - 1)) {
            if ((i != 0) and (i != (numItems - 1))) {
                points += x[i]
                points += (reverseTopBotton(y[i], minY, maxY, percent2pixel(90f, height)))
                points += x[i]
                points += (reverseTopBotton(y[i], minY, maxY, percent2pixel(90f, height)))
            } else {
                points += x[i]
                points += (reverseTopBotton(y[i], minY, maxY, percent2pixel(90f, height)))
            }
        }

        c!!.drawLines(points, p)
        c.drawText(maxY.toString(), percent2pixel(0.5f, width), percent2pixel(6f, height), p)
        c.drawText(minY.toString(), percent2pixel(0.5f, width), percent2pixel(87f, height), p)

        return c
    }

    // TODO: If need to query the previous training data
    fun queryBluetooth(view: View) {}
}

