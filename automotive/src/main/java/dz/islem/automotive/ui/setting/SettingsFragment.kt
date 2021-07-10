package dz.islem.automotive.ui.setting

import android.car.Car
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import dz.islem.automotive.R
import dz.islem.automotive.data.CarManager
import dz.islem.automotive.ui.base.BasePreferenceFragment


class SettingsFragment : BasePreferenceFragment<SettingsViewModel>() {

    private val factory by lazy { SettingsViewModelFactory(carManager) }
    private val carManager by lazy { CarManager(car) }
    private val car by lazy { Car.createCar(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences_setting, rootKey)
        // update preferences
        findPreference<Preference>("preference_car_type")?.summary = viewModel.getCarType()
        findPreference<Preference>("preference_fuel_capacity")?.summary = resources.getString(R.string.fuel_level,viewModel.getFuelCapacity().toInt())
        findPreference<Preference>("preference_battery_capacity")?.summary = resources.getString(R.string.battery_level,viewModel.getBatteryCapacity().toInt())
        findPreference<Preference>("preference_manufacturer")?.summary = viewModel.getManufacture()
        findPreference<Preference>("preference_model")?.summary = viewModel.getModel()
        findPreference<Preference>("preference_model_year")?.summary = viewModel.getModelYear().toString()
        findPreference<Preference>("preference_outside_temperature")?.summary = resources.getString(R.string.outside_temperature,viewModel.getOutsideTemperature().toInt())
        findPreference<Preference>("preference_android_version")?.summary = viewModel.getAndroidVersion()
        findPreference<Preference>("preference_app_version")?.summary = viewModel.getAppVersion(requireContext())

        findPreference<SwitchPreference>("switch_night_mode")?.apply {
            isEnabled = false
            isChecked = viewModel.isNightModeOn()
            setOnPreferenceClickListener {
                val isChecked = (it as SwitchPreference).isChecked
                viewModel.setNightMode(isChecked)
                return@setOnPreferenceClickListener true
            }
        }

    }

    override fun createViewModel() = ViewModelProvider(this, factory).get(SettingsViewModel::class.java)

}
