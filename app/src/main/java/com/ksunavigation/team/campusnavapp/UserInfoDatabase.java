package com.ksunavigation.team.campusnavapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.util.Log;

import com.ksunavigation.team.campusnavapp.routing.Point;
import com.ksunavigation.team.campusnavapp.utils.ParserUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Marcel on 08-Nov-14.
 */
public class UserInfoDatabase {
    public static final String KEY_POINTS = "POINTS";
    private static final String FTS_VIRTUAL_TABLE_CAR = "CAR";
    public static final String BUILDING_ID = "BUILDING_ID";
    private static final String FTS_VIRTUAL_TABLE_HISTORY = "DESTINATION_HISTORY";
    //private static final int DATABASE_VERSION = 2;

    private final DatabaseOpenHelper mDatabaseOpenHelper;
    private static final HashMap<String,String> mColumnMapCar = buildColumnMapCar();
    private static final HashMap<String,String> mColumnMapHistory = buildColumnMapHistory();
    private final int maxSavedDestinations = 5; //maximum number of destinations to be saved

    /**
     * Constructor
     * @param context The Context within which to work, used to create the DB
     */
    public UserInfoDatabase(Context context) {
        mDatabaseOpenHelper = new DatabaseOpenHelper(context);
    }

    /**
     * Builds a map for all columns that may be requested, which will be given to the
     * SQLiteQueryBuilder. This is a good way to define aliases for column names, but must include
     * all columns, even if the value is the key. This allows the ContentProvider to request
     * columns w/o the need to know real column names and create the alias itself.
     */
    private static HashMap<String,String> buildColumnMapCar() {
        HashMap<String,String> map = new HashMap<String,String>();
        map.put(KEY_POINTS, KEY_POINTS);

        map.put(BaseColumns._ID, "rowid AS " +
                BaseColumns._ID);

        return map;
    }

    private static HashMap<String,String> buildColumnMapHistory() {
        HashMap<String,String> map = new HashMap<String,String>();
        map.put(BUILDING_ID, BUILDING_ID);
        //name
        map.put(BuildingDatabase.KEY_BUILDING, BuildingDatabase.KEY_BUILDING);
        //points
        map.put(BuildingDatabase.KEY_DETAILS, BuildingDatabase.KEY_DETAILS);

        map.put(BaseColumns._ID, "rowid AS " +
                BaseColumns._ID);


        map.put(BUILDING_ID,
                "BUILDING."+BaseColumns._ID);

        return map;
    }

    public void saveCarLocation(Point car) {
        SQLiteDatabase db = null;
        try {
            db = mDatabaseOpenHelper.getWritableDatabase();
            db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put(KEY_POINTS, car.getX() + "," + car.getY());

            if (getCarLocation() != null)
                db.update(FTS_VIRTUAL_TABLE_CAR, values,BaseColumns._ID + " = " + 1, null);
            else
                db.insert(FTS_VIRTUAL_TABLE_CAR, null, values);

            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            Log.e("ExceptionTAG", e.getMessage());
        } finally {
            db.endTransaction();
        }
    }

    public Point getCarLocation() {
        /* The SQLiteBuilder provides a map for all possible columns requested to
         * actual columns in the database, creating a simple column alias mechanism
         * by which the ContentProvider does not need to know the real column names
         */

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE_CAR);
        builder.setProjectionMap(mColumnMapCar);

        Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),
                null, null, null, null, null, null);

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }

        String point_str = cursor.getString(1);

        point_str = point_str.trim();
        String[] coordinates = point_str.split(",");
        String lon = coordinates[0];
        String lat = coordinates[1];

        double flon = Float.valueOf(lon);
        double flat = Float.valueOf(lat);

        return (new Point(flon, flat));
    }

    public int getOldestDestination() {
        ArrayList<Building> destinations = getSavedDestinations();

        Building lastDest = destinations.get(destinations.size() - 1);
        return lastDest.getId();
    }

    public boolean destinationExists(ArrayList<Building> destinations, int building_id) {
        for (Building b : destinations) {
            if (b.getId() == building_id)
                return true;
        }

        return false;
    }

    public void saveDestination(int building_id) {
        SQLiteDatabase db = null;

        try {
            db = mDatabaseOpenHelper.getWritableDatabase();
            db.beginTransaction();
            ArrayList<Building> destinations = getSavedDestinations();
            int count = destinations.size();

            if (destinationExists(destinations, building_id)) {
                db.delete(FTS_VIRTUAL_TABLE_HISTORY, BUILDING_ID + " = " + building_id, null);
                Log.i("UserInfoDatabase: ", "Deleted repeated " + building_id);
            }
            else {
                if (count >= maxSavedDestinations) {
                    int id_oldestDestination = getOldestDestination();
                    db.delete(FTS_VIRTUAL_TABLE_HISTORY,
                            BUILDING_ID + " = " + id_oldestDestination,
                            null);

                    Log.i("UserInfoDatabase: ", "Deleted oldest destination " + id_oldestDestination);
                }
            }

            ContentValues values = new ContentValues();
            values.put(BUILDING_ID, building_id);

            db.insert(FTS_VIRTUAL_TABLE_HISTORY, null, values);

            Log.i("UserInfoDatabase: ", "Inserted " + building_id);

            db.setTransactionSuccessful();

            Log.i("UserInfoDatabase: ", "Destinations now are: " + Arrays.toString(getSavedDestinations().toArray()));

        } catch (SQLiteException e) {
            Log.e("ExceptionTAG", e.getMessage());
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    public ArrayList<Building> getSavedDestinations() {
            /* The SQLiteBuilder provides a map for all possible columns requested to
             * actual columns in the database, creating a simple column alias mechanism
             * by which the ContentProvider does not need to know the real column names
             */

        ArrayList<Building> buildingsList = new ArrayList<Building>();
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE_HISTORY);
        builder.setProjectionMap(mColumnMapHistory);

        builder.setTables(FTS_VIRTUAL_TABLE_HISTORY + " INNER JOIN " + BuildingDatabase.FTS_VIRTUAL_TABLE +
                " ON " + BUILDING_ID + " = " + BuildingDatabase.FTS_VIRTUAL_TABLE + "." + BaseColumns._ID);

        String[] columns = new String[] {
                BUILDING_ID,                    //building id
                BuildingDatabase.KEY_BUILDING, //name
                BuildingDatabase.KEY_DETAILS, //points
        };

        Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),
                columns, null, null, null, null, null);

        while (cursor.moveToNext()) {
            int building_id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
            String name = cursor.getString(cursor.getColumnIndex(BuildingDatabase.KEY_BUILDING));
            String content = cursor.getString(cursor.getColumnIndex(BuildingDatabase.KEY_DETAILS));
            List<Point> list = ParserUtils.buildingPointsParser(content);

            buildingsList.add(new Building(building_id, name, list));
        }

        Collections.reverse(buildingsList);
        return buildingsList;
    }

    public Cursor getSavedHistoryCursor(){
        ArrayList<Building> buildingsList = new ArrayList<Building>();
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE_HISTORY);
        builder.setProjectionMap(mColumnMapHistory);

        builder.setTables(FTS_VIRTUAL_TABLE_HISTORY + " INNER JOIN " + BuildingDatabase.FTS_VIRTUAL_TABLE +
                " ON " + BUILDING_ID + " = " + BuildingDatabase.FTS_VIRTUAL_TABLE + "." + BaseColumns._ID);

        String[] columns = new String[] {
                BUILDING_ID,                    //building id
                BuildingDatabase.KEY_BUILDING, //name
                BuildingDatabase.KEY_DETAILS, //points
        };

        Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),
                columns, null, null, null, null, null);

        return cursor;
    }



}
