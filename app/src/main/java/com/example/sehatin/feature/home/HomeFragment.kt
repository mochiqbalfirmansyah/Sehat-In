package com.example.sehatin.feature.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.sehatin.R
import com.example.sehatin.application.base.BaseFragment
import com.example.sehatin.databinding.FragmentHomeBinding
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sehatin.application.data.response.RecipesResponse
import com.example.sehatin.feature.adapter.RecipesAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(), RecipesAdapter.OnItemClickListener {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var recipeAdapter: RecipesAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val TAG = "HomeFragment"
    }
    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        attachToParent: Boolean
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, attachToParent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setUpViews()
        setupObserver()
        getMyLastLocation()
    }



//    override fun setupNavigation() {
//        auth = Firebase.auth
//        if (auth.currentUser == null) {
//            findNavController().navigate(R.id.auth)
//        }
//    }

    override fun setUpViews() {

        binding.fabScan.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_scanFragment)

        }
        binding.recyclerViewArticles.layoutManager = LinearLayoutManager(requireContext())

    }
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false -> {
                    getMyLastLocation()
                }
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false -> {
                    getMyLastLocation()
                }
                else -> {
                    Toast.makeText(requireContext(), "Location access denied.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getMyLastLocation() {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latitude = it.latitude
                    val longitude = it.longitude
                    Log.d(TAG, "Latitude: $latitude, Longitude: $longitude")
                    fetchWeatherData(latitude, longitude)
                } ?: run {
                    Toast.makeText(requireContext(), "Unable to get location.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }




    private fun fetchWeatherData(latitude: Double, longitude: Double){
        val apiKey = getString(R.string.apikeyWeather)
        viewModel.fetchWeather(latitude,longitude,apiKey)
    }

    override fun setupObserver() {
        viewModel.weatherData.observe(viewLifecycleOwner, Observer { weather ->
            weather?.let {
                binding.textCityName.text = weather.name
                binding.textWeatherDescription.text = weather.weather[0].description
                val temperatureKelvin = weather.main.temp as Double
                val temperatureCelsius = (temperatureKelvin - 273.15).toInt()
                binding.textTemperature.text = "$temperatureCelsius°C"

                Glide.with(requireContext())
                    .load("https://openweathermap.org/img/w/${weather.weather[0].icon}.png")
                    .into(binding.imageWeatherIcon)
            }
        })

        viewModel.recipesData.observe(viewLifecycleOwner, Observer { recipesResponse ->
            recipesResponse?.let { recipes ->
                setupRecyclerView(recipes)

            }
        })
        val type = getString(R.string.type)
        val appId = getString(R.string.appIdRecipe)
        val appKey = getString(R.string.appKeyRecipe)
        val search = getString(R.string.search)
        viewModel.fetchRecipes(type,appId,appKey,search)
    }

    private fun setupRecyclerView(recipes: RecipesResponse) {
        val recipesList = recipes.hits
        recipeAdapter = RecipesAdapter(recipesList,this)
        binding.recyclerViewArticles.adapter = recipeAdapter
    }
    override fun onItemClick(url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(context, "No browser available to open the URL", Toast.LENGTH_SHORT).show()
        }
    }

}

