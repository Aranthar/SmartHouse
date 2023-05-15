package com.example.smarthouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData

class ViewModel : ViewModel() {
        var minNumber = 60
        var maxNumber = 70
        val minCurrentNumber: MutableLiveData<Int> by lazy {
                MutableLiveData()
        }
        val maxCurrentNumber: MutableLiveData<Int> by lazy {
                MutableLiveData()
        }

        var isHomeLampOnPowerEnable = false
        var isStreetLampOnPowerEnable = false
        val homeLampOnPower: MutableLiveData<Boolean> by lazy {
                MutableLiveData()
        }
        val streetLampOnPower: MutableLiveData<Boolean> by lazy {
                MutableLiveData()
        }
}