package com.example.myapplication

import android.util.Log

object Utils {

    private val fullBytes = mutableListOf<Byte>()
/*    private fun crcXModemUpdate(crc: Int, data: Int): Int {
        var newCrc = crc xor (data shl 8)
        for (i in 0 until 8) {
            if ((newCrc and 0x8000) != 0) {
                newCrc = (newCrc shl 1) xor 0x1021
            } else {
                newCrc = (newCrc shl 1)
            }
        }
        return newCrc
    }

    fun setCrc16f(buff: ByteArray): ByteArray {
        val count = buff.size
        var crc = 0xffff
        for (i in 0 until count) {
            crc = crcXModemUpdate(crc, buff[i].toInt())
        }
        return buff + byteArrayOf((crc shr 8).toByte(), (crc and 0xff).toByte())
    }*/

    fun calculateCrc16Xmodem(data: ByteArray): Int {
        val polynomial = 0x1021
        var crc = 0x0000

        for (byte in data) {
            crc = crc xor (byte.toInt() shl 8)
            for (i in 0 until 8) {
                if (crc and 0x8000 != 0) {
                    crc = (crc shl 1) xor polynomial
                } else {
                    crc = crc shl 1
                }
            }
        }

        return crc and 0xFFFF
    }

    fun getSample(): ByteArray{
       // return byteArrayOf(0x10, 0x7c, 0x02, 0xE6.toShort().toByte(), 0x78, 0x2b, 0xd0.toShort().toByte(), 0xd1.toShort().toByte(), 0x7d)
        return byteArrayOf(0x10, 0, 0, 0 ,0 ,0) + "dcsdfbfdgntsbfgfghnдлолдолдоtорлролролрлорлрлysbsbsdbfgsbg".toByteArray()
    }

    fun getResult(START: Int, RAW_DATA: Int, FROM: Int, TO: Int, BAT: Int, data: ByteArray, END: Int): ByteArray {
        return byteArrayOf(START.toByte(), RAW_DATA.toByte(), FROM.toByte(), FROM.toByte(), TO.toByte(), TO.toByte(), BAT.toByte())+
                data + byteArrayOf(END.toByte());
    }

    fun getResult(START: Int, data: ByteArray, END: Int): ByteArray {
        return byteArrayOf(START.toShort().toByte()) + data + byteArrayOf(END.toShort().toByte());
    }

    fun getBytesForCRC(RAW_DATA: Int, FROM: Int, TO: Int, BAT: Int, data: ByteArray): ByteArray {
        return byteArrayOf(RAW_DATA.toByte(), FROM.toByte(),FROM.toByte(), TO.toByte(),TO.toByte(), BAT.toByte()) + data
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

    fun parseData(data: ByteArray) {
        val list = data.toMutableList()
        if (list.first().toInt() and 0xFF == 0xD0 && list.last().toInt() and 0xFF == 0xD1){
            Log.d("GRANITTTT", "Определил пакет!")
            list.removeFirst()
            list.removeLast()
            if (list.first().toInt() and 0xFF == 0x10){
                Log.d("GRANITTTT", "Пакет с данными!")
                for (i in 1..6)
                    list.removeFirst()
                val crcByte1 = list.last()
                list.removeLast()
                val crcByte2 = list.last()
                list.removeLast()
                checkCrc(data, byteArrayOf(crcByte1, crcByte2))
                fullBytes.addAll(list)
            }
        }
    }

    private fun checkCrc(data: ByteArray, byteArrayOf: ByteArray) {

    }
}