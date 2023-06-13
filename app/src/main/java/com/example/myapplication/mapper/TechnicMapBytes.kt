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
    TechnicTypes.BTR to "БТР",
    TechnicTypes.BMP to intToBytes(3213),
    TechnicTypes.HELICOPTER to "ВЕРТ",
    TechnicTypes.PTRK to intToBytes(3110),
    TechnicTypes.KLN_PESH to "КЛН ПЕШ",
    TechnicTypes.KLN_BR to "КЛН БР",
    TechnicTypes.TANK to intToBytes(3112),
    TechnicTypes.GAP to "Разрыв",
    TechnicTypes.ANOTHER to "ДРУГАЯ",
    TechnicTypes.MORTAR to "МИНОМЁТ",
    TechnicTypes.TANK_UNION to "ТАНК",
    TechnicTypes.BMD to "БМД"
)

fun intToBytes(i: Int): ByteArray =
    ByteBuffer.allocate(Int.SIZE_BYTES).putInt(i).array()

fun bytesToInt(bytes: ByteArray): Int =
    ByteBuffer.wrap(bytes).int