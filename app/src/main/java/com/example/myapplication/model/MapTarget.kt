package com.example.myapplication.model

import com.example.myapplication.mapper.TechnicTypes
import com.example.myapplication.model.Coordinates

interface MapTarget {
    val coordinates: Coordinates
    val id: Int
    val name: String
    val division: String?
    val droneName: String?
    var isDelivered: Boolean
    val isAllies: Boolean
    val isDestroyed: Boolean
    val isCovered: Boolean
    val technicType: TechnicTypes
    val description: String?
}
