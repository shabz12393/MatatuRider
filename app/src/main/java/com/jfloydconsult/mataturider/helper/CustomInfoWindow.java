package com.jfloydconsult.mataturider.helper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.jfloydconsult.mataturider.R;

public class CustomInfoWindow implements GoogleMap.InfoWindowAdapter {

    View myView;

    public CustomInfoWindow(Context context) {
        myView= LayoutInflater.from(context)
                .inflate(R.layout.customer_rider_info_window, null);
    }

    @Override
    public View getInfoWindow(Marker marker) {
        TextView txtPickupTitle = myView.findViewById(R.id.customer_rider_info_window_txtPickupInfo);
        txtPickupTitle.setText(marker.getTitle());
        TextView txtPickupSnippet = myView.findViewById(R.id.customer_rider_info_window_txtPickupSnippet);
        txtPickupSnippet.setText(marker.getSnippet());

        return myView;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
