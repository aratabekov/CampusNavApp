package com.ksunavigation.team.campusnavapp;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

import java.util.HashMap;

/**
 * Created by Amir on 10/22/14.
 */
public class PathDatabase {

    // private static final String TAG = "BuildingDatabase";


    //public static final String KEY_BUILDING = SearchManager.SUGGEST_COLUMN_TEXT_1;
    //public static final String KEY_DETAILS = SearchManager.SUGGEST_COLUMN_TEXT_2;
    public static final String KEY_NAME = "NAME";
    public static final String KEY_POINTS = "POINTS";

    private static final String FTS_VIRTUAL_TABLE = "PATH";
    //private static final int DATABASE_VERSION = 2;

    private final DatabaseOpenHelper mDatabaseOpenHelper;
    private static final HashMap<String,String> mColumnMap = buildColumnMap();

    /**
     * Constructor
     * @param context The Context within which to work, used to create the DB
     */
    public PathDatabase(Context context) {
        mDatabaseOpenHelper = new DatabaseOpenHelper(context);
    }

    /**
     * Builds a map for all columns that may be requested, which will be given to the
     * SQLiteQueryBuilder. This is a good way to define aliases for column names, but must include
     * all columns, even if the value is the key. This allows the ContentProvider to request
     * columns w/o the need to know real column names and create the alias itself.
     */
    private static HashMap<String,String> buildColumnMap() {
        HashMap<String,String> map = new HashMap<String,String>();
        //map.put(KEY_BUILDING, KEY_BUILDING);
        //map.put(KEY_DETAILS, KEY_DETAILS);

        //map.put(KEY_BUILDING, KEY_BUILDING+','+KEY_BUILDING+ " AS "+ SearchManager.SUGGEST_COLUMN_TEXT_1);
        //map.put(KEY_DETAILS, KEY_DETAILS+','+ KEY_DETAILS+" AS "+SearchManager.SUGGEST_COLUMN_TEXT_2);
        map.put(KEY_NAME, KEY_NAME);
        map.put(KEY_POINTS, KEY_POINTS);

        map.put(BaseColumns._ID, "rowid AS " +
                BaseColumns._ID);
        map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS " +
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
        map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "rowid AS " +
                SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);


        return map;
    }





    public Cursor GetPaths(String[] columns) {
        /* The SQLiteBuilder provides a map for all possible columns requested to
         * actual columns in the database, creating a simple column alias mechanism
         * by which the ContentProvider does not need to know the real column names
         */
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE);
        builder.setProjectionMap(mColumnMap);

        //mDatabaseOpenHelper.getReadableDatabase().//
        Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),
                columns, null, null, null, null, null);

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }


}
