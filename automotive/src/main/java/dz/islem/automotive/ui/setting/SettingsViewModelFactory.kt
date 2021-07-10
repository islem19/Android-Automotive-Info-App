package dz.islem.automotive.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dz.islem.automotive.data.CarManager

class SettingsViewModelFactory(private val carManager: CarManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T  = if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) SettingsViewModel(carManager) as T
    else throw IllegalArgumentException("wrong view model")
}
