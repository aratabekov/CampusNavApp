package com.ksunavigation.team.campusnavapp.routing;

/**
 * Created by Amir on 11/10/14.
 *
 * This is a QueueElement used in Graph class
 */
public class QueueElement implements Comparable<QueueElement> {

    private float distance;
    private String point;
    public QueueElement(String point, float distance){
        this.point=point;
        this.distance=distance;
    }
    public float getDistance(){
        return this.distance;
    }
    public String getPoint(){
        return this.point;
    }

    //
    public int compareTo(QueueElement other){

        return Float.compare(distance, other.distance);
    }
}
