package com.example.myapplication

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.mapper.MessageMapper.createRadioGranitMessage
import com.example.myapplication.mapper.MessageMapper.mapMessageFromGranit
import com.example.myapplication.mapper.TechnicMapperUI.mapGapToText
import com.example.myapplication.mapper.TechnicMapperUI.mapTechnicToText
import com.example.myapplication.mapper.TechnicTypes
import com.example.myapplication.model.Coordinates
import com.example.myapplication.model.Gap
import com.example.myapplication.model.Technic
import com.example.myapplication.service.LocalService
import com.example.myapplication.service.ServiceCallback


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
            if (destId.getText().toString() != "" && message.getText().toString() != "") send(
                id.getText().toString(),
                destId.getText().toString(),
                message.getText().toString()
            ) else showToast("Пустые поля!")
        })
        sendTechnicButton.setOnClickListener {
            if (destId.getText().toString() != "") send(
                id.getText().toString(),
                destId.getText().toString(),
                mapTechnicToText(technic)
            )
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
        Toast.makeText(this, "Подключено!", Toast.LENGTH_SHORT).show()
    }

    override fun receiveGranitMessage(message: String) {
        mapMessageFromGranit(message, this)
    }

    override fun send(senderId: String, destId: String, data: String) {
        val message = createRadioGranitMessage(senderId, destId, data)
        if (mService != null) {
            Toast.makeText(this, "Готов к отправке!", Toast.LENGTH_SHORT).show()
            mService!!.sendWithWave(message)
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