package com.example.myapplication.service

interface ServiceCallback {
    fun disconnected()
    fun connected()
    fun receiveGranitMessage(message: String)
    fun send(senderId: String, destId: String, data: String)
    fun setTechnicDelivered(technicName: String)
    fun setGapDelivered(x: Double, y: Double)
    fun updateMessageGranit()
    fun showToast(message: String)
    fun getMyId(): String
}