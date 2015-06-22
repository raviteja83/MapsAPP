package com.example.ravi_pc.mapapp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DirectionsJSONParser {
    public List<String> parse(JSONObject jObject) {
        JSONArray jDirections = null;
        try {
            jDirections = jObject.getJSONArray("routes");
            JSONArray jArray = jDirections.getJSONObject(0).getJSONArray("legs");
            JSONArray jsonArray = jArray.getJSONObject(0).getJSONArray("steps");
            return getPoints(jArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getPoints(JSONArray jsonArray){
        List<String> points = new ArrayList<>();
        for(int i=0;i<jsonArray.length();i++){
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return points;
    }
}
