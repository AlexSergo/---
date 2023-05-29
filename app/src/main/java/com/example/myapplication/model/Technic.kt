package com.example.myapplication.model

import com.example.myapplication.mapper.TechnicTypes
import com.example.myapplication.model.Coordinates
import com.example.myapplication.model.MapTarget

data class Technic(
    override val coordinates: Coordinates,
    override val id: Int = 0,
    override val name: String,
    override val technicType: TechnicTypes,
    override val division: String? = null,
    override val droneName: String? = null,
    override var isDelivered: Boolean = false,
    override val isAllies: Boolean = false,
    override val isDestroyed: Boolean = false,
    override val isCovered : Boolean = false,
    override val description: String?,
): MapTarget
