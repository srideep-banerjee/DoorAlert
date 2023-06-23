package com.example.dooralarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val notificationChannelID = "dooralert_notif_id"
    private lateinit var loadingScreen: View
    private lateinit var recyclerView: RecyclerView
    private val itemDataList = mutableListOf<ItemData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        loadingScreen = findViewById(R.id.loadingScreen)
        recyclerView = findViewById(R.id.recyclerView)


        val logsFile = File("${filesDir}\\logs.txt")
        if (logsFile.exists()) {
            lifecycleScope.launch(Dispatchers.IO) {
                logsFile.forEachLine {
                    val deviceID = it.split("|")[0]
                    val timestamp = it.split(" ")[1]
                    val date = timestamp.split(",")[0]
                    val time = timestamp.split(",")[1]
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName: CharSequence = "DoorAlert"
            val channelDescription = "Notifications for when movement is detected"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel =
                NotificationChannel(notificationChannelID, channelName, importance)
            notificationChannel.description = channelDescription
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, notificationChannelID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_launcher_foreground
                    )
                )
                .setContentTitle("Notification Title")
                .setContentText("Notification Content")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setFullScreenIntent(pendingIntent, true)
                .setOngoing(true)

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(1, builder.build())

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