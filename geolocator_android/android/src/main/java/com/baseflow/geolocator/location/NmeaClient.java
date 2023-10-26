package com.baseflow.geolocator.location;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;

public class NmeaClient {

  public static final String NMEA_ALTITUDE_EXTRA = "geolocator_mslAltitude";
  public static final String NMEA_SATELLITES_IN_VIEW_EXTRA = "geolocator_satellites_in_view";
  public static final String PREFIX_GLOBAL_POSITION_FIX_DATA = "$GPGGA";
  public static final String PREFIX_GPS_SATELLITES_IN_VIEW = "$GPGSV";
  public static final String PREFIX_GLONASS_SATELLITES_IN_VIEW = "$GLGSV";
  public static final String PREFIX_QZSS_SATELLITES_IN_VIEW = "$QZGSV";
  public static final String PREFIX_BAIDU_IN_VIEW = "$BDGSV";
  public static final String PREFIX_GALILEO_IN_VIEW = "$GAGSV";
  private final Context context;
  private final LocationManager locationManager;
  @Nullable private final LocationOptions locationOptions;

  @TargetApi(Build.VERSION_CODES.N)
  private OnNmeaMessageListener nmeaMessageListener;

  private final ArrayList<String> rawNmeaMessages = new ArrayList<>();
  private boolean listenerAdded = false;

  long groupedTimeStamp = 0;

  public NmeaClient(@NonNull Context context, @Nullable LocationOptions locationOptions) {
    this.context = context;
    this.locationOptions = locationOptions;
    this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        nmeaMessageListener =
                (message, timestamp) -> {
                    if (message.startsWith(PREFIX_GLOBAL_POSITION_FIX_DATA)) {
                        //has a fix, create a new group of messages related to the fix.
                        groupedTimeStamp = timestamp;
                        rawNmeaMessages.clear();
                        rawNmeaMessages.add(message);
                    } else {
                        rawNmeaMessages.add(message);
                    }
                };
    }
  }

  @SuppressLint("MissingPermission")
  public void start() {
    if (listenerAdded) {
      return;
    }

    if (locationOptions != null && locationOptions.isUseMSLAltitude()) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && locationManager != null) {
        locationManager.addNmeaListener(nmeaMessageListener, null);
        listenerAdded = true;
      }
    }
  }

  public void stop() {
    if (locationOptions != null && locationOptions.isUseMSLAltitude()) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && locationManager != null) {
        locationManager.removeNmeaListener(nmeaMessageListener);
        listenerAdded = false;
      }
    }
  }

  public void enrichExtrasWithNmea(@Nullable Location location) {

    if (location == null) {
      return;
    }
    if (!rawNmeaMessages.isEmpty() && locationOptions != null && listenerAdded) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Double galileoSatellitesInView = Double.valueOf(rawNmeaMessages.stream().filter(c -> c.startsWith(PREFIX_GALILEO_IN_VIEW)).count());
            Double baiduSatellitesInView = Double.valueOf(rawNmeaMessages.stream().filter(c -> c.startsWith(PREFIX_BAIDU_IN_VIEW)).count());
            Double gpsSatellitesInView = Double.valueOf(rawNmeaMessages.stream().filter(c -> c.startsWith(PREFIX_GPS_SATELLITES_IN_VIEW)).count());
            Double qzssSatellitesInView = Double.valueOf(rawNmeaMessages.stream().filter(c -> c.startsWith(PREFIX_QZSS_SATELLITES_IN_VIEW)).count());
            Double glonassSatellitesInView = Double.valueOf(rawNmeaMessages.stream().filter(c -> c.startsWith(PREFIX_GLONASS_SATELLITES_IN_VIEW)).count());

            if (location.getExtras() == null) {
                location.setExtras(Bundle.EMPTY);
            }
            location.getExtras().putDouble(NMEA_SATELLITES_IN_VIEW_EXTRA, galileoSatellitesInView + baiduSatellitesInView + gpsSatellitesInView + qzssSatellitesInView + glonassSatellitesInView);
            String[] tokens = rawNmeaMessages.stream().filter(c -> c.startsWith(PREFIX_GLOBAL_POSITION_FIX_DATA)).findFirst().toString().split(",");
            if (tokens.length > 8 && !tokens[9].isEmpty()) {
                double mslAltitude = Double.parseDouble(tokens[9]);
                if (location.getExtras() == null) {
                    location.setExtras(Bundle.EMPTY);
                }
                location.getExtras().putDouble(NMEA_ALTITUDE_EXTRA, mslAltitude);
            }
        }
      }
    }
  }

