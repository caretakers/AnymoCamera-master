package com.example.anymo.anymocamera;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.example.libanymocameratools.AnymoCameraFragment;

public class MainActivity extends AppCompatActivity {

    private AnymoCameraFragment anymoCameraFragment;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        anymoCameraFragment = new AnymoCameraFragment();
        fragmentManager = getFragmentManager();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fragmentManager.beginTransaction()
                .add(R.id.fl_cameraview, anymoCameraFragment)
                .commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        fragmentManager.beginTransaction()
                .remove(anymoCameraFragment)
                .commit();
    }
}