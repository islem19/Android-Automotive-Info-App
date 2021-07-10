package dz.islem.automotive.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dz.islem.automotive.data.CarManager

class MainViewModelFactory(private val carManager: CarManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T  =
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) MainViewModel(carManager) as T
            else throw IllegalArgumentException("wrong view model")
}