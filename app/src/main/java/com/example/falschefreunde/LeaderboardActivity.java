package com.example.falschefreunde;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity for the functionality of activity_leaderboard
 */
public class LeaderboardActivity extends AppCompatActivity {

    /**
     * Create an instance of ActionBar
     */
    public ActionBar actionbar;

    /**
     * Called when LeaderboardActivity is first created
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        actionbar = getSupportActionBar();

        actionbar.setDisplayHomeAsUpEnabled(true);

    }
    /**
     * Return to Menu, when back button in actionbar gets clicked
     * Made this with help of:
     * https://www.geeksforgeeks.org/how-to-add-and-customize-back-button-of-action-bar-in-android/
     * https://stackoverflow.com/questions/6121797/android-how-to-change-layout-on-button-click
     * @param item
     * @return
     */
    public boolean onOptionsItemSelected(MenuItem item){
        Intent myIntent = new Intent(getApplicationContext(), MenuActivity.class);
        startActivity(myIntent);
        return true;
    }

}

