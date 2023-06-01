package com.example.myapplication.mapper;

import com.example.myapplication.service.ServiceCallback


object MessageMapper {

    fun mapMessageFromGranit(message: String, callback: ServiceCallback) {
        try {
            val stringArray = message.split(";")
            val destId = stringArray[1].trimEnd()
            val text = stringArray[2].trimEnd()
            val senderId = stringArray[0].replace("+", "")

            if (destId == callback.getMyId())
                checkMessageGranit(text, senderId, destId, callback)
            else
                callback.showToast("Сообщение адресовано другому абоненту!")

        } catch (_: Exception) {
            callback.showToast("Не удалось распознать сообщение!")
        }
    }

    private fun checkMessageGranit(str: String, senderId: String, destId: String, callback: ServiceCallback) {
            if (str.contains("Данные по цели:")) {
                val technic = TechnicMapperUI.mapTextToTechnicUI(str)
                callback.showToast("Получил цель " + technic.name)
                callback.send(
                    destId, senderId, "Принял цель " + technic.name)
            } else if (str.contains("Разрыв")) {
                val pair = TechnicMapperUI.mapTextRadioToGap(str)
                callback.showToast("Получил разрыв!")
                val msg = "Принял разрыв x=${pair.first.coordinates.x}:X y=${pair.first.coordinates.y}:Y"
                callback.send(destId, senderId, msg)
            } else if (str.contains("Принял цель")) {
                callback.setTechnicDelivered(str.substring(12))
            } else if (str.contains("Принял разрыв")) {
                callback.setGapDelivered(
                    str.substring(str.indexOf("x=") + 2, str.indexOf(":X")).toDouble(),
                    str.substring(str.indexOf("y=") + 2, str.indexOf(":Y")).toDouble()
                )
            } else if (str.contains("Zпринято")) {
                callback.updateMessageGranit()
            }else {
                callback.send(destId, senderId, "Zпринято")
            }
    }

    fun createRadioGranitMessage(senderId: String, destId: String, data: String): ByteArray {
        return "|+$senderId;$destId;$data\r\n|".toByteArray(charset("ISO-8859-5"))
    }
}
