package com.example.falschefreunde;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * As reference for this class was the example form the course used and modified
 * Class to create database and modify it with several functions
 */
public class DatabaseAccess extends SQLiteOpenHelper {

    /**
     * Name of the database
     */
    private final SQLiteDatabase db;

    /**
     * SQL-Command to create the table
     */
    private final String tableSQL;

    /**
     * Name of the table
     */
    private String table;

    /**
     * Constructor
     * @param activity:     activity that calls database
     * @param dbname:       name of the database (will get created when it doesn't exist)
     * @param SQLCommand:   SQL-Command to create the table
     */
    public DatabaseAccess(Context activity, String dbname, String SQLCommand){
        super(activity, dbname, null, 1);
        this.tableSQL = SQLCommand;
        getTableName();
        db = this.getWritableDatabase();
        onCreate(db);
    }


    /**
     * Get name of the table out of the SQL-Command to create a new table
     */
    private void getTableName(){
        String sql = tableSQL.toUpperCase();
        StringTokenizer tokenizer = new StringTokenizer(sql);

        while(tokenizer.hasMoreTokens()){
            String token = tokenizer.nextToken();

            if(token.equals("TABLE")){
                table = tokenizer.nextToken();
                break;
            }
        }
    }


    /**
     * Gets only called when database gets created
     * @param db: database that should get created
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        try{
            db.execSQL(tableSQL);
            Log.d("Database", "Database got created");
        }
        catch(Exception ex){
            Log.e("Database", ex.getMessage());
        }
    }


    /**
     * Creates a new database when the version of it changed
     * Gets called automatically
     * @param db:           database that got upgraded
     * @param oldversion:   old version number
     * @param newversion:   new version number
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldversion, int newversion) {
        db.execSQL("DROP TABLE IF EXISTS " + table);
        onCreate(db);
    }


    /**
     * Gets cursor with all database entries, (ordered by time)
     * @return cursor
     */
    public Cursor createListViewCursor(){
        String[] columns = new String[]{"_id", "name", "time"};
        return db.query(table, columns, null, null, null, null, null );
    }


    /**
     * Gets all datasets
     * @return list of datasets
     */
    public List<Dataset> readDatasets(){
        List<Dataset> list =new ArrayList<Dataset>();
        Cursor cursor = null;

        try{
            cursor = db.query(table, null, null, null, null, null, null);
            int count = cursor.getCount();
            cursor.moveToFirst();

            for(int i = 0; i < count; i++){
                Dataset ds = createDataset(cursor);
                list.add(ds);
                cursor.moveToNext();
            }
        }
        catch(Exception ex){
            Log.e("Table", ex.getMessage());
        }

        finally{
            if(cursor != null && !cursor.isClosed()){
                cursor.close();
            }
        }

        return list;
    }


    /**
     * Insert dataset into the database
     * @param ds:   dataset that should be inserted into the database
     * @return      returns value, if insert was successful or not
     */
    public long insertDataset( Dataset ds){
        try{
            ContentValues data = createDatasetObject(ds);
            return db.insert(table, null, data);
        }
        catch(Exception ex){
            Log.d("Insert", ex.getMessage());
            return -1;
        }
    }


    /**
     * Read data from cursor and create a dataset out of it
     * @param cursor:   cursor with its current data
     * @return          dataset that got created
     */
    private Dataset createDataset(Cursor cursor){
        Dataset ds = new Dataset();
        ds.id = cursor.getLong(0);
        ds.name = cursor.getString(1);
        ds.time = cursor.getString(2);

        return ds;
    }


    /**
     * creates a SQLite object
     * @param dataset:  datasetobject whos values are needed to create the return value
     * @return          SQLite object that got its values
     */
    private ContentValues createDatasetObject(Dataset dataset){
        ContentValues data = new ContentValues();
        data.put("name", dataset.name);
        data.put("time", dataset.time);

        return data;
    }

}
