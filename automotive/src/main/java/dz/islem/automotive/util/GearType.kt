package dz.islem.automotive.util

enum class GearType(val type : Int) {
    UNKNOWN(0),
    N(1),
    R(2),
    P(4),
    D(8),
    D8(2048),
    D5(256),
    D1(16),
    D4(128),
    D9(4096),
    D2(32),
    D7(1024),
    D6(512),
    D3(64);

    companion object {
        fun forType(type: Int) : String = values().find{ it.type == type }?.name ?: values()[0].name
    }
}