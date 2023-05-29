package com.example.myapplication.model

import com.example.myapplication.mapper.TechnicTypes
import com.example.myapplication.model.Coordinates

data class Gap(
    val coordinates: Coordinates,
    val targetId: Int = 0,
    val division: String?,
    val id: Int = 0,
    var isDelivered: Boolean = false,
    val technicType: TechnicTypes = TechnicTypes.GAP,
    val droneName: String? = null
)
