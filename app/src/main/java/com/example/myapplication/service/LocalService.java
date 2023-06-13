package com.example.myapplication.service;


import static android.app.Notification.CATEGORY_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.Device;
import com.example.myapplication.GranitMessage;
import com.example.myapplication.MainActivity;
import com.example.myapplication.ManevrProtocol;
import com.example.myapplication.R;
import com.example.myapplication.Utils;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public class LocalService extends Service {

    private final IBinder binder = new LocalBinder();


    private byte[] start = "|".getBytes(Charset.forName("ISO-8859-5"));
    private byte[] end = "|".getBytes(Charset.forName("ISO-8859-5"));
    private byte[] allByteArray;
    private byte[] combine = new byte[0];
    private ByteBuffer buff;

    private ServiceCallback listener;

    private UsbDevice usbDevice;
    private UsbDeviceConnection usbConnection;
    public UsbSerialDevice serial;
    private UsbManager mUsbManager;
    private Context context;
    private static final String NOTIFICATION_NAME = "Foreground_service";
    private static final String NOTIFICATION_ID = "Foreground_service_id";

    public void setContext(Context context, ServiceCallback listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mUsbManager =
            (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbReceiver, filter);


        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel =
            new NotificationChannel(NOTIFICATION_ID,
            NOTIFICATION_NAME,
            NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(false);
            Intent i = new Intent(getApplicationContext(),
                MainActivity.class);
            i.addFlags(FLAG_ACTIVITY_SINGLE_TOP);
            ((NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE)).createNotificationChannel(
                channel);
            Notification notification =
            new NotificationCompat.Builder(this, NOTIFICATION_ID)
            .setContentTitle("Работает Гранит")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setAutoCancel(true)
                .setCategory(CATEGORY_SERVICE)
                .setOngoing(true)
                .setWhen(0)
                .setVisibility(
                    NotificationCompat.VISIBILITY_SECRET)
                .setNumber(0)
                .setBadgeIconType(
                    NotificationCompat.BADGE_ICON_NONE)
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .build();
            startForeground(1002, notification);
        }

    }

    @Override
    public void onDestroy() {

        try {
            unregisterReceiver(usbReceiver);
        }catch (Exception e){}
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public LocalService getService() {
            // Return this instance of LocalService so clients can call public methods.
            return LocalService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private static final String ACTION_USB_PERMISSION =
        "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (usbDevice != null) {
                if (usbDevice.getVendorId() != 4292)
                    return;
            } else
                return;
            if (ACTION_USB_PERMISSION.equals(action)) {
                Log.e("USB", "Permission");
                try {
                    synchronized (this) {
                        usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        connectToUsbDevice(usbDevice);
                    }
                } catch (Exception e) {
                    Log.e("SerialService", e.toString());
                }
            } else if (ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.e("USB", "DETACHED");
                try {
                    if (serial != null)
                        serial.close();
                    if (usbConnection != null)
                        usbConnection.close();
                    listener.disconnected();
                }
                catch (Exception e) {}
            } else if (ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.e("USB", "ATTACHED");
                listener.showToast("Подключено USB");
                usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                tryToconnectToUsb(usbDevice);
            }
        }
    };

    public void tryToconnectToUsb(UsbDevice usbDevice) {

        if (usbDevice != null) {
            mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            connectToUsbDevice(usbDevice);
        }
    }


    private void connectToUsbDevice(UsbDevice usbDevice) {
        Log.e("USB", "CONNECT " + usbDevice.toString());
        synchronized (this) {
            if (usbDevice != null) {
                try {
                    usbConnection = mUsbManager.openDevice(usbDevice);
                    serial = UsbSerialDevice.createUsbSerialDevice(usbDevice, usbConnection);
                    Log.e("USB1", "Serial1 " + serial.toString());
                    serial.open();
                    serial.setBaudRate(115200);
                    serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serial.setParity(UsbSerialInterface.STOP_BITS_1);
                    serial.setParity(UsbSerialInterface.PARITY_NONE);
                    serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serial.read(mCallback);
                    Log.e("USB", "Serial " + serial.toString());
                    listener.connected();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Готов к передаче", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    listener.showToast("не удалось подключиться!");
                }
            }
        }
    }

    private final Integer START = 0xD0;
    private final Integer END = 0xD1;
    private final Integer FROM = 0x0000;
    private final Integer TO = 0x0000;
    private final Integer BAT = 0x00;
    private final Integer RAW_DATA = 0x10;

    private final int BUFF_ERROR = 0x22;


    public void sendWithWave(byte[] bytes, String senderId, String destId) throws InterruptedException {
        if (serial != null) {
            List<byte[]> list = ManevrProtocol.INSTANCE.getListData(bytes, senderId, destId, context);
            for (int i = 0; i < list.size(); i++) {
                byte[] sample = Utils.INSTANCE.addIdentificators(RAW_DATA, FROM, TO, BAT, list.get(i));
                byte[] crc = Utils.INSTANCE.getCRCXModem(sample);
                byte[] withCRC = Utils.INSTANCE.addCRCXModem(sample);
                byte[] afterStuff = Utils.INSTANCE.stuffBytes(withCRC);
                byte[] result = Utils.INSTANCE.getResult(START, afterStuff, END);

                Utils.INSTANCE.zz(afterStuff);
                System.out.println("result " +  result.length);

                serial.write(result);
                Thread.sleep(50);
            }
        }
        else
            listener.showToast("Гранит не подключен!");
    }


    public void addBuffer(byte[] data) {
        allByteArray = new byte[combine.length + data.length];
        buff = ByteBuffer.wrap(allByteArray);
        buff.put(combine);
        buff.put(data);
        combine = buff.array();
    }

    void receiveMessage(byte[] data){
/*        String str = new String(data, Charset.forName("ISO-8859-5"));
        String newStr = (str.substring( 1, str.length() - 1 ));
        listener.receiveGranitMessage(newStr);*/
    }

    private final UsbSerialInterface.UsbReadCallback mCallback =
    new UsbSerialInterface.UsbReadCallback() {

        @Override
        public void onReceivedData(byte[] data) {
            if (data.length <= 0) {
                return;
            }
            try {

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        GranitMessage result = Utils.INSTANCE.parseData(data, context);
                        if (result != null) {
                            listener.receiveGranitMessage(result);
                        }

      /*                  if (data[0] == start[0] && data[data.length-1] == end[0]) {
                            //пакет размером меньше чем 55 байт
                            // затем очистить буфер
                            addBuffer(data);
                            receiveMessage(combine);
                            combine = new byte[0];
                        }
                        if (data[0] == start[0] && data[data.length-1] != end[0]) {
                            //пришел пакет больше чем 55 байт добавить в буффер
                            addBuffer(data);
                        }
                        if (data[0] != start[0] && data[data.length-1] != end[0]) {
                            //пакет который не содержит в себе стартового разделителя и конечного
                            addBuffer(data);
                        }
                        if (data[0] != start[0] && data[data.length-1] == end[0]) {
                            // самый последний пакет, добавить в буфер сообщить о готовности отправить строку в activity
                            // затем очистить буфер
                            addBuffer(data);
                            receiveMessage(combine);
                            combine = new byte[0];
                        }*/

                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    };


}
