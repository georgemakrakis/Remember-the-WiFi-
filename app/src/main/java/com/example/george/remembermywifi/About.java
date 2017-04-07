package com.example.george.remembermywifi;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class About extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setTitle("About");
    }
    public void repoPageClick(View v)
    {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/georgemakrakis/Remember-the-WiFi-"));
        startActivity(browserIntent);
    }
}
