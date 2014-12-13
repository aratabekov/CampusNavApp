/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ksunavigation.team.campusnavapp;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

import com.ksunavigation.team.campusnavapp.routing.Point;
import com.ksunavigation.team.campusnavapp.utils.ParserUtils;

import java.util.HashMap;
import java.util.List;

/**
 * Contains logic to return specific buildings from the buildings table, and
 * load the building table when it needs to be created.
 */
public class BuildingDatabase {
   // private static final String TAG = "BuildingDatabase";


    //public static final String KEY_BUILDING = SearchManager.SUGGEST_COLUMN_TEXT_1;
    //public static final String KEY_DETAILS = SearchManager.SUGGEST_COLUMN_TEXT_2;
    public static final String KEY_BUILDING = "NAME";
    public static final String KEY_DETAILS = "POINTS";

    public static final String FTS_VIRTUAL_TABLE = "BUILDING";
    //private static final int DATABASE_VERSION = 2;

    private final DatabaseOpenHelper mDatabaseOpenHelper;
    private static final HashMap<String,String> mColumnMap = buildColumnMap();

    /**
     * Constructor
     * @param context The Context within which to work, used to create the DB
     */
    public BuildingDatabase(Context context) {
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

        map.put(KEY_BUILDING, KEY_BUILDING+','+KEY_BUILDING+ " AS "+SearchManager.SUGGEST_COLUMN_TEXT_1);
        //map.put(KEY_DETAILS, KEY_DETAILS+','+ KEY_DETAILS+" AS "+SearchManager.SUGGEST_COLUMN_TEXT_2);
        map.put(KEY_DETAILS, KEY_DETAILS);

        map.put(BaseColumns._ID, "rowid AS " +
                BaseColumns._ID);
        map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS " +
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
        map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "rowid AS " +
                SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);


        return map;
    }

    /**
     * Returns a Cursor positioned at the building specified by rowId
     *
     * @param rowId id of building to retrieve
     * @param columns The columns to include, if null then all are included
     * @return Cursor positioned to matching word, or null if not found.
     */
    public Cursor getBuilding(String rowId, String[] columns) {
        String selection = "rowid = ?";
        String[] selectionArgs = new String[] {rowId};

        return query(selection, selectionArgs, columns);

        /* This builds a query that looks like:
         *     SELECT <columns> FROM <table> WHERE rowid = <rowId>
         */
    }

    public Building getBuildingById(String rowId){
        String[] columns = new String[] {
                BaseColumns._ID,
                BuildingDatabase.KEY_BUILDING,
                BuildingDatabase.KEY_DETAILS};

        //BuildingDatabase db=new BuildingDatabase(getAppli)
        Cursor cursor= getBuilding(rowId,columns);
        cursor.moveToPosition(-1);
        //String r="";
        if(cursor!=null) {
            if (cursor.moveToNext()) {

                int building_id = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
                String name = cursor.getString(cursor.getColumnIndex(BuildingDatabase.KEY_BUILDING));
                String content = cursor.getString(cursor.getColumnIndex(BuildingDatabase.KEY_DETAILS));
                List<Point> list = ParserUtils.buildingPointsParser(content);

                return new Building(building_id,name,list);


            }
        }
        return null;
    }

    /**
     * Returns a Cursor over all buildings that match the given query
     *
     * @param query The string to search for
     * @param columns The columns to include, if null then all are included
     * @return Cursor over all words that match, or null if none found.
     */
    public Cursor getBuildingMatches(String query, String[] columns) {
        String selection = KEY_BUILDING + " MATCH ?";
        String[] selectionArgs = new String[] {query+"*"};

        return query(selection, selectionArgs, columns);

        /* This builds a query that looks like:
         *     SELECT <columns> FROM <table> WHERE <KEY_BUILDING> MATCH 'query*'
         * which is an FTS3 search for the query text (plus a wildcard) inside the building name column.
         *
         * - "rowid" is the unique id for all rows but we need this value for the "_id" column in
         *    order for the Adapters to work, so the columns need to make "_id" an alias for "rowid"
         * - "rowid" also needs to be used by the SUGGEST_COLUMN_INTENT_DATA alias in order
         *   for suggestions to carry the proper intent data.
         *   These aliases are defined in the BuildingProvider when queries are made.
         * - This can be revised to also search the definition text with FTS3 by changing
         *   the selection clause to use FTS_VIRTUAL_TABLE instead of KEY_BUILDING (to search across
         *   the entire table, but sorting the relevance could be difficult.
         */
    }

    /**
     * Performs a database query.
     * @param selection The selection clause
     * @param selectionArgs Selection arguments for "?" components in the selection
     * @param columns The columns to return
     * @return A Cursor over all rows matching the query
     */
    private Cursor query(String selection, String[] selectionArgs, String[] columns) {
        /* The SQLiteBuilder provides a map for all possible columns requested to
         * actual columns in the database, creating a simple column alias mechanism
         * by which the ContentProvider does not need to know the real column names
         */
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE);
        builder.setProjectionMap(mColumnMap);

        Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),
                columns, selection, selectionArgs, null, null, null);

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }




}
