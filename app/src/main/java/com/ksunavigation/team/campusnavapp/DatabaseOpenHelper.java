package com.ksunavigation.team.campusnavapp;

/**
 * Created by Amir on 10/22/14.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This creates/opens the database.
 */
public  class DatabaseOpenHelper extends SQLiteOpenHelper {

    private final Context mHelperContext;
    private SQLiteDatabase mDatabase;

    private static final String DATABASE_NAME = "Navigation.db";
    private static final int DATABASE_VERSION = 1;
    private static String DB_PATH = "";

    //private static final String FTS_VIRTUAL_TABLE = "BUILDING";
    private static final String TAG = "BuildingDatabase";
    //just a flag when there is need to re-deploy the database. Set true to copy the database even if it already exists

    //private static final boolean copy_database = true;

    private static final boolean replace_local_database = true;


    /* Note that FTS3 does not support column constraints and thus, you cannot
     * declare a primary key. However, "rowid" is automatically used as a unique
     * identifier, so when making requests, we will use "_id" as an alias for "rowid"
     */
        /*private static final String FTS_TABLE_CREATE =
                    "CREATE VIRTUAL TABLE " + FTS_VIRTUAL_TABLE +
                    " USING fts3 (" +
                            KEY_BUILDING + ", " +
                            KEY_DETAILS + ");";
        */
    DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mHelperContext = context;

        if(android.os.Build.VERSION.SDK_INT >= 17){
            DB_PATH = context.getApplicationInfo().dataDir + "/databases/";
        }
        else
        {
            DB_PATH = "/data/data/" + context.getPackageName() + "/databases/";
        }

    }
    private void copyDataBase() throws IOException
    {

        InputStream mInput = mHelperContext.getAssets().open(DATABASE_NAME);
        String outFileName = DB_PATH + DATABASE_NAME;
        OutputStream mOutput = new FileOutputStream(outFileName);
        byte[] mBuffer = new byte[1024];
        int mLength;
        while ((mLength = mInput.read(mBuffer))>0)
        {
            mOutput.write(mBuffer, 0, mLength);
        }
        mOutput.flush();
        mOutput.close();
        mInput.close();

    }

    public boolean databaseExists() {
        String fullPath = DB_PATH + DATABASE_NAME;
        SQLiteDatabase checkDB = null;

        Log.i("databaseExistsTAG", "Trying to connect to : " + fullPath);
        checkDB = SQLiteDatabase.openDatabase(fullPath, null,
                SQLiteDatabase.OPEN_READONLY);
        Log.i("databaseExistsTAG", "Database " + DATABASE_NAME + " found!");
        checkDB.close();

        return checkDB != null ? true : false;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("MapsActivity", "onCreate beginning now");
        if (replace_local_database | !databaseExists()) {
            try {
                Log.d("MapsActivity", "Copying now inside oncreate");
                //this.close();
                copyDataBase();

            } catch (Exception e) {
                Log.d("MapsActivity", "could not copy inside onCreate");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        if (replace_local_database | !databaseExists()) {
            Log.d("MapsActivity", "onCreate beginning now");
            try {

                Log.d("MapsActivity", "Copying now inside oncreate");
                //this.close();
                copyDataBase();

            } catch (Exception e) {
                Log.d("MapsActivity", "could not copy inside onCreate");
                e.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void close() {

        if(mDatabase != null)
            mDatabase.close();

        super.close();

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        //db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
        //onCreate(db);
    }
}
