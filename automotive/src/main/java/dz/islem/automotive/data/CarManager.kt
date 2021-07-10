package dz.islem.automotive.data

import android.car.Car
import android.car.VehicleAreaType
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import androidx.annotation.RequiresPermission
import dz.islem.automotive.util.FuelType
import dz.islem.automotive.util.GearType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class CarManager(private val car : Car) {
    private val carPropertyManager by lazy { car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager }

    private val fuelLevel : Subject<Float> by lazy { PublishSubject.create() }
    val fuelLevelObservable : Observable<Float>
        get() = fuelLevel

    private val fuelDoor : Subject<Boolean> by lazy { PublishSubject.create() }
    val fuelDoorObservable : Observable<Boolean>
        get() = fuelDoor

    private val evChargePort : Subject<Boolean> by lazy { PublishSubject.create() }
    val evChargePortObservable : Observable<Boolean>
        get() = evChargePort

    private val fuelLevelLow : Subject<Boolean> by lazy { PublishSubject.create() }
    val fuelLevelLowObservable : Observable<Boolean>
        get() = fuelLevelLow

    private val speedCar : Subject<Int> by lazy { PublishSubject.create() }
    val speedCarObservable : Observable<Int>
        get() = speedCar

    private val gearSelected : Subject<String> by lazy { PublishSubject.create() }
    val gearSelectedObservable : Observable<String>
        get() = gearSelected

    private val batteryLevel : Subject<Float> by lazy { PublishSubject.create() }
    val batteryLevelObservable : Observable<Float>
        get() = batteryLevel

    @RequiresPermission(allOf = [
        "android.car.permission.CAR_POWERTRAIN",
        "android.car.permission.CAR_SPEED",
        "android.car.permission.CAR_ENERGY",
        "android.car.permission.CAR_ENERGY_PORTS"])
    fun init(){
        carPropertyManager.registerCallback(carPropertyCallback, VehiclePropertyIds.FUEL_LEVEL, CarPropertyManager.SENSOR_RATE_NORMAL)
        carPropertyManager.registerCallback(carPropertyCallback, VehiclePropertyIds.FUEL_DOOR_OPEN, CarPropertyManager.SENSOR_RATE_NORMAL)
        carPropertyManager.registerCallback(carPropertyCallback, VehiclePropertyIds.FUEL_LEVEL_LOW, CarPropertyManager.SENSOR_RATE_NORMAL)
        carPropertyManager.registerCallback(carPropertyCallback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL)
        carPropertyManager.registerCallback(carPropertyCallback, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_NORMAL)
        carPropertyManager.registerCallback(carPropertyCallback, VehiclePropertyIds.EV_BATTERY_LEVEL, CarPropertyManager.SENSOR_RATE_NORMAL)
        carPropertyManager.registerCallback(carPropertyCallback, VehiclePropertyIds.EV_CHARGE_PORT_OPEN, CarPropertyManager.SENSOR_RATE_NORMAL)
    }

    fun cleanCallback(){
        carPropertyManager.unregisterCallback(carPropertyCallback)
    }

    private val carPropertyCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(carPropertvalue: CarPropertyValue<*>?) {
            when(carPropertvalue?.propertyId){
                VehiclePropertyIds.FUEL_LEVEL -> {
                    val fuelPercent = (carPropertvalue.value as Float) * 100 / getFuelCapacity()
                    fuelLevel.onNext(fuelPercent)
                }
                VehiclePropertyIds.EV_BATTERY_LEVEL -> {
                    val batteryPercent = (carPropertvalue.value as Float) * 100 / getBatteryCapacity()
                    batteryLevel.onNext(batteryPercent)
                }
                VehiclePropertyIds.FUEL_DOOR_OPEN -> fuelDoor.onNext(carPropertvalue.value as Boolean)
                VehiclePropertyIds.EV_CHARGE_PORT_OPEN -> evChargePort.onNext(carPropertvalue.value as Boolean)
                VehiclePropertyIds.FUEL_LEVEL_LOW -> fuelLevelLow.onNext(carPropertvalue.value as Boolean)
                VehiclePropertyIds.PERF_VEHICLE_SPEED -> {
                    // convert speed from sensor (m/s) to (km/h)
                    val speedKmH = (carPropertvalue.value as Float) * 3600 / 1000
                    speedCar.onNext(speedKmH.toInt())
                }
                VehiclePropertyIds.GEAR_SELECTION -> {
                    val gear = GearType.forType(carPropertvalue.value as Int)
                    gearSelected.onNext(gear)
                }
            }
        }

        override fun onErrorEvent(propertyId: Int, zone: Int) = Unit
    }

    @RequiresPermission(allOf = ["android.car.permission.CAR_INFO"])
    fun getFuelType() : String {
        var fuel = ""
        val fuelTypes = getCarPropertyValue<Array<Int>>(VehiclePropertyIds.INFO_FUEL_TYPE,VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL)
        fuelTypes.forEach {
            fuel = "$fuel ${FuelType.values()[it].name}"
        }
        return fuel
    }

    @RequiresPermission(allOf = ["android.car.permission.CAR_ENERGY"])
    fun getRemainingRange() : Int = getCarPropertyValue(VehiclePropertyIds.RANGE_REMAINING,VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL)

    @RequiresPermission(allOf = ["android.car.permission.CAR_INFO"])
    fun getFuelCapacity() : Float{
        return getCarPropertyValue(VehiclePropertyIds.INFO_FUEL_CAPACITY,VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL)
    }

    @RequiresPermission(allOf = ["android.car.permission.CAR_INFO"])
    fun getBatteryCapacity() : Float{
        return getCarPropertyValue(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY,VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL)
    }

    fun isCarElectric() = getFuelType().trimStart(' ') == FuelType.ELECTRIC.name

    fun isCarHybrid() = getFuelType().split(" ").size > 2 && getFuelType().contains(FuelType.ELECTRIC.name)

    @RequiresPermission(allOf = ["android.car.permission.CAR_EXTERIOR_ENVIRONMENT"])
    fun getOutsideTemperature() : Float {
        return getCarPropertyValue(VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE,VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL)
    }

    @RequiresPermission(allOf = ["android.car.permission.CAR_INFO"])
    fun getManufacturer() : String{
        return getCarPropertyValue(VehiclePropertyIds.INFO_MAKE,VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL)
    }

    @RequiresPermission(allOf = ["android.car.permission.CAR_INFO"])
    fun getModel() : String{
        return getCarPropertyValue(VehiclePropertyIds.INFO_MODEL,VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL)
    }

    @RequiresPermission(allOf = ["android.car.permission.CAR_INFO"])
    fun getModelYear() : Int{
        return getCarPropertyValue(VehiclePropertyIds.INFO_MODEL_YEAR,VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL)
    }

    @RequiresPermission(allOf = ["android.car.permission.CAR_EXTERIOR_ENVIRONMENT"])
    fun getNightMode() : Boolean {
        return getCarPropertyValue(VehiclePropertyIds.NIGHT_MODE, VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL)
    }

    @RequiresPermission(allOf = ["android.car.permission.CAR_EXTERIOR_ENVIRONMENT"])
    fun setNightMode( isNightMode : Boolean){
        carPropertyManager.setBooleanProperty(VehiclePropertyIds.NIGHT_MODE,VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL,isNightMode)
    }

    private inline fun <reified T> getCarPropertyValue(carPropertyId : Int, carAreaType: Int) : T{
        return carPropertyManager.getProperty<T>(carPropertyId,carAreaType).value
    }

}