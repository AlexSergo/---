package com.example.myapplication

import android.content.Context
import android.util.Log
import android.widget.Toast

data class GranitMessage(
    val senderCRC: ByteArray,
    val data: ByteArray
)

object ManevrProtocol {

    private var myAddressCRC: ByteArray? = null
    private var mapOfSenders = hashMapOf<String, MutableMap<Int, ByteArray>>()
    private var  idPackage = -1

    fun getListData(data: ByteArray, senderId: String, destId: String, context: Context): List<ByteArray>{
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
            0x0, 0x0,
            port[0], port[1],
            crc16[0], crc16[1],
            size.shr(8).toByte(), (size and 0xFF).toShort().toByte()) + data

        println("my data ${data.contentToString()}")
        if (idPackage != 15)
            idPackage++
        else
            idPackage = 0
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
            println("pack send ${(header + subArray).contentToString()}")
            i += chunkSize
        }
        return subArrays
    }

    private fun getHead(chunkNumber: Int, countOfChunks: Int): ByteArray {
        val magicBytes = byteArrayOf(0x04, 0x96.toShort().toByte(), 0x43)
        if (myAddressCRC == null)
            myAddressCRC = Utils.getCRCXModem(Device.id.toByteArray())
        val port = byteArrayOf((idPackage shl 4).toShort().toByte(), 0x0)

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

    fun parsePacket(packet: ByteArray, context: Context): GranitMessage? {
        val HEADER_SIZE = 8
        val header = packet.copyOfRange(0, HEADER_SIZE)
        if (header[0] == 0x04.toShort().toByte() &&
            header[1] == 0x96.toShort().toByte() &&
            header[2] == 0x43.toShort().toByte()){

            val sender = byteArrayOf(header[3], header[4])
            val senderStr = header[3].toString() + header[4].toString()
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
            val chunkNumber = ((key.toInt() and 0xF0) shr 4) + 1
            val countOfChunks = (key.toInt() and 0xF) + 1
            println("packet rec ${packet.contentToString()}")
            val newPack = if (packet.last().toInt() and 0xFF == 125)
                packet.copyOfRange(0, packet.size - 1)
            else
                packet
            if (mapOfSenders[senderStr] != null) {
                if (chunkNumber == 1) {
                    mapOfSenders[senderStr]?.clear()
                }

                mapOfSenders[senderStr]?.set(
                    chunkNumber.toInt() and 0xFF,
                    newPack.copyOfRange(HEADER_SIZE, newPack.size)
                )
            }
            else {
                mapOfSenders[senderStr] = mutableMapOf()
                mapOfSenders[senderStr]?.set(chunkNumber.toInt() and 0xFF,
                    newPack.copyOfRange(HEADER_SIZE, newPack.size)
                )
            }

            if (mapOfSenders[senderStr]?.size == countOfChunks){
                val map = mutableMapOf<Int, ByteArray>()
                map.putAll(mapOfSenders[senderStr]!!)
                mapOfSenders[senderStr]?.clear()
                return cleanMessage(map, sender, context)
            }
        }
        return null
    }

    private fun cleanMessage(packages: MutableMap<Int, ByteArray>, id: ByteArray, context: Context): GranitMessage? {
        val sortedPackages = packages.toSortedMap()
        val HEADER_SIZE = 13
        if (sortedPackages.isEmpty()) {
            Toast.makeText(context, "Нет данных!", Toast.LENGTH_SHORT).show()
            return null
        }
        val header = sortedPackages[sortedPackages.keys.first()]?.copyOfRange(0, HEADER_SIZE) ?: byteArrayOf()
        if (header.isEmpty()) {
            Toast.makeText(context, "ошибка!", Toast.LENGTH_SHORT).show()
            return null
        }
        val destCRC = (byteArrayOf(header[3], header[4]))

        if (myAddressCRC == null)
            myAddressCRC = Utils.getCRCXModem(Device.id.toByteArray())
        if ((!byteArrayOf(header[3], header[4]).contentEquals(Utils.getCRCXModem("-".toByteArray()))))
            if (myAddressCRC!![0] !=header[3]|| myAddressCRC!![1] != header[4]){
                Toast.makeText(context, "сообщение не для меня! ${myAddressCRC.contentToString()} ${header.contentToString()}", Toast.LENGTH_SHORT).show()
                return null
            }
        val size = ((header[HEADER_SIZE - 2].toInt() shl 8) or (header[HEADER_SIZE - 1].toInt() and 0xFF))
        println("--- ${header[HEADER_SIZE - 2]} ${header[HEADER_SIZE - 1].toInt() and 0xFF}")
        // получаем все тело
        val fullData = mutableListOf<Byte>()
        for ((key, value) in packages){
            fullData.addAll(value.toList())
        }
        val body = removeTrailingZeros(fullData.subList(HEADER_SIZE, fullData.size).toByteArray(), size)
        val bodyCRC = Utils.getCRCXModem(body)
        val crc = byteArrayOf(header[HEADER_SIZE - 4], header[HEADER_SIZE - 3])
        println("body ${body.contentToString()}")

        // проверка контрольной суммы тела
        if (!bodyCRC.contentEquals(crc)) {
            Toast.makeText(context, "Сообщение битое! ${crc.contentToString()} ${bodyCRC.contentToString()} ${body.decodeToString()}", Toast.LENGTH_SHORT).show()
            return null
        }

        return GranitMessage(senderCRC = byteArrayOf(header[1], header[2]), data = body)
    }

    private fun removeTrailingZeros(input: ByteArray, size: Int): ByteArray {
        var endIndex = input.size - 1
        while (endIndex >= size) {
            endIndex--
        }
        println("size $size, endIn $endIndex, input.size ${input.size}")
        val withoutZeroes = input.copyOfRange(0, endIndex + 1).toMutableList()
        return withoutZeroes.toByteArray()
    }
}