package com.ksunavigation.team.campusnavapp.utils;

import com.ksunavigation.team.campusnavapp.routing.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marcel on 11-Nov-14.
 */
public class ParserUtils {

    public static List<Point> buildingPointsParser(String points_str) {

        String[] points = points_str.split(" ");

        List<Point> pointsList = new ArrayList<Point>();
        for (int i = 0; i < points.length; i++) {
            points[i] = points[i].trim();
            if (points[i].length() != 0) {
                String[] coordinates = points[i].split(",");
                String lon = coordinates[0];
                String lat = coordinates[1];
                if (lat.trim().length() == 0 | lon.trim().length() == 0) {
                    //Popup(("happened"));
                    continue;
                } else {
                    double flon = Float.valueOf(lon);
                    double flat = Float.valueOf(lat);

                    Point point = new Point(flon, flat);
                    point.setOriginalString(lon+":"+lat);
                    pointsList.add(point);
                }
            }
        }
        return pointsList;
    }
}