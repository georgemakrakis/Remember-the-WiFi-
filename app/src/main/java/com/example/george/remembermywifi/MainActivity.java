package com.example.george.remembermywifi;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setTitle(Html.fromHtml("<font color='#000000'>Remember the WiFi?</font>"));
        loadList();

    }

    public void refreshClick(View v)
    {
        //alternative method to refresh page
        /*Intent intent = getIntent();
        finish();
        startActivity(intent);*/

        loadList();
    }
    //executing all necessary adb commands
    public void commands()
    {
        //transeferring file form /data/misc/wifi/wpa_supplicant.conf to app's files directory
        String[] commands2= {"grep -e ssid -e psk /data/misc/wifi/wpa_supplicant.conf > /data/data/com.example.george.remembermywifi/files/wpa_supplicant.txt"};
        RunAsRoot(commands2);


        //changing file permissions so the file could be read next
        String[] commands3 = {"cd /data/data/com.example.george.remembermywifi/files/ && chmod 666 wpa_supplicant.txt"};
        RunAsRoot(commands3);

        //adding some delay so the previous adb command finish execute
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    //finds file "wpa_supplicant.txt" and loads it's content in an Listview
    public void loadList()
    {
        final ListView listView = (ListView) findViewById(R.id.wifilist);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                //getting selected item and sending to CLIPBOARD_TEXT the pattern between double quotes
                String sel=listView.getItemAtPosition(i).toString();
                String CLIPBOARD_TEXT="";
                Pattern pattern = Pattern.compile("\"(.*?)\"");//regular expression to choose all text between "..."
                Matcher matcher = pattern.matcher(sel);
                if (matcher.find())
                {
                    CLIPBOARD_TEXT=matcher.group(1);
                }
                // copying data to clipboard
                ClipData clip = ClipData.newPlainText(CLIPBOARD_TEXT, CLIPBOARD_TEXT);
                ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this,"Copied!", Toast.LENGTH_LONG).show();
                return true;
            }
        });
        commands();
        try
        {
            //read new txt file and load it's content
            File f2 = new File(getFilesDir(), "wpa_supplicant.txt");
            InputStream in = new FileInputStream(f2);
            BufferedReader buffreader = new BufferedReader(new InputStreamReader(in));

            String line;
            int counter = 0;
            ArrayList<String> lines = new ArrayList<>();
            while ((line = buffreader.readLine()) != null)
            {
                if (counter == 2)
                {
                    lines.add("");
                    counter = 0;
                }
                lines.add(line);
                ++counter;
            }
            buffreader.close();

            /*if(lines.isEmpty())
            {
                Dialog("No networks exist in your device.");
            }*/

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.activity_listview, lines);

            listView.setAdapter(adapter);
            in.close();

        }
        catch (java.io.FileNotFoundException e)
        {
            e.printStackTrace();
            Dialog("An error occurred with the networks file.");
        }
        catch (java.io.IOException e)
        {
            e.printStackTrace();
            Dialog("An error occurred with the networks file.");
        }
    }

    //function to excecute adb shell commands
    public void RunAsRoot(String[] cmds)
    {

        Process p = null;
        try
        {
            p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (String tmpCmd : cmds)
            {
                os.writeBytes(tmpCmd + "\n");
                os.writeBytes("exit\n");
                os.flush();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Dialog("Your device is not rooted!");
        }
    }

    public void Dialog(String msg)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    //exits the application
                    public void onClick(DialogInterface dialog, int id)
                    {
                        moveTaskToBack(true);
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(1);
                        //finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
