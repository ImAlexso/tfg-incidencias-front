package com.incidencias.ui.manager

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.incidencias.R
import com.incidencias.databinding.ActivityManagerMainBinding
import com.incidencias.ui.manager.home.ManagerHomeFragment
import com.incidencias.ui.settings.SettingsActivity

class ManagerMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagerMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.managerFragmentContainer, ManagerHomeFragment())
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_manager_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }
}