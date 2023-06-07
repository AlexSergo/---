package com.example.myapplication

import android.util.Log

data class Sender(
    val id: ByteArray
)

object ManevrProtocol {

    private var myAddressCRC: ByteArray? = null
    private var mapOfSenders = mutableMapOf<Sender, MutableMap<Int, ByteArray>>()

    fun getListData(data: ByteArray, senderId: String, destId: String): List<ByteArray>{
        val options = 0x0
        val port = byteArrayOf(0x0, 0x1)
        val senderCRC = Utils.getCRCXModem(senderId.toByteArray())
        val destCRC = Utils.getCRCXModem(destId.toByteArray())
        val crc16 = Utils.getCRCXModem(data)
        val size = data.size
        val fullMessage = byteArrayOf(
            options.toByte(),
            senderCRC[0], senderCRC[1],
            destCRC[0], destCRC[1],
            port[0], port[1],
            crc16[0], crc16[1],
            size.shr(8).toByte(), size.toByte()) + data

        return divideArray(fullMessage)
    }

    private fun divideArray(fullMessage: ByteArray): List<ByteArray> {
        val subArrays = mutableListOf<ByteArray>()
        var i = 0
        val chunkSize = 42
        val countOfChunks = if (fullMessage.size % chunkSize != 0)
            fullMessage.size / chunkSize + 1
        else
            fullMessage.size / chunkSize

        while (i < fullMessage.size) {
            val subArray = ByteArray(chunkSize)
            for (j in 0 until chunkSize) {
                if (i + j < fullMessage.size) {
                    subArray[j] = fullMessage[i + j]
                } else {
                    subArray[j] = 0
                }
            }
            val header = getHead(i / chunkSize, countOfChunks)
            subArrays.add(header + subArray)
            i += chunkSize
        }
        return subArrays
    }

    private fun getHead(chunkNumber: Int, countOfChunks: Int): ByteArray {
        val magicBytes = byteArrayOf(0x04, 0x96.toShort().toByte(), 0x43)
        if (myAddressCRC == null)
            myAddressCRC = Utils.getCRCXModem(Device.id.toByteArray())
        val port = byteArrayOf(0x0, 0x1)

        val bit0 = (chunkNumber shr 0) and 1
        val bit1 = (chunkNumber shr 1) and 1
        val bit2 = (chunkNumber shr 2) and 1
        val bit3 = (chunkNumber shr 3) and 1
        Log.d("GRANITTTT", "num $countOfChunks, bits: $bit0, $bit1, $bit2. $bit3")
        val bit4 = (countOfChunks shr 0) and 1
        val bit5 = (countOfChunks shr 1) and 1
        val bit6 = (countOfChunks shr 2) and 1
        val bit7 = (countOfChunks shr 3) and 1
        Log.d("GRANITTTT", "count $chunkNumber, bits: $bit4, $bit5, $bit6. $bit7")

        val key = ((chunkNumber shl 4) + (countOfChunks - 1)).toShort().toByte()

        return magicBytes + myAddressCRC!! + port + byteArrayOf(key)
    }

    fun parsePacket(packet: ByteArray): ByteArray? {
        val HEADER_SIZE = 8
        val header = packet.copyOfRange(0, HEADER_SIZE)
        if (header[0] == 0x04.toShort().toByte() &&
            header[1] == 0x96.toShort().toByte() &&
            header[2] == 0x43.toShort().toByte()){
            Log.d("GRANITTTT", "Маневр принял пакет!")
            val senderCRC = byteArrayOf(header[3], header[4])

            val sender = Sender(senderCRC)
            val key = header.last()
            val bit0 = (key.toInt() shr 0) and 1
            val bit1 = (key.toInt() shr 1) and 1
            val bit2 = (key.toInt() shr 2) and 1
            val bit3 = (key.toInt() shr 3) and 1
            val bit4 = (key.toInt() shr 4) and 1
            val bit5 = (key.toInt() shr 5) and 1
            val bit6 = (key.toInt() shr 6) and 1
            val bit7 = (key.toInt() shr 7) and 1
            Log.d("GRANITTTT", "key $key, bits: $bit0, $bit1, $bit2. $bit3, $bit4, $bit5, $bit6. $bit7")
            val chunkNumber = (key.toInt() shr 4) + 1
            val countOfChunks = ((bit3 shl 3) or (bit2 shl 2) or (bit1 shl 1) or (bit0 shl 0)).toByte()

            if (mapOfSenders[sender] != null)
                mapOfSenders[sender]?.set(chunkNumber.toInt() and 0xFF,
                    packet.copyOfRange(HEADER_SIZE, packet.size)
                )
            else
                mapOfSenders[sender]

            if (mapOfSenders[sender]?.size == countOfChunks.toInt() and 0xFF){
                val packages = mapOfSenders[sender]!!
                mapOfSenders[sender]?.clear()
                return cleanMessage(packages, sender.id)
            }
        }
        return null
    }

    private fun cleanMessage(packages: MutableMap<Int, ByteArray>, id: ByteArray): ByteArray? {
        val sortedPackages = packages.toSortedMap()
        val HEADER_SIZE = 13
        val header = sortedPackages[sortedPackages.firstKey()]?.copyOfRange(0, HEADER_SIZE) ?: byteArrayOf()
        if (header.isEmpty())
            return null
        val destCRC = Utils.getCRCXModem(byteArrayOf(header[3], header[4]))
        if (myAddressCRC == null)
            myAddressCRC = Utils.getCRCXModem(Device.id.toByteArray())
        if (!myAddressCRC.contentEquals(destCRC)){
            return null
        }

        // получаем все тело
        val fullData = mutableListOf<Byte>()
        for ((key, value) in packages){
            fullData.addAll(value.toList())
        }
        val body = fullData.subList(HEADER_SIZE, fullData.size).toByteArray()
        val bodyCRC = Utils.getCRCXModem(body)
        val crc = byteArrayOf(header[9], header[10])

        // проверка контрольной суммы тела
        if (!bodyCRC.contentEquals(crc))
            return null

        return body
    }
}