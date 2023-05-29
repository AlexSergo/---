package com.example.myapplication.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.felhr.usbserial.UsbSerialInterface.UsbReadCallback
import java.nio.ByteBuffer
import java.nio.charset.Charset

class LocalService : Service() {
    private val binder: IBinder = LocalBinder()
    private val start = "|".toByteArray(Charset.forName("ISO-8859-5"))
    private val end = "|".toByteArray(Charset.forName("ISO-8859-5"))
    private lateinit var allByteArray: ByteArray
    private var combine = ByteArray(0)
    private var buff: ByteBuffer? = null
    private var listener: ServiceCallback? = null
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    var serial: UsbSerialDevice? = null
    private var mUsbManager: UsbManager? = null
    private var context: Context? = null
    fun setContext(context: Context?, listener: ServiceCallback?) {
        this.context = context
        this.listener = listener
    }

    override fun onCreate() {
        super.onCreate()
        mUsbManager = getSystemService(USB_SERVICE) as UsbManager
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbReceiver, filter)
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                NOTIFICATION_ID,
                NOTIFICATION_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setShowBadge(false)
            val i = Intent(
                applicationContext,
                MainActivity::class.java
            )
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            (getSystemService(
                NOTIFICATION_SERVICE
            ) as NotificationManager).createNotificationChannel(
                channel
            )
            val notification = NotificationCompat.Builder(this, NOTIFICATION_ID)
                .setContentTitle("Работает Гранит")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setWhen(0)
                .setVisibility(
                    NotificationCompat.VISIBILITY_SECRET
                )
                .setNumber(0)
                .setBadgeIconType(
                    NotificationCompat.BADGE_ICON_NONE
                )
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .build()
            startForeground(1002, notification)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(usbReceiver)
        super.onDestroy()
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods.
        val service: LocalService
            get() =// Return this instance of LocalService so clients can call public methods.
                this@LocalService
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            usbDevice = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
            if (usbDevice != null) {
                if (usbDevice!!.vendorId != 4292) return
            } else return
            if (ACTION_USB_PERMISSION == action) {
                Log.e("USB", "Permission")
                try {
                    synchronized(this) {
                        usbDevice =
                            intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice?
                        connectToUsbDevice(usbDevice)
                    }
                } catch (e: Exception) {
                    Log.e("SerialService", e.toString())
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
                Log.e("USB", "DETACHED")
                try {
                    if (serial != null) serial!!.close()
                    if (usbConnection != null) usbConnection!!.close()
                    listener!!.disconnected()
                } catch (e: Exception) {
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                Log.e("USB", "ATTACHED")
                usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                tryToconnectToUsb(usbDevice)
            }
        }
    }

    fun tryToconnectToUsb(usbDevice: UsbDevice?) {
        if (usbDevice != null) {
            mUsbManager = getSystemService(USB_SERVICE) as UsbManager
            connectToUsbDevice(usbDevice)
        }
    }

    private fun connectToUsbDevice(usbDevice: UsbDevice?) {
        Log.e("USB", "CONNECT " + usbDevice.toString())
        synchronized(this) {
            if (usbDevice != null) {
                try {
                    usbConnection = mUsbManager!!.openDevice(usbDevice)
                    serial = UsbSerialDevice.createUsbSerialDevice(usbDevice, usbConnection)
                    Log.e("USB1", "Serial1 " + serial.toString())
                    serial?.open()
                    serial?.setBaudRate(115200)
                    serial?.setDataBits(UsbSerialInterface.DATA_BITS_8)
                    serial?.setParity(UsbSerialInterface.PARITY_ODD)
                    serial?.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                    serial?.read(mCallback)
                    Log.e("USB", "Serial " + serial.toString())
                    listener!!.connected()
                } catch (e: Exception) {
                }
            }
        }
    }

    fun sendWithWave(data: ByteArray?) {
        if (serial != null) serial!!.write(data)
        listener?.showToast("Подключите гранит!")
    }

    fun addBuffer(data: ByteArray) {
        allByteArray = ByteArray(combine.size + data.size)
        buff = ByteBuffer.wrap(allByteArray)
        buff?.put(combine)
        buff?.put(data)
        combine = buff?.array()!!
    }

    fun receiveMessage(data: ByteArray?) {
        val str = String(data!!, Charset.forName("ISO-8859-5"))
        val newStr = str.substring(1, str.length - 1)
        listener!!.receiveGranitMessage(newStr)
    }

    private val mCallback = UsbReadCallback { data ->
        if (data.size <= 0) return@UsbReadCallback
        try {
            Handler(Looper.getMainLooper()).post {
                if (data[0] == start[0] && data[data.size - 1] == end[0]) {
                    //пакет размером меньше чем 55 байт
                    // затем очистить буфер
                    addBuffer(data)
                    receiveMessage(combine)
                    combine = ByteArray(0)
                }
                if (data[0] == start[0] && data[data.size - 1] != end[0]) {
                    //пришел пакет больше чем 55 байт добавить в буффер
                    addBuffer(data)
                }
                if (data[0] != start[0] && data[data.size - 1] != end[0]) {
                    //пакет который не содержит в себе стартового разделителя и конечного
                    addBuffer(data)
                }
                if (data[0] != start[0] && data[data.size - 1] == end[0]) {
                    // самый последний пакет, добавить в буфер сообщить о готовности отправить строку в activity
                    // затем очистить буфер
                    addBuffer(data)
                    receiveMessage(combine)
                    combine = ByteArray(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val NOTIFICATION_NAME = "Foreground_service"
        private const val NOTIFICATION_ID = "Foreground_service_id"
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    }
}