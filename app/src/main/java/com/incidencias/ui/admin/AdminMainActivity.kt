package com.incidencias.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.incidencias.databinding.ActivityAdminMainBinding
import com.incidencias.ui.admin.home.AdminHomeFragment

class AdminMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.adminFragmentContainer.id, AdminHomeFragment())
                .commit()
        }
    }
}