package com.incidencias.ui.technician

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.incidencias.R
import com.incidencias.databinding.ActivityTechnicianMainBinding
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

    fun setToolbarTitle(title: String) {
        binding.toolbar.title = title
    }
}