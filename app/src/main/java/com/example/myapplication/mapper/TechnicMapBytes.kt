package com.example.myapplication.mapper

import java.nio.ByteBuffer

var TechnicTypesBytes = mapOf(
    TechnicTypes.LAUNCHER to intToBytes(9028),
    TechnicTypes.OVERLAND to intToBytes(1000),
    TechnicTypes.ARTILLERY to intToBytes(1001),
    TechnicTypes.REACT to intToBytes(1002),
    TechnicTypes.MINES to intToBytes(1003),
    TechnicTypes.ZUR to intToBytes(1004),
    TechnicTypes.RLS to intToBytes(3126),
    TechnicTypes.INFANTRY to intToBytes(6137),
    TechnicTypes.O_POINT to intToBytes(1005),
    TechnicTypes.KNP to intToBytes(1006),
    TechnicTypes.TANKS to  intToBytes(6194),
    TechnicTypes.BTR to intToBytes(3114),
    TechnicTypes.BMP to intToBytes(3213),
    TechnicTypes.HELICOPTER to intToBytes(1007),
    TechnicTypes.PTRK to intToBytes(3110),
    TechnicTypes.KLN_PESH to intToBytes(1008),
    TechnicTypes.KLN_BR to intToBytes(1009),
    TechnicTypes.TANK to intToBytes(3112),
    TechnicTypes.GAP to intToBytes(1010),
    TechnicTypes.ANOTHER to intToBytes(1011),
    TechnicTypes.MORTAR to intToBytes(1012),
    TechnicTypes.TANK_UNION to intToBytes(1013),
    TechnicTypes.BMD to intToBytes(1014),
)

fun intToBytes(i: Int): ByteArray =
    ByteBuffer.allocate(Int.SIZE_BYTES).putInt(i).array()

fun bytesToInt(bytes: ByteArray): Int =
    ByteBuffer.wrap(bytes).int