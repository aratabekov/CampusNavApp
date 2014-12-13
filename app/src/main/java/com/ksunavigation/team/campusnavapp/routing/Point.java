package com.ksunavigation.team.campusnavapp.routing;

/**
 * Created by Amir on 10/23/14.
 *
 * This Point class represents a vertex
 */
public class Point {


    private double x;
    private double y;
    //private String name;
    private String value;
    public double getX()
    {
        return this.x;
    }
    public double getY()
    {
        return this.y;
    }
    public Point(double x, double y){
        this.x=x;
        this.y=y;
        //this.name=name;\

    }
    public void setOriginalString(String value){
        this.value=value;
    }
    public String getOriginalValue(){
        return this.value;
    }

    @Override
    public String toString(){

        return "["+x+":"+y+"]";
    }
}
