package dz.islem.automotive.ui.setting

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.car.ui.core.CarUi
import com.android.car.ui.toolbar.MenuItem
import com.android.car.ui.toolbar.Toolbar
import dz.islem.automotive.R
import java.util.*

class SettingsActivity : AppCompatActivity() {
    private val mMenuItems: ArrayList<MenuItem> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initSettingToolbar()
        // Display the fragment as the main content.
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(android.R.id.content, SettingsFragment())
                    .commitNow()
        }
    }

    private fun initSettingToolbar() {
        val toolbar = CarUi.requireToolbar(this)
        toolbar.setLogo(R.mipmap.ic_launcher)
        toolbar.state = Toolbar.State.SUBPAGE
        toolbar.title = title
        toolbar.registerOnBackListener {
            if (toolbar.state == Toolbar.State.SEARCH || toolbar.state == Toolbar.State.EDIT) {
                toolbar.state = Toolbar.State.SUBPAGE
                return@registerOnBackListener true
            }
            return@registerOnBackListener false
        }

        mMenuItems.add(MenuItem.builder(this)
                .setToSearch()
                .setOnClickListener { toolbar.state = Toolbar.State.SEARCH }
                .build())

        toolbar.setMenuItems(mMenuItems)

    }
}