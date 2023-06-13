package com.example.myapplication.service

import com.example.myapplication.GranitMessage

interface ServiceCallback {
    fun disconnected()
    fun connected()
    fun receiveGranitMessage(message: GranitMessage)
    fun send(senderId: String, destId: String, data: String)
    fun sendTest(senderId: String, destId: String, data: String)
    fun setTechnicDelivered(technicName: String)
    fun setGapDelivered(x: Double, y: Double)
    fun updateMessageGranit()
    fun showToast(message: String)
    fun getMyId(): String
}