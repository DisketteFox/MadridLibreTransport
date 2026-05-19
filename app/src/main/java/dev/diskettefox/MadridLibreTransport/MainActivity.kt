package dev.diskettefox.MadridLibreTransport

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.loadingindicator.LoadingIndicator
import dev.diskettefox.MadridLibreTransport.api.ApiCall
import dev.diskettefox.MadridLibreTransport.api.ApiInterface
import dev.diskettefox.MadridLibreTransport.fragments.FragmentLines
import dev.diskettefox.MadridLibreTransport.fragments.FragmentMap
import dev.diskettefox.MadridLibreTransport.fragments.FragmentStop
import dev.diskettefox.MadridLibreTransport.models.TokenModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    companion object {
        const val CLIENT_ID = "b0c015e3-7e51-401a-b69b-0c8bb6e6e55f"
        const val PASS_KEY = "A5C462BF6FD9BE59C286125F9A4101BA38071FCB1D0CDA03F205C0F07DA8CE2E7E5D1AB6A5A2829D60A7C2E3E7116E134B7BBCE782ACB70DC7FA880A294F4D75"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        getToken(savedInstanceState)
    }

    fun loadMain(savedInstanceState: Bundle?) {
        val fl = findViewById<FrameLayout>(R.id.frame_layout)
        val ld = findViewById<LoadingIndicator>(R.id.loading_indicator)
        val nv = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            loadFragment(FragmentStop(), booted = true)
        }

        // Navigation bar logic
        nv.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_stops -> {
                    loadFragment(FragmentStop(), booted = false)
                    true
                }
                R.id.menu_lines -> {
                    loadFragment(FragmentLines(), booted = false)
                    true
                }
                R.id.menu_maps -> {
                    loadFragment(FragmentMap(), booted = false)
                    true
                }
                else -> false
            }
        }

        ld.visibility = View.GONE
        fl.visibility = View.VISIBLE
        nv.visibility = View.VISIBLE
    }

    fun getToken(savedInstanceState: Bundle?) {
        val apiInterface = ApiCall.callApi().create(ApiInterface::class.java)

        val call = apiInterface.getToken(CLIENT_ID, PASS_KEY)
        call.enqueue(
            object : Callback<TokenModel> {
                override fun onResponse(call: Call<TokenModel>, response: Response<TokenModel>) {
                    if (response.isSuccessful && (response.body() != null)) {
                        val token = response.body()!!

                        if (token.code == 1 || token.code == 0) {
                            if (token.data != null && token.data.isNotEmpty()) {
                                val data = token.data[0]
                                Log.d("Token", data.accessToken)
                                ApiCall.setToken(data.accessToken)

                                // Notify existing fragment if it was restored from saved state
                                val currentFragment = supportFragmentManager.findFragmentById(R.id.frame_layout)
                                if (currentFragment is FragmentStop) {
                                    currentFragment.refreshFavorites()
                                    currentFragment.fetchAllStops()
                                }

                                loadMain(savedInstanceState)
                            }
                        }
                        if (token.code == 89) {
                            val intent = Intent(baseContext, Login::class.java)
                            startActivity(intent)
                        }

                    } else {
                        Log.e("API Error", "Unable to connect to database")
                    }
                }

                override fun onFailure(call: Call<TokenModel>, t: Throwable) {
                    Log.e("Call Error", "Error retrieving lines", t)
                }
            },
        )
    }

    // Fragment manager
    fun loadFragment(fragment: Fragment, booted: Boolean) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        if (booted) {
            fragmentTransaction.add(R.id.frame_layout, fragment)
        } else {
            fragmentTransaction.replace(R.id.frame_layout, fragment)
        }

        fragmentTransaction.commit()
    }
}
