package com.example.dooralarm

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dooralarm.credentials.CommonData
import com.example.dooralarm.databinding.MainActivityBinding
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Deferred
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
    private lateinit var databaseDeferred: Deferred<FirebaseDatabase>
    private lateinit var sensorStateFile: File
    private lateinit var logsFile: File
    private var logsLoaded = false
    public var destroyed = false
    private val TAG = "My printlines"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (!credentialsExist()) finish()

        sensorStateFile = File("${filesDir}\\sensor state.txt")
        logsFile = File("${filesDir}/logs.txt")

        databaseDeferred = lifecycleScope.async {
            Firebase.database(CommonData.databaseURL)
        }

        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancelAll()

        loadSensorState()
        loadLogs()
        updateToken()

        lifecycleScope.launch(Dispatchers.IO) {
            val database = databaseDeferred.await()
            database.reference.child("Logs")
                .addChildEventListener(initializeChildEventListener())
        }

        binding.switch1.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch(Dispatchers.IO) {
                val database = databaseDeferred.await()
                val myRef = database.reference.child("Sensor Active")
                myRef.setValue(if (isChecked) "true" else "false") { error, _ ->
                    if (error != null) {
                        runOnUiThread {
                            Toast.makeText(
                                applicationContext,
                                "Couldn't change change sensor state",
                                Toast.LENGTH_LONG
                            )
                                .show()
                            binding.switch1.isChecked = !isChecked
                        }
                    } else {
                        if (!sensorStateFile.exists()) sensorStateFile.createNewFile()
                        sensorStateFile.writeText("$isChecked\n")

                    }
                }
            }
        }

    }

    private fun credentialsExist(): Boolean {
        try {
            Class.forName("com.example.dooralarm.credentials.CommonData")
        } catch (e: ClassNotFoundException) {
            Toast.makeText(applicationContext, "Credentials file not found", Toast.LENGTH_LONG)
                .show()
            finish()
            return false
        }
        return true
    }

    private fun initializeChildEventListener() =
        object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (destroyed || !logsLoaded) return
                if (!logsFile.exists()) logsFile.createNewFile()
                snapshot.getValue<String>()?.let {
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
                    logsFile.appendText("$it\n")
                    snapshot.ref.removeValue()
                }
                runOnUiThread {
                    binding.recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
                    binding.recyclerView.adapter = CustomAdapter(itemDataList)
                    Toast.makeText(
                        applicationContext,
                        "New movement detected",
                        Toast.LENGTH_LONG
                    )
                        .show()

                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildRemoved(snapshot: DataSnapshot) {}

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {}

        }

    private fun loadSensorState() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (sensorStateFile.exists()) {
                val state = sensorStateFile.bufferedReader().use { it.readLine() }
                withContext(Dispatchers.Main) {
                    binding.switch1.isChecked = state.equals("true")
                }
            } else {
                sensorStateFile.createNewFile()
                sensorStateFile.writeText("false\n")
            }
        }
    }

    private fun loadLogs() {
        lifecycleScope.launch(Dispatchers.IO) {

            withContext(Dispatchers.Main) {
                binding.loadingTV.text = "Loading previous logs"
            }
            appendPreviousLogs()

            withContext(Dispatchers.Main) {
                binding.loadingTV.text = "Connecting to Server"
            }
            val database = databaseDeferred.await()

            withContext(Dispatchers.Main) {
                binding.loadingTV.text = "Loading new logs"
            }
            appendNewLogs(database)

        }
    }

    private fun appendPreviousLogs() {
        println("Reading logs|||||||||||||||||||||||||||||||||")
        if (logsFile.exists()) {
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
                println(logData)
            }
        } else logsFile.createNewFile()
    }

    private fun appendNewLogs(database: FirebaseDatabase) {

        val myRef = database.reference.child("Logs")
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                println("On data change called")
                lifecycleScope.launch(Dispatchers.IO) {

                    var ind = 1
                    if (!logsFile.exists()) logsFile.createNewFile();
                    for (childSnap in snapshot.children) {
                        // example data {log 1 : "1,26.06.2023,7:53 PM"}
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
                            logsFile.appendText("$it\n")
                        }
                        childSnap.ref.removeValue()
                    }
                    withContext(Dispatchers.Main) {
                        binding.recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
                        binding.recyclerView.adapter = CustomAdapter(itemDataList)
                        binding.loadingScreen.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }
                    logsLoaded = true
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

    private fun updateToken() {
        lifecycleScope.launch(Dispatchers.IO) {
            val database = databaseDeferred.await()
            val myRef = database.reference
            FirebaseMessaging.getInstance().token.addOnCompleteListener {
                if (it.isComplete) {
                    myRef.child("Token")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (!snapshot.exists()) {
                                    snapshot.ref.setValue(it.result.toString())
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}

                        })
                }
            }
        }
    }

    private fun getProcessedDate(inputDateString: String): String {
        val date: Date =
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(inputDateString)!!
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

    override fun onDestroy() {
        destroyed = true
        lifecycleScope.launch(Dispatchers.IO) {
            databaseDeferred.await().goOffline()
        }
        super.onDestroy()
    }

}