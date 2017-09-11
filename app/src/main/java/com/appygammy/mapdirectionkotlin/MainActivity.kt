package com.appygammy.mapdirectionkotlin

import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.PendingResult
import com.google.maps.android.PolyUtil
import com.google.maps.errors.ApiException
import com.google.maps.model.DirectionsResult
import com.google.maps.model.LatLng
import com.google.maps.model.TravelMode
import org.joda.time.DateTime
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val startLatLng = LatLng(22.5706075, 88.4355548)
    private val endLatLng = LatLng(22.512367, 88.128875)

    private fun LatLng.toAndroidLatLng(): com.google.android.gms.maps.model.LatLng {
        return com.google.android.gms.maps.model.LatLng(this.lat, this.lng)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initMap()

    }

    private fun initMap() {
        val fragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        fragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        if (googleMap != null)
            this.googleMap = googleMap

        setUpBounds()

        getDirectionsDetails(startLatLng, endLatLng, TravelMode.DRIVING)
    }

    private fun setUpBounds() {
        googleMap.setOnMapLoadedCallback {
            runOnUiThread {
                val builder = LatLngBounds.Builder()
                builder.include(startLatLng.toAndroidLatLng())
                builder.include(endLatLng.toAndroidLatLng())
                val llBounds = builder.build()

                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(llBounds, 50))
            }
        }
    }

    private fun getDirectionsDetails(origin: LatLng, destination: LatLng, mode: TravelMode) {
        val now = DateTime()
        try {
            val req = DirectionsApi.newRequest(getGeoContext())
                    .mode(mode)
                    .origin(origin)
                    .destination(destination)
                    .departureTime(now)

            req.setCallback(object : PendingResult.Callback<DirectionsResult> {
                override fun onResult(result: DirectionsResult?) {
                    runOnUiThread {
                        if (result != null) {
                            addPolyline(result)
                        }
                    }
                }

                override fun onFailure(e: Throwable?) {
                    runOnUiThread({
                        Toast.makeText(this@MainActivity, getString(R.string.no_direction), Toast.LENGTH_LONG).show()
                    })
                }
            }
            )
        } catch (e: ApiException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun addPolyline(results: DirectionsResult) {
        results.routes
                .map { PolyUtil.decode(it.overviewPolyline.encodedPath) }
                .map { googleMap.addPolyline(PolylineOptions().addAll(it)) }
                .forEach { it.color = ActivityCompat.getColor(this@MainActivity, R.color.black) }
    }

    private fun getGeoContext(): GeoApiContext {
        val geoApiContext = GeoApiContext()
        return geoApiContext
                .setQueryRateLimit(3)
                .setApiKey(getString(R.string.google_maps_key))
                .setConnectTimeout(1, TimeUnit.SECONDS)
                .setReadTimeout(1, TimeUnit.SECONDS)
                .setWriteTimeout(1, TimeUnit.SECONDS)
    }
}
