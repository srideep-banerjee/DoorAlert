package com.example.dooralarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.main_activity.loadingScreen
import kotlinx.android.synthetic.main.main_activity.recyclerView
import kotlinx.android.synthetic.main.main_activity.switch1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val itemDataList = mutableListOf<ItemData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)


        val logsFile = File("${filesDir}/logs.txt")
        if (logsFile.exists()) {
            lifecycleScope.launch(Dispatchers.IO) {
                logsFile.forEachLine {
                    val logData = it.split(",")
                    val deviceID = logData[0]
                    val date = logData[1]
                    val time = logData[2]
                    val itemData =
                        ItemData(
                            "Movement detected by device $deviceID",
                            "${getProcessedDate(date)},$time"
                        )
                    itemDataList.add(itemData)
                }
                withContext(Dispatchers.Main) {
                    recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
                    recyclerView.adapter = CustomAdapter(itemDataList)
                    loadingScreen.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        } else logsFile.createNewFile()

        val sensorStateFile = File("${filesDir}\\sensor state.txt")
        if (sensorStateFile.exists()) {
            val state = sensorStateFile.bufferedReader().use { it.readLine() }
            switch1.isChecked = state.equals("true")
        } else sensorStateFile.createNewFile()

    }

    override fun onDestroy() {
        println("Desssssstttttttttttroyyyyyyyyyyyyyd")
        super.onDestroy()
    }

    override fun onPause() {
        println("PAUSED PAUSED PAUSED PAUSED PAUSED PAUSED PAUSED")
        super.onPause()
    }

    private fun getProcessedDate(inputDateString: String): String {
        val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val date: Date = format.parse(inputDateString)
        val currentDate = Calendar.getInstance()
        currentDate.timeInMillis = System.currentTimeMillis()
        val compareDate = Calendar.getInstance()
        compareDate.time = date
        val isToday = (currentDate.get(Calendar.YEAR) == compareDate.get(Calendar.YEAR)
                && currentDate.get(Calendar.DAY_OF_YEAR) == compareDate.get(Calendar.DAY_OF_YEAR))
        currentDate.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = (currentDate.get(Calendar.YEAR) == compareDate.get(Calendar.YEAR)
                && currentDate.get(Calendar.DAY_OF_YEAR) == compareDate.get(Calendar.DAY_OF_YEAR))
        val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        val year = yearFormat.format(date)
        return if (isToday) {
            "Today"
        } else if (isYesterday) {
            "Yesterday"
        } else {
            val formattedDate = outputFormat.format(date)
            if (year == yearFormat.format(currentDate.time)) {
                formattedDate
            } else {
                "$formattedDate $year"
            }
        }
    }

}