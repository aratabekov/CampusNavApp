package com.ksunavigation.team.campusnavapp;

import com.ksunavigation.team.campusnavapp.routing.Point;

import java.util.List;

/**
 * Created by Marcel on 11-Nov-14.
 */
public class Building {
    private int id;
    private String name = null;
    private List<Point> points = null;

    public Building(int id, String name, List<Point> points) {
        this.id = id;
        this.name = name;
        this.points = points;
    }

    public String getName() {
        return name;
    }

    public List<Point> getPoints() {
        return points;
    }

    public int getId() { return id; }

    @Override
    public String toString() {
        return id + "|" + name;
    }
}
