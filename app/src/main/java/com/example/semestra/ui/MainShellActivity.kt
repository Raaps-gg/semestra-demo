package com.example.semestra.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.semestra.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainShellActivity : AppCompatActivity() {

    private val shellViewModel: MainShellViewModel by viewModels()

    private val homeFragment = HomeCalendarFragment()
    private val uploadFragment = UploadFragment()
    private val classesFragment = ClassesFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_shell)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.mainContentContainer, settingsFragment, TAG_SETTINGS).hide(settingsFragment)
                .add(R.id.mainContentContainer, classesFragment, TAG_CLASSES).hide(classesFragment)
                .add(R.id.mainContentContainer, uploadFragment, TAG_UPLOAD).hide(uploadFragment)
                .add(R.id.mainContentContainer, homeFragment, TAG_HOME)
                .commit()
        }

        findViewById<BottomNavigationView>(R.id.bottomNavigationMain).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> show(TAG_HOME)
                R.id.nav_upload -> show(TAG_UPLOAD)
                R.id.nav_classes -> show(TAG_CLASSES)
                R.id.nav_settings -> show(TAG_SETTINGS)
            }
            true
        }
    }

    fun openHomeWithClassFilter(className: String?) {
        shellViewModel.setClassFilter(className)
        findViewById<BottomNavigationView>(R.id.bottomNavigationMain).selectedItemId = R.id.nav_home
    }

    private fun show(tagToShow: String) {
        val tx = supportFragmentManager.beginTransaction()
        val mapping = listOf(TAG_HOME, TAG_UPLOAD, TAG_CLASSES, TAG_SETTINGS)
        mapping.forEach { tag ->
            val fragment = supportFragmentManager.findFragmentByTag(tag)
            if (fragment != null) {
                if (tag == tagToShow) tx.show(fragment) else tx.hide(fragment)
            }
        }
        tx.commit()
    }

    private companion object {
        private const val TAG_HOME = "home"
        private const val TAG_UPLOAD = "upload"
        private const val TAG_CLASSES = "classes"
        private const val TAG_SETTINGS = "settings"
    }
}
