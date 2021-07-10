package dz.islem.automotive.ui.main

import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.capur16.digitspeedviewlib.DigitSpeedView
import com.github.jorgecastillo.FillableLoader
import dz.islem.automotive.data.CarManager
import dz.islem.automotive.ui.base.BaseViewModel
import kotlin.math.max

class MainViewModel(private val carManager: CarManager) : BaseViewModel() {
    init {
        carManager.init()
    }

    private val _fuelLevel = MutableLiveData(0f)
    val fuelLevel : LiveData<Float>
        get() = _fuelLevel

    private val _fuelLevelLow = MutableLiveData(false)
    val fuelLevelLow : LiveData<Boolean>
        get() = _fuelLevelLow

    private val _fuelDoor = MutableLiveData(false)
    val fuelDoor : LiveData<Boolean>
        get() = _fuelDoor

    private val _evChargePort = MutableLiveData(false)
    val evChargePort : LiveData<Boolean>
        get() = _evChargePort

    private val _speedCar = MutableLiveData(0)
    val speedCar : LiveData<Int>
        get() = _speedCar

    private val _gearSelected = MutableLiveData<String>()
    val gearSelected : LiveData<String>
        get() = _gearSelected

    private val _batteryLevel = MutableLiveData<Float>()
    val batteryLevel : LiveData<Float>
        get() = _batteryLevel

    fun loadFuelProperties(){
        compositeDisposable.addAll(
            carManager.fuelLevelObservable
                .subscribe { _fuelLevel.postValue(it) },
            carManager.fuelDoorObservable
                .subscribe { _fuelDoor.postValue(it) } ,
            carManager.evChargePortObservable
                .subscribe { _evChargePort.postValue(it) },
            carManager.fuelLevelLowObservable
                .subscribe { _fuelLevelLow.postValue(it) },
            carManager.batteryLevelObservable
                    .subscribe { _batteryLevel.postValue(it) }
        )
    }

    fun loadSpeedProperties(){
        compositeDisposable.addAll(
            carManager.speedCarObservable
                .subscribe { _speedCar.postValue(it) },
            carManager.gearSelectedObservable
                .subscribe { _gearSelected.postValue(it) }
        )
    }

    fun getFuelType() : String = carManager.getFuelType()

    override fun cleanUp() {
        carManager.cleanCallback()
        compositeDisposable.dispose()
    }

    fun getRemainingRange(): Int = carManager.getRemainingRange()

    companion object{
        @JvmStatic
        @BindingAdapter("fuelPercentage","batteryPercentage")
        fun bindFillPercentage(fillableLoader: FillableLoader, fuelPercentage: Float, batteryPercentage: Float) {
            fillableLoader.setPercentage(max(fuelPercentage,batteryPercentage))
        }

        @JvmStatic
        @BindingAdapter("setSpeed")
        fun bindSpeed(digitSpeedView: DigitSpeedView, speed: Int) {
            digitSpeedView.updateSpeed(speed)
        }
    }

}
