package com.incidencias.ui.technician

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.incidencias.R
import com.incidencias.databinding.ActivityTechnicianMainBinding
import com.incidencias.ui.settings.SettingsActivity
import com.incidencias.ui.technician.home.TechnicianHomeFragment

class TechnicianMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTechnicianMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTechnicianMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.technicianFragmentContainer, TechnicianHomeFragment())
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_technician_main, menu)
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