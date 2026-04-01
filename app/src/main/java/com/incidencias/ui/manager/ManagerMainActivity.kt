package com.incidencias.ui.manager

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.incidencias.databinding.ActivityManagerMainBinding
import com.incidencias.ui.manager.home.ManagerHomeFragment

class ManagerMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagerMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.managerFragmentContainer.id, ManagerHomeFragment())
                .commit()
        }
    }

    fun setToolbarTitle(title: String) {
        binding.toolbar.title = title
    }
}