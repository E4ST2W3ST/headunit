package ca.yyx.hu.location;

import android.content.Context;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * @author algavris
 * @date 06/12/2016.
 */

public class GpsLocation implements GpsStatus.Listener, LocationListener {
    private final LocationManager mLocationManager;
    private GpsStatus mStatus = null;

    GpsLocation(Context context)
    {
        // Acquire a reference to the system Location Manager
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mLocationManager.addGpsStatusListener(this);

    }

    public void start()
    {
        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        mLocationManager.requestLocationUpdates(0, 0, criteria, this, null);
    }

    @Override
    public void onGpsStatusChanged(int event) {
        mStatus = mLocationManager.getGpsStatus(mStatus);
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                break;

            case GpsStatus.GPS_EVENT_FIRST_FIX:
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void stop() {
        mLocationManager.removeUpdates(this);
    }
}
