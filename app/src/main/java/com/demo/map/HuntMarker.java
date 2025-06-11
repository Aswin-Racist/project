package com.demo.map;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.MapView;

public class HuntMarker extends Marker {

    private String type; // Example: "amenity", "shop", etc.
    private String name;

    public HuntMarker(MapView mapView) {
        super(mapView);
    }

    public HuntMarker(MapView mapView, GeoPoint position, String title, String snippet, String type, String name) {
        super(mapView);
        this.mPosition = position;
        this.mTitle = title;
        this.mSnippet = snippet;
        this.type = type;
        this.name = name;
        // You might want to set a custom icon here based on the type
        // setIcon(mapView.getContext().getResources().getDrawable(R.drawable.your_custom_marker_icon));
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // You can add more methods if needed, for example, to customize the InfoWindow
}