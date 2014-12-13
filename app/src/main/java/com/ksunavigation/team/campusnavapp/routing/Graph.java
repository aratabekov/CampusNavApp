package com.ksunavigation.team.campusnavapp.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Created by Amir on 10/21/14.
 */
public class Graph {

    /**
     * This variable defines the structure of the graph, the key contains
     * the String representation of a particular Vertex and value represents
     * its adjacent vertices. Please don't make it public
     */
    private HashMap<String,List<Point>> graph=null;

    /**
     * This variable is our lookup table, we can lookup any Vertex given its
     * String representation. Please don't make it public
     */
    private HashMap<String, Point> lookup = null;

    /**
     * This variable contains shortest distance from source to the Vertex given it's String representation
     * Please don't make it public
     */
    private HashMap<String, Float> dist;

    /**
     * This variable holds the string representation of the previous vertex given
     * the string representation of current vertex. Please don't make it public
     */
    private HashMap<String, String> previous;

    private List<List<Point>> polylines;


    public Graph(){
        lookup = new HashMap<String, Point>();
        graph=new HashMap<String, List<Point>>();
        dist=new HashMap<String, Float>();
        previous = new HashMap<String, String>();
        polylines=new ArrayList<List<Point>>();
    }

    /**
     * Adds a new edge to the graph
     * @param points which represent vertices of this edge
     */
    public void addEdge(List<Point> points){

        //
        polylines.add(points);

        for (int i = 0; i < points.size()-1; i++)
        {
            Point curPoint = points.get(i);
            Point nextPoint = points.get(i + 1);
            if (!lookup.containsKey(curPoint.toString()))
                lookup.put(curPoint.toString(), curPoint);

            if (graph.containsKey(curPoint.toString()))
            {
                graph.get(curPoint.toString()).add(nextPoint);
            }
            else
            {
                List<Point> list = new ArrayList<Point>();
                list.add(nextPoint);
                graph.put(curPoint.toString(), list);
            }
            if (graph.containsKey(nextPoint.toString()))
            {
                graph.get(nextPoint.toString()).add(curPoint);
            }
            else
            {
                List<Point> list = new ArrayList<Point>();
                list.add(curPoint);
                graph.put(nextPoint.toString(), list);
            }
        }
        if (!lookup.containsKey(points.get(points.size() - 1).toString()))
            lookup.put(points.get(points.size() - 1).toString(), points.get(points.size() - 1));
    }

    private HashMap<String, Float> dijkstra(String source, String target)
    {
        dist.clear();
        previous.clear();
        for(Map.Entry<String,List< Point >> pair : graph.entrySet() )
        {
            dist.put(pair.getKey(), Float.MAX_VALUE);
            previous.put(pair.getKey(), null);
        }

        PriorityQueue<QueueElement> Q=new PriorityQueue<QueueElement>();

        dist.put(source,  new Float(0));

        for (Map.Entry<String,Float> pair : dist.entrySet())
            Q.add(new QueueElement(pair.getKey(),  pair.getValue()));

        while (!Q.isEmpty())
        {
            String v = ((QueueElement)Q.poll()).getPoint();
            List<Point> list = graph.get(v);
            if (v.equals(target))
                break;
            //
            for (Point w : list)
            {
                float cost=distance(lookup.get(v), w);

                // if (dist[v] + cost < dist[w])
                if (cost < Float.MAX_VALUE && dist.get(v) + cost < dist.get(w.toString()))
                {
                    dist.put(w.toString(),dist.get(v) + cost) ;
                    previous.put(w.toString(),  v);
                    Q.add(new QueueElement(w.toString(), (float) dist.get(w.toString())));
                }
            }
        }
        return dist;
    }

    public List<Point[]> getEdges(){
        //return graph.values().toArray(new ArrayList<Point>());;
        List<Point[]> edges=new ArrayList<Point[]>();
        for (Map.Entry<String,List<Point>> pair : graph.entrySet()){
            edges.add(pair.getValue().toArray(new Point[0]));
        }

        return edges;
    }
    public List<List<Point>> getPolylines(){
        return this.polylines;
    }


    /**
     *
     * @param source
     * @param target
     * @param path this is where the list of points will be added that contains the shortest route
     * @return
     */
    public float getRoute(String source, String target, List<Point> path){



        HashMap<String, Float> distance = dijkstra(source, target);
        //List<Point> path = new ArrayList<Point>();

        String u = target;
        while (previous.get(u) != null)
        {
            path.add(lookup.get(u));
            u = previous.get(u);
        }

        path.add(lookup.get(source));

        return distance.get(target.toString())*1000;
        //return path;
    }

    /**
     * Returns an array of Vertices of the constructed graph
     * @return
     */
    public Point[] getVertices(){

        return lookup.values().toArray(new Point[0]);
    }

    /**
     * Find a distance in km using Haversine formula
     * @param p1
     * @param p2
     * @return
     */
    public static float distance(Point p1,Point p2)
    {
        int R = 6371; // km
        float dLat = toRad((float)(p2.getY() - p1.getY()));
        float dLon = toRad((float)(p2.getX() - p1.getX()));
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(toRad((float) p1.getY())) * Math.cos(toRad((float)p2.getY())) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        float d = (float)(R * c);
        return d;

    }
    private static float toRad(float degrees)
    {
        return (float)(degrees * Math.PI / (float)180);
    }

    public Point getClosestVertex(Point currentLocation, Point[] vertices) {
        float minDistance = Float.MAX_VALUE;
        Point closestVertex = null;

        for (int i=0;i<vertices.length;i++) {
            float distance = distance(currentLocation, vertices[i])*1000;
            if (distance < minDistance) {
                minDistance = distance;
                closestVertex = vertices[i];
            }
        }

        return closestVertex;
    }
    public boolean containsPoint(Point p){
        return lookup.containsKey(p.toString());
    }
}
