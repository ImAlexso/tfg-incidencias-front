package com.incidencias.ui.user

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.incidencias.R
import com.incidencias.databinding.ActivityUserMainBinding
import com.incidencias.ui.user.home.UserHomeFragment

class UserMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.userFragmentContainer, UserHomeFragment())
                .commit()
        }
    }

    fun setToolbarTitle(title: String) {
        binding.toolbar.title = title
    }
}