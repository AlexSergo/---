package com.example.myapplication

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.mapper.MessageMapper.createRadioGranitMessage
import com.example.myapplication.mapper.MessageMapper.mapMessageFromGranit
import com.example.myapplication.mapper.TechnicMapperUI.mapGapToText
import com.example.myapplication.mapper.TechnicTypes
import com.example.myapplication.model.Coordinates
import com.example.myapplication.model.Gap
import com.example.myapplication.model.Technic
import com.example.myapplication.service.LocalService
import com.example.myapplication.service.ServiceCallback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.Calendar
import java.util.Date


class MainActivity : AppCompatActivity(), ServiceCallback {
    lateinit var sendTextButton: Button
    lateinit var sendTechnicButton: Button
    lateinit var sendGapButton: Button
    lateinit var destId: EditText
    lateinit var message: EditText
    lateinit var id: TextView
    var mService: LocalService? = null
    var mBound = false
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sendTextButton = binding.buttonText
        sendTechnicButton = binding.buttonTechnic
        sendGapButton = binding.buttonGap
        id = binding.myId
        id.text = Device.getDeviceId(applicationContext)
        destId = binding.subscriberId
        message = binding.message
        val technic = Technic(
            Coordinates(53.0, 46.0, 100.0),
            1,
            "101",
            TechnicTypes.BMP,
            "Гроза",
            "-",
            false,
            false,
            false,
            false,
            "Цель движется"
        )
        sendTextButton.setOnClickListener(View.OnClickListener {
            val split = binding.ip.text.toString().split(".")
            val destSplit = binding.destIp.text.toString().split(".")
            Device.ip = mutableListOf(split[0].toInt().toByte(), split[1].toInt().toByte(),
                split[2].toInt().toByte(), split[3].toInt().toByte())

            if (message.getText().toString() != "")
                    send(
                        split,
                        destSplit,
                        message.getText().toString()
                    )
            println(message.text.toString())
        })
        sendTechnicButton.setOnClickListener {
 /*           if (destId.getText().toString() != "") send(
                id.getText().toString(),
                destId.getText().toString(),
                mapTechnicToText(technic)
            )*/
        }
        sendGapButton.setOnClickListener {
            val gap = Gap(
                Coordinates(55.0, 44.0, 90.0),
                1,
                "Гроза",
                1,
                false,
                TechnicTypes.GAP,
                "-"
            )
            if (destId.getText().toString() != "") send(
                id.getText().toString(),
                destId.getText().toString(),
                mapGapToText(gap, technic)
            )
        }
        binding.checkbox.setOnClickListener {
            if (binding.checkbox.isChecked){
                val split = binding.ip.text.toString().split(".")
                val destSplit = binding.destIp.text.toString().split(".")
                sendTest(split, destSplit, "55.9876, 43.0912")
            }
        }


        val sharedPref: SharedPreferences = applicationContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE)
        val id = sharedPref.getString("ID", null)
        val mes = sharedPref.getString("MES", null)
        val ip = sharedPref.getString("IP", "")
        val destIP = sharedPref.getString("destIP", "")
        if (id != null)
            destId.setText(id)
        if (mes != null)
            message.setText(mes)
        binding.ip.setText(ip)
        binding.destIp.setText(destIP)
        message.addTextChangedListener {
            sharedPref.let {
                val editor: SharedPreferences.Editor = sharedPref.edit()

                editor.putString("MES", message.text.toString())

                editor.apply()
            }
        }
        destId.addTextChangedListener {
            sharedPref.let {
                val editor: SharedPreferences.Editor = sharedPref.edit()

                editor.putString("ID", destId.text.toString())

                editor.apply()
            }
        }
        binding.ip.addTextChangedListener {
            sharedPref.let {
                val editor: SharedPreferences.Editor = sharedPref.edit()

                editor.putString("IP", binding.ip.text.toString())

                editor.apply()
            }
            try {
                val split = binding.ip.text.toString().split(".")
                if (split.size == 4)
                    Device.ip = mutableListOf(
                        split[0].toInt().toByte(), split[1].toInt().toByte(),
                        split[2].toInt().toByte(), split[3].toInt().toByte()
                    )
            }catch (_: Exception){}
        }
        binding.destIp.addTextChangedListener {
            sharedPref.let {
                val editor: SharedPreferences.Editor = sharedPref.edit()
                editor.putString("destIP", binding.destIp.text.toString())
                editor.apply()
            }
        }
        val split = binding.ip.text.toString().split(".")
        if (split.size == 4)
            Device.ip = mutableListOf(split[0].toInt().toByte(), split[1].toInt().toByte(),
                split[2].toInt().toByte(), split[3].toInt().toByte())
    }

    override fun onStart() {
        super.onStart()
        startService()
        // Bind to LocalService.
    }

    override fun onStop() {
        super.onStop()
        //        unbindService(connection);
//        mBound = false;
    }

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            val binder = service as LocalService.LocalBinder
            mService = binder.service
            mBound = true
            mService!!.setContext(this@MainActivity, this@MainActivity)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    fun startService() {
        if (mService == null) {
            if (!isSerialServiceRunning(LocalService::class.java)) {
                startService(Intent(this, LocalService::class.java))
            }
            val intent = Intent(this, LocalService::class.java)
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
    }

    private fun isSerialServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun disconnected() {
        Toast.makeText(this, "Отключено!", Toast.LENGTH_SHORT).show()
    }

    override fun connected() {
      //  Toast.makeText(this, "Подключено!", Toast.LENGTH_SHORT).show()
    }

    override fun receiveGranitMessage(message: GranitMessage) {
        if (message.data[0].toInt() and 0xFF == 0x55 && message.data[1].toInt() and 0xFF == 0xAA) {
            Toast.makeText(
                applicationContext,
                "Принято широковещательное сообщение!",
                Toast.LENGTH_SHORT
            ).show()
            binding.message.setText(message.data.copyOfRange(2, message.data.size).decodeToString())
        }
        else {
            Toast.makeText(
                applicationContext,
                "Принято личное сообщение!",
                Toast.LENGTH_SHORT
            ).show()
            binding.message.setText(message.data.decodeToString())
        }

    }

    override fun send(senderId: String, destId: String, data: String) {
        if (mService != null) {
            Toast.makeText(this, "Готов к отправке!", Toast.LENGTH_SHORT).show()
            mService!!.sendWithWave(data.toByteArray(), senderId, destId)
        }
    }

    fun send(senderId: List<String>, destId: List<String>, data: String) {
        if (mService != null) {
            Toast.makeText(this, "Готов к отправке!", Toast.LENGTH_SHORT).show()
            mService!!.sendWithWave(data.toByteArray(), senderId, destId)
        }
    }

    override fun sendTest(senderId: List<String>, destId: List<String>, data: String) {
        GlobalScope.launch {
            while (binding.checkbox.isChecked) {
                if (mService != null) {
                    val date = Calendar.getInstance().time.toString()
                    mService!!.sendWithWave(byteArrayOf(0x55, 0xAA.toShort().toByte()) +
                            (senderId[2] + "." + senderId[3]).toByteArray() +
                            ",".toByteArray() +
                            date.toByteArray() +
                            ",".toByteArray() +
                            data.toByteArray(), senderId, "0.0.0.0".split("."))
                }
                delay(10000)
            }
        }
    }

    override fun setTechnicDelivered(technicName: String) {
        updateMessageGranit()
            // Toast.makeText(this, "Получил подтв технику $technicName", Toast.LENGTH_SHORT).show()
    }

    override fun setGapDelivered(x: Double, y: Double) {
        updateMessageGranit()
       // Toast.makeText(this, "Получил разрыв lat: " + x + "lon: " + y, Toast.LENGTH_SHORT).show()
    }

    override fun updateMessageGranit() {
        Toast.makeText(this, "Сообщение успешно доставлено!", Toast.LENGTH_SHORT).show()
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun getMyId(): String {
        return binding.myId.text.toString()
    }
}