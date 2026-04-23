package com.example.semestra.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainShellViewModel : ViewModel() {
    private val _classFilter = MutableLiveData<String?>(null)
    val classFilter: LiveData<String?> = _classFilter

    fun setClassFilter(value: String?) {
        _classFilter.value = value
    }
}
