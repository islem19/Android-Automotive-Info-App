package dz.islem.automotive.ui.setting

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dz.islem.automotive.data.CarManager
import dz.islem.automotive.ui.base.BaseViewModel

const val ELECTRIC = "Electric"
const val HYBRID = "Hybrid"
const val FUEL = "Fuel"

class SettingsViewModel(private val carManager: CarManager) : BaseViewModel(){

    fun getFuelCapacity() = carManager.getFuelCapacity()

    fun getBatteryCapacity() = carManager.getBatteryCapacity()

    fun getCarType() = if (carManager.isCarElectric()) ELECTRIC else if (carManager.isCarHybrid()) HYBRID else FUEL

    fun getOutsideTemperature() = carManager.getOutsideTemperature()

    fun getManufacture() = carManager.getManufacturer()

    fun getModel() = carManager.getModel()

    fun getModelYear() = carManager.getModelYear()

    fun isNightModeOn() = carManager.getNightMode()

    fun setNightMode(isNightMode : Boolean) = carManager.setNightMode(isNightMode)

    fun getAndroidVersion() = Build.VERSION.RELEASE ?: "0"

    fun getAppVersion(context: Context) = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES).versionName ?: "0"

    override fun cleanUp() {
        compositeDisposable.dispose()
    }

}
