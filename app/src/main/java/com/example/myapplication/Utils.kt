package com.example.myapplication

import android.content.Context
import android.util.Log
import android.widget.Toast

object Utils {

    private val fullBytes = mutableListOf<Byte>()

    fun getResult(START: Int, data: ByteArray, END: Int): ByteArray {
        return byteArrayOf(START.toShort().toByte()) + data + byteArrayOf(END.toShort().toByte());
    }

    fun stuffBytes(bytes: ByteArray): ByteArray {
        val list = mutableListOf<Byte>()
        for (i in bytes.indices)
            if (bytes[i] == 0xD0.toShort().toByte() ||
                bytes[i] == 0xD1.toShort().toByte() ||
                bytes[i] == 0x7D.toShort().toByte()) {
                list.add(0x7D.toShort().toByte())
                if (bytes[i] != 0x7D.toShort().toByte())
                    list.add(((bytes[i].toInt() and 0xFF) + 0x20).toShort().toByte())
                else
                    list.add(0x5d.toShort().toByte())
            }
            else
                list.add(bytes[i])

        return list.toByteArray()
    }

    fun getResult(START: Int, bytes: ByteArray, crc: ByteArray, END: Int): ByteArray {
        return byteArrayOf(START.toShort().toByte()) + bytes + crc + byteArrayOf(END.toShort().toByte());
    }

    fun parseData(data: ByteArray, context: Context): GranitMessage? {
        val copy = data.copyOfRange(1, data.size - 1)

        val list = data.toMutableList()
        if (list.first().toInt() and 0xFF == 0xD0 && list.last().toInt() and 0xFF == 0xD1){
            Log.d("GRANITTTT", "Определил пакет! ${list[1]}")
            list.removeFirst()
            list.removeLast()
            if (list.first().toInt() and 0xFF == 0x10){
                Log.d("GRANITTTT", "Пакет с данными!")
                // удаляем отправителя, получателя, батарею и идентификатор
                for (i in 1..6)
                    list.removeFirst()

                val crc = byteArrayOf(list.last() , list[list.size - 2])
                // удаляем crc
                list.removeLast()
                list.removeLast()
                if (crc.contentEquals(byteArrayOf(copy[copy.size - 1], copy[copy.size - 2]))) {
                    return ManevrProtocol.parsePacket(list.toByteArray(), context)
                }
                else {
                    Log.d("GRANITTTT", "Данные битые! crc = ${crc.decodeToString()}")
                    return null
                }
            }
            else if (list.first().toInt() and 0xFF == 0x21){
                Log.d("GRANITTTT", "Успешно отправлено!")
                Toast.makeText(context, "Успешно отправлено!", Toast.LENGTH_SHORT).show()
            }
        }
        return null
    }
    fun addCRCXModem(data: ByteArray): ByteArray? {
        val result = ByteArray(data.size + 2)
        System.arraycopy(data, 0, result, 0, data.size)
        var crc = 0xFFFF.toShort()
        for (b in data) {
            crc = (crc_xmodem_update(crc, b).toInt() and 0xFFFF).toShort()
        }
        result[result.size - 2] = (crc.toInt() shr 8).toByte()
        result[result.size - 1] = (crc.toInt() and 0xFF).toByte()
        //Log.d(TAG, "WaveNetCRC crc = " + BytesUtil.bytesToHexString(result));
        return result
    }

    // Возвращает CRCXModem с начальным значением 0xFFFF
    fun getCRCXModem(data: ByteArray): ByteArray {
        val result = ByteArray(2)
        result[0] = 0
        result[1] = 0
        var crc = 0xFFFF.toShort()
        for (b in data) {
            crc = crc_xmodem_update(crc, b)
        }
        result[0] = (crc.toInt() shr 8).toByte()
        result[1] = (crc.toInt() and 0xFF).toByte()
        return result
    }

    /** Функция расчета СRCXModem  */
    fun crc_xmodem_update(crc: Short, data: Byte): Short {
        var crc = crc
        crc = (crc.toInt() xor (data.toShort().toInt() shl 8).toShort().toInt()).toShort()
        for (i in 0..7) {
            crc = if (crc.toInt() and 0x8000.toShort().toInt() == 0x8000.toShort().toInt()) {
                ((crc.toInt() shl 1).toShort().toInt() xor 0x1021).toShort()
            } else (crc.toInt() shl 1).toShort()
        }
        return crc
    }

    // Проверяем СRCXModem пакета данных (CRC - 2 последних байта)
    fun verifyCRCXModem(data: ByteArray): Boolean {
        return try {
            val dataLength = data.size
            // Получаем массив чистых данных
            val bytes = ByteArray(dataLength - 2)
            System.arraycopy(data, 0, bytes, 0, bytes.size)
            // Получаем CRC
            val crc = getCRCXModem(bytes)

            // Сравниваем CRC и возвращаем результат
            data[dataLength - 2] == crc[0] && data[dataLength - 1] == crc[1]
        } catch (e: Exception) {
            false
        }
    }

    fun addIdentificators(RAW_DATA: Int, FROM: Int, TO: Int, BAT: Int, bytes: ByteArray): ByteArray {
        return byteArrayOf(RAW_DATA.toShort().toByte(),
            FROM.toShort().toByte(), FROM.toShort().toByte(),
            TO.toShort().toByte(), TO.toShort().toByte(),
            BAT.toShort().toByte()) + bytes
    }

    fun zz(afterStuff: ByteArray) {
        println(afterStuff.contentToString())
    }
}