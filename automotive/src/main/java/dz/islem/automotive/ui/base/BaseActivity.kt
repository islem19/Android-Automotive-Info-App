package dz.islem.automotive.ui.base

import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity<VM : BaseViewModel> : AppCompatActivity() {
    protected val viewModel : VM by lazy { createViewModel() }

    abstract fun createViewModel() : VM

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cleanUp()
    }
}