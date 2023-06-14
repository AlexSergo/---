package com.example.myapplication.mapper

import com.example.myapplication.model.Coordinates
import com.example.myapplication.model.Gap
import com.example.myapplication.model.Technic
import java.nio.charset.Charset

object TechnicMapperUI {

    fun mapTextToTechnicUI(text: String): Technic {
        val name = text.substring(text.indexOf("Данные") + 16, text.indexOf("\n"))
        val xPlane = text.substring(text.indexOf("X = ") + 4, text.indexOf("Y = ") - 1)
        val yPlane = text.substring(text.indexOf("Y = ") + 4, text.indexOf("Высота:") - 1)
        val zPlane = text.substring(text.indexOf("Высота:") + 8, text.indexOf("Тип") - 1)
        val type = text.substring(text.indexOf("Тип") + 13, text.indexOf("Подразделение") - 1)
        var allies = true
        var division = ""
        var isCovered = false
        var isDestroyed = false
        var description = ""
        if (text.contains("Враг")) {
            allies = false
            division = text.substring(text.indexOf("Подразделение") + 15, text.indexOf("Враг") - 2)
        } else
            division =
                text.substring(text.indexOf("Подразделение") + 15, text.indexOf("Союзник") - 2)
        if (text.contains("Укрытая"))
            isCovered = true
        if (text.contains("Уничтожена"))
            isDestroyed = true

        val technicType = (TechnicTypesRu.filterValues { it == type }.keys).first()

        return Technic(
            coordinates = Coordinates(
                x = xPlane.toDouble(),
                y = yPlane.toDouble(),
                h = zPlane.toDouble()
            ),
            technicType = technicType,
            division = division,
            isAllies = allies,
            name = name,
            isDestroyed = isDestroyed,
            isCovered = isCovered,
            description = description,
        )
    }

    fun mapTextRadioToGap(string: String): Pair<Gap, Coordinates> {

        val gap: Gap
        val id: Int = string.substring(string.indexOf("Разрыв") + 7, string.indexOf("\n")).toInt()

        val division = string.substring(
            string.indexOf("Позывной") + 9,
            string.indexOf("Позывной дрона")
        )
        val droneName = string.substring(
            string.indexOf("Позывной дрона") + 15,
            string.indexOf("Широта")
        )
        val lat = string.substring(
            string.indexOf("Широта") + 7,
            string.indexOf("Долгота")
        ).toDouble()
        val lon = string.substring(
            string.indexOf("Долгота") + 8,
            string.indexOf("Высота")
        ).toDouble()
        val alt = string.substring(
            string.indexOf("Высота") + 7,
            string.indexOf("Шц")
        ).toDouble()
        val latTechnic = string.substring(
            string.indexOf("Шц") + 3,
            string.indexOf("Дц")
        ).toDouble()
        val lonTechnic = string.substring(string.indexOf("Дц") + 3, string.length).toDouble()
        gap = Gap(
            id = id,
            coordinates = Coordinates(lat, lon, alt),
            division = division,
            droneName = droneName
        )

        return Pair(gap, Coordinates(latTechnic, lonTechnic))
    }

    fun mapTechnicToBytes(technic: Technic): ByteArray {
        val type = TechnicTypesBytes[technic.technicType]!!
        val name = technic.name.toByteArray(Charset.forName("ISO-8859-5"))
        val allies = if (technic.isAllies) 1 else 0
        val destroyed = if (technic.isDestroyed) 1 else 0
        val covered = if (technic.isCovered) 1 else 0
        val sum = (allies shl 2) or (destroyed shl 1) or (covered shl 0)
        val lat = (technic.coordinates.x.toString() + " ").toByteArray()
        val lon = (technic.coordinates.y.toString() + " ").toByteArray()
        val h = technic.coordinates.h.toString().toByteArray()

        return type + name + byteArrayOf(sum.toByte()) + lat + lon + h
    }

    fun mapGapToText(gap: Gap, technic: Technic): String {
        val builder = StringBuilder()
        builder.append("Разрыв ${gap.id}\n")
        builder.append("Позывной " + gap.division + "\n")
        builder.append("Позывной дрона " + gap.droneName + "\n")
        builder.append("Широта " + gap.coordinates.x.toString() + "\n")
        builder.append("Долгота " + gap.coordinates.y.toString() + "\n")
        builder.append("Высота " + gap.coordinates.h.toString() + "\n")
        builder.append(
            "Шц ${Math.round(technic.coordinates.x * 1000000.0) / 1000000.0} Дц ${
                Math.round(
                    technic.coordinates.y * 1000000.0
                ) / 1000000.0
            }"
        )

        return builder.toString()
    }
}
