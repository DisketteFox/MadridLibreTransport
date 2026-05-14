package dev.diskettefox.MadridLibreTransport.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class StopsModel {
    @SerializedName("code")
    private String code;
    @SerializedName("data")
    private ArrayList<Stops> stops = new ArrayList<>();
    public ArrayList<Stops> getStopsData() {
        return stops;
    }
    public String getCode() {
        return code;
    }

    public static class Stops {
        @SerializedName("node")
        private String stopId;
        @SerializedName("name")
        private String stopName;
        @SerializedName("geometry")
        private Geometry geometry;

        public String getStopName() {
            return stopName;
        }
        public String getStopId() {
            return stopId;
        }
        public Geometry getGeometry() {
            return geometry;
        }
    }

    public static class Geometry {
        @SerializedName("coordinates")
        private List<Double> coordinates;
        public List<Double> getCoordinates() {
            return coordinates;
        }
    }
}
