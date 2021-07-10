package dz.islem.automotive.ui.main

import android.car.Car
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.android.car.ui.core.CarUi
import com.android.car.ui.toolbar.MenuItem
import dz.islem.automotive.R
import dz.islem.automotive.data.CarManager
import dz.islem.automotive.databinding.MainActivityBinding
import dz.islem.automotive.ui.base.BaseActivity
import dz.islem.automotive.ui.setting.SettingsActivity
import dz.islem.automotive.util.Path
import kotlinx.android.synthetic.main.main_activity.*


class MainActivity : BaseActivity<MainViewModel>() {
    private val car by lazy { Car.createCar(this) }
    private val factory by lazy { MainViewModelFactory(carManager) }
    private val carManager by lazy { CarManager(car) }
    lateinit var binding: MainActivityBinding
    /**
     * https://medium.com/siili-automotive/first-steps-with-android-automotive-car-api-374d995c859c
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.main_activity)
        binding.viewModel = viewModel
        binding.activity = this
        binding.lifecycleOwner = this

        requestPermission()
        initCarToolbar()
        initFuelLevelView()
        viewModel.loadFuelProperties()
        viewModel.loadSpeedProperties()
        carManager.getRemainingRange()

    }

    private fun requestPermission() {
        requestPermissions(arrayOf(
            "android.car.permission.CAR_EXTERIOR_ENVIRONMENT",
            "android.car.permission.CAR_POWERTRAIN",
            "android.car.permission.CAR_SPEED",
            "android.car.permission.CAR_ENERGY",
            "android.car.permission.CAR_ENERGY_PORTS",
            "android.car.permission.CAR_INFO"), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Log.e("TAG", "onRequestPermissionsResult: granted")

            } else {
                Log.e("TAG", "onRequestPermissionsResult: denied")
            }
            return
        }
    }

    private fun initCarToolbar() {
        val toolbar = CarUi.requireToolbar(this)
        toolbar.setLogo(R.mipmap.ic_launcher)
        toolbar.title = title
        toolbar.setMenuItems(R.xml.main_menu_items)
    }

    fun settingMenuItemClicked(item: MenuItem) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun initFuelLevelView() {
        fillableLoader.setSvgPath(Path.CAR)
        fillableLoader.postDelayed({ fillableLoader.start() }, 50)
    }

    fun onFuelInfoClick(fuelLevel: Float, batteryLevel: Float){
        val fuelType = viewModel.getFuelType()
        val remainingRange = viewModel.getRemainingRange()
        Toast.makeText(this, resources.getString(R.string.fuel_message, fuelType, fuelLevel.toInt(), batteryLevel.toInt(), remainingRange), Toast.LENGTH_SHORT).show()
    }

    override fun createViewModel(): MainViewModel = ViewModelProvider(this, factory).get(MainViewModel::class.java)


}