package dz.islem.automotive.ui.base

import com.android.car.ui.preference.PreferenceFragment

abstract class BasePreferenceFragment<VM : BaseViewModel> : PreferenceFragment() {

    protected val viewModel : VM by lazy { createViewModel() }

    abstract fun createViewModel() : VM

    override fun onDestroy(){
        super.onDestroy()
        viewModel.cleanUp()
    }
}