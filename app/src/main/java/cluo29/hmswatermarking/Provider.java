package cluo29.hmswatermarking;

/**
 * Created by Comet on 13/02/16.
 */
import java.util.HashMap;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

public class Provider extends ContentProvider {
    public static final int DATABASE_VERSION = 2;
    /**
     * Provider authority: cluo29.hmswatermarking.provider.hmswatermarking
     */
    public static String AUTHORITY = "cluo29.hmswatermarking.provider.hmswatermarking";
    //store watermarking testing data
    private static final int UNLOCK_MONITOR = 1;
    private static final int UNLOCK_MONITOR_ID = 2;
    //store signals
    private static final int UNLOCK_MONITOR2 = 3;
    private static final int UNLOCK_MONITOR2_ID = 4;

    public static final String DATABASE_NAME = "hmswatermarking.db";

    public static final String[] DATABASE_TABLES = {
            "plugin_hmswatermarking", "plugin_hmswatermarking2"
    };

    public static final class Unlock_Monitor_Data implements BaseColumns {
        private Unlock_Monitor_Data(){}

        public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/plugin_hmswatermarking");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.hmswatermarking";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.hmswatermarking";

        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String INTERVAL  = "double_interval";    //
        public static final String PROCESSING  = "double_processing";    //
        public static final String LABEL  = "label";    //

    }

    public static final class Unlock_Monitor_Data2 implements BaseColumns {
        private Unlock_Monitor_Data2(){}

        public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/plugin_hmswatermarking2");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.hmswatermarking2";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.hmswatermarking2";

        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String ORIGINALX  = "double_originalx";    //
        public static final String ORIGINALY  = "double_originaly";    //
        public static final String ORIGINALZ  = "double_originalz";    //
        public static final String MARKEDX  = "double_markedx";    //
        public static final String MARKEDY  = "double_markedy";    //
        public static final String MARKEDZ  = "double_markedz";    //
    }

    public static final String[] TABLES_FIELDS = {
            Unlock_Monitor_Data._ID + " integer primary key autoincrement," +
                    Unlock_Monitor_Data.TIMESTAMP + " real default 0," +
                    Unlock_Monitor_Data.INTERVAL + " real default 0," +
                    Unlock_Monitor_Data.PROCESSING + " real default 0," +
                    Unlock_Monitor_Data.LABEL + " text default ''," +
                    "UNIQUE("+ Unlock_Monitor_Data._ID+")",

            Unlock_Monitor_Data2._ID + " integer primary key autoincrement," +
                    Unlock_Monitor_Data2.TIMESTAMP + " real default 0," +
                    Unlock_Monitor_Data2.ORIGINALX + " real default 0," +
                    Unlock_Monitor_Data2.ORIGINALY + " real default 0," +
                    Unlock_Monitor_Data2.ORIGINALZ + " real default 0," +
                    Unlock_Monitor_Data2.MARKEDX + " real default 0," +
                    Unlock_Monitor_Data2.MARKEDY + " real default 0," +
                    Unlock_Monitor_Data2.MARKEDZ + " real default 0," +
                    "UNIQUE("+ Unlock_Monitor_Data2._ID+")"
    };

    private static UriMatcher URIMatcher;
    private static HashMap<String, String> databaseMap;
    private static HashMap<String, String> databaseMap2;
    private static DatabaseHelper databaseHelper;
    private static SQLiteDatabase database;

    @Override
    public boolean onCreate() {
        //AUTHORITY = getContext().getPackageName() + ".provider.template";

        URIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], UNLOCK_MONITOR);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", UNLOCK_MONITOR_ID);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[1], UNLOCK_MONITOR2);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[1] + "/#", UNLOCK_MONITOR2_ID);

        databaseMap = new HashMap<>();
        databaseMap.put(Unlock_Monitor_Data._ID, Unlock_Monitor_Data._ID);
        databaseMap.put(Unlock_Monitor_Data.TIMESTAMP, Unlock_Monitor_Data.TIMESTAMP);
        databaseMap.put(Unlock_Monitor_Data.INTERVAL, Unlock_Monitor_Data.INTERVAL);
        databaseMap.put(Unlock_Monitor_Data.PROCESSING, Unlock_Monitor_Data.PROCESSING);
        databaseMap.put(Unlock_Monitor_Data.LABEL, Unlock_Monitor_Data.LABEL);


        databaseMap2 = new HashMap<>();
        databaseMap2.put(Unlock_Monitor_Data2._ID, Unlock_Monitor_Data2._ID);
        databaseMap2.put(Unlock_Monitor_Data2.TIMESTAMP, Unlock_Monitor_Data2.TIMESTAMP);
        databaseMap2.put(Unlock_Monitor_Data2.ORIGINALX, Unlock_Monitor_Data2.ORIGINALX);
        databaseMap2.put(Unlock_Monitor_Data2.ORIGINALY, Unlock_Monitor_Data2.ORIGINALY);
        databaseMap2.put(Unlock_Monitor_Data2.ORIGINALZ, Unlock_Monitor_Data2.ORIGINALZ);
        databaseMap2.put(Unlock_Monitor_Data2.MARKEDX, Unlock_Monitor_Data2.MARKEDX);
        databaseMap2.put(Unlock_Monitor_Data2.MARKEDY, Unlock_Monitor_Data2.MARKEDY);
        databaseMap2.put(Unlock_Monitor_Data2.MARKEDZ, Unlock_Monitor_Data2.MARKEDZ);
        return true;
    }

    private boolean initializeDB() {

        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper( getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );
        }
        if( databaseHelper != null && ( database == null || ! database.isOpen() )) {
            database = databaseHelper.getWritableDatabase();
        }
        return( database != null && databaseHelper != null);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (URIMatcher.match(uri)) {
            case UNLOCK_MONITOR:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            case UNLOCK_MONITOR2:
                count = database.delete(DATABASE_TABLES[1], selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (URIMatcher.match(uri)) {
            case UNLOCK_MONITOR:
                return Unlock_Monitor_Data.CONTENT_TYPE;
            case UNLOCK_MONITOR_ID:
                return Unlock_Monitor_Data.CONTENT_ITEM_TYPE;
            case UNLOCK_MONITOR2:
                return Unlock_Monitor_Data2.CONTENT_TYPE;
            case UNLOCK_MONITOR2_ID:
                return Unlock_Monitor_Data2.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (!initializeDB()) {
            Log.w(AUTHORITY, "Database unavailable...");
            return null;
        }

        ContentValues values = (initialValues != null) ? new ContentValues(
                initialValues) : new ContentValues();

        switch (URIMatcher.match(uri)) {
            case UNLOCK_MONITOR:
                long weather_id = database.insert(DATABASE_TABLES[0], Unlock_Monitor_Data.LABEL, values);

                if (weather_id > 0) {
                    Uri new_uri = ContentUris.withAppendedId(
                            Unlock_Monitor_Data.CONTENT_URI,
                            weather_id);
                    getContext().getContentResolver().notifyChange(new_uri,
                            null);
                    return new_uri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            case UNLOCK_MONITOR2:
                long weather_id2 = database.insert(DATABASE_TABLES[1], Unlock_Monitor_Data2.ORIGINALX, values);

                if (weather_id2 > 0) {
                    Uri new_uri = ContentUris.withAppendedId(
                            Unlock_Monitor_Data2.CONTENT_URI,
                            weather_id2);
                    getContext().getContentResolver().notifyChange(new_uri,
                            null);
                    return new_uri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (URIMatcher.match(uri)) {
            case UNLOCK_MONITOR:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(databaseMap);
                break;
            case UNLOCK_MONITOR2:
                qb.setTables(DATABASE_TABLES[1]);
                qb.setProjectionMap(databaseMap2);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }
    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (URIMatcher.match(uri)) {
            case UNLOCK_MONITOR:
                count = database.update(DATABASE_TABLES[0], values, selection,
                        selectionArgs);
                break;
            case UNLOCK_MONITOR2:
                count = database.update(DATABASE_TABLES[1], values, selection,
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}