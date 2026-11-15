package dev.diskettefox.madridtransport

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.loadingindicator.LoadingIndicator
import dev.diskettefox.madridtransport.api.ApiCall
import dev.diskettefox.madridtransport.api.ApiInterface
import dev.diskettefox.madridtransport.fragments.FragmentLines
import dev.diskettefox.madridtransport.fragments.FragmentMap
import dev.diskettefox.madridtransport.fragments.FragmentStop
import dev.diskettefox.madridtransport.models.TokenModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    companion object {
        const val CLIENT_ID = "b0c015e3-7e51-401a-b69b-0c8bb6e6e55f"
        const val PASS_KEY = "A5C462BF6FD9BE59C286125F9A4101BA38071FCB1D0CDA03F205C0F07DA8CE2E7E5D1AB6A5A2829D60A7C2E3E7116E134B7BBCE782ACB70DC7FA880A294F4D75"

        const val NAV_STOPS = 0
        const val NAV_LINES = 1
        const val NAV_MAP = 2
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        getToken()
    }

    fun loadMain() {
        viewPager = findViewById(R.id.view_pager)
        val ld = findViewById<LoadingIndicator>(R.id.loading_indicator)
        bottomNav = findViewById(R.id.bottom_navigation)

        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 2
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val itemId = when (position) {
                    NAV_STOPS -> R.id.menu_stops
                    NAV_LINES -> R.id.menu_lines
                    NAV_MAP -> R.id.menu_maps
                    else -> R.id.menu_stops
                }
                if (bottomNav.selectedItemId != itemId) {
                    bottomNav.selectedItemId = itemId
                }
                viewPager.isUserInputEnabled = (position != NAV_MAP)
            }
        })

        bottomNav.setOnItemSelectedListener { menuItem ->
            val targetPosition = when (menuItem.itemId) {
                R.id.menu_stops -> NAV_STOPS
                R.id.menu_lines -> NAV_LINES
                R.id.menu_maps -> NAV_MAP
                else -> NAV_STOPS
            }

            val currentPosition = viewPager.currentItem
            val smoothScroll = true

            if (currentPosition != targetPosition) {
                viewPager.setCurrentItem(targetPosition, smoothScroll)
            }
            true
        }

        ld.visibility = View.GONE
        viewPager.visibility = View.VISIBLE
        bottomNav.visibility = View.VISIBLE
    }

    fun getToken() {
        val apiInterface = ApiCall.callApi().create(ApiInterface::class.java)

        val call = apiInterface.getToken(CLIENT_ID, PASS_KEY)
        call.enqueue(
            object : Callback<TokenModel> {
                override fun onResponse(call: Call<TokenModel>, response: Response<TokenModel>) {
                    if (response.isSuccessful && (response.body() != null)) {
                        val token = response.body()!!

                        if (token.code == 1 || token.code == 0) {
                            if (token.data.isNotEmpty()) {
                                val data = token.data[0]
                                Log.d("Token", data.accessToken)
                                ApiCall.setToken(data.accessToken)

                                loadMain()

                                val currentFragment = supportFragmentManager.findFragmentByTag("f0")
                                if (currentFragment is FragmentStop) {
                                    currentFragment.refreshFavorites()
                                    currentFragment.fetchAllStops()
                                }
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

    private class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                NAV_STOPS -> FragmentStop()
                NAV_LINES -> FragmentLines()
                NAV_MAP -> FragmentMap()
                else -> FragmentStop()
            }
        }
    }
}
