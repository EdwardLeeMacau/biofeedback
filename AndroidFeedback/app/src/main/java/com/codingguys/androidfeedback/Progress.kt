package com.codingguys.androidfeedback

import android.os.AsyncTask
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.TextView
import codingguys.Biofeedback.*
import kotlinx.android.synthetic.main.activity_main.*

class Progress: AsyncTask<Int, Int, Int>() {
    lateinit var textBox: TextView
    override fun doInBackground(vararg params: Int?): Int {
       readStr = readCommand()
        Log.d("background","!")
        return 0
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        Log.d("background","...")
    }
    override fun onPostExecute(result: Int?) {
        super.onPostExecute(result)
        Log.d("background","end")
        //textBox.text = readStr
    }
}