package dev.diskettefox.MadridLibreTransport.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.view.MapView;

import dev.diskettefox.MadridLibreTransport.R;

public class FragmentMap extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_map,container,false);
        MapView mapView = view.findViewById(R.id.map_view);

        LatLong center = new LatLong(40.416974, -3.703634);
        mapView.setCenter(center);

        return view;
    }
}