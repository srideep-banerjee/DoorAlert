package com.example.dooralarm

import com.example.dooralarm.credentials.CommonData
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dooralarm.databinding.MainActivityBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val itemDataList = mutableListOf<ItemData>()
    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        try {
            Class.forName("credentials.CommonData")
        } catch (e: ClassNotFoundException) {
            Toast.makeText(applicationContext, "Credentials file not found", Toast.LENGTH_LONG)
                .show()
            finish()
            return
        }

        val databaseDeferred= lifecycleScope.async {Firebase.database(CommonData.databaseURL)}

        val sensorStateFile = File("${filesDir}\\sensor state.txt")
        if (sensorStateFile.exists()) {
            val state = sensorStateFile.bufferedReader().use { it.readLine() }
            binding.switch1.isChecked = state.equals("true")
        } else sensorStateFile.createNewFile()

        val logsFile = File("${filesDir}/logs.txt")
        if (logsFile.exists()) {
            binding.loadingTV.text = "Loading old logs"

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
                    binding.loadingTV.text = "Connecting to Server"
                }

                val database =databaseDeferred.await()

                val myRef = database.reference
                myRef.child("Obstacles Detected")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val value = dataSnapshot.getValue(String::class.java)
                            value?.let {
                                if (it == "true") {
                                    runOnUiThread {
                                        binding.loadingTV.text = "Loading new logs"
                                    }
                                    getLogsFromDatabase(database)
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            runOnUiThread {
                                Toast.makeText(
                                    applicationContext,
                                    "Unable to fetch data",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                                finish()
                            }
                        }
                    })
            }
        } else logsFile.createNewFile()
        
        lifecycleScope.launch { 
            val database=databaseDeferred.await()

        }

    }

    fun getLogsFromDatabase(database: FirebaseDatabase) {
        val myRef = database.reference.child("/Logs")
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lifecycleScope.launch(Dispatchers.IO) {
                    var ind = 1
                    val file = File(filesDir.path + "/logs.txt")
                    if (!file.exists()) file.createNewFile()
                    while (snapshot.hasChildren()) {
                        val childSnap = snapshot.child("log ${ind++}")
                        childSnap.getValue<String>()?.let {
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
                            file.appendText("$it\n")
                        }
                        childSnap.ref.removeValue()
                    }
                    withContext(Dispatchers.Main) {
                        binding.recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
                        binding.recyclerView.adapter = CustomAdapter(itemDataList)
                        binding.loadingScreen.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Unable to fetch data", Toast.LENGTH_LONG)
                        .show()
                    finish()
                }
            }

        })
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