package com.example.george.remembermywifi;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
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
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //When Product action item is clicked
        if (id == R.id.rate)
        {

            Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            // To count with Play market backstack, After pressing back button,
            // to taken back to our application, we need to add following flags to intent.
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try
            {
                startActivity(goToMarket);
            }
            catch (ActivityNotFoundException e)
            {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
            }

        }
        //When Contact action item is clicked
        else if (id == R.id.contact)
        {

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{"georgemakrakis88@gmail.com"});
            i.putExtra(Intent.EXTRA_SUBJECT, "AegeanApp_Feedback");
            try
            {
                startActivity(Intent.createChooser(i, "Send mail..."));
            }
            catch (android.content.ActivityNotFoundException ex)
            {
                Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
            }

        }
        //When About action item is clicked
        else if (id == R.id.about)
        {
            Intent i = new Intent(this,About.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
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
        String[] commands2 = {"grep -e ssid -e psk /data/misc/wifi/wpa_supplicant.conf > /data/data/com.example.george.remembermywifi/files/wpa_supplicant.txt"};
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
            Dialog("Something interrupted the execution of the app. Please close the app and try again.");
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
                String sel = listView.getItemAtPosition(i).toString();
                String CLIPBOARD_TEXT = "";
                Pattern pattern = Pattern.compile("\"(.*?)\"");//regular expression to choose all text between "..."
                Matcher matcher = pattern.matcher(sel);
                if (matcher.find())
                {
                    CLIPBOARD_TEXT = matcher.group(1);
                }
                // copying data to clipboard
                ClipData clip = ClipData.newPlainText(CLIPBOARD_TEXT, CLIPBOARD_TEXT);
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "Copied!", Toast.LENGTH_LONG).show();
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

            if(lines.isEmpty())
            {
                Dialog("No wireless networks exist in your device.");
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.activity_listview, lines);

            listView.setAdapter(adapter);
            in.close();

        }
        catch (java.io.FileNotFoundException e)
        {
            e.printStackTrace();
            Dialog("An error occurred with the networks file. Please click the refresh button.");
        }
        catch (java.io.IOException e)
        {
            e.printStackTrace();
            Dialog("An error occurred with the networks file. Please close the app and try again.");
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
            Dialog("Your device is not rooted! Please root your device to proceed.");
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
