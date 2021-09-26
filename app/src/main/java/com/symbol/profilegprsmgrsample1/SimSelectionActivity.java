package com.symbol.profilegprsmgrsample1;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;
import com.symbol.profilegprssample1.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class SimSelectionActivity extends Activity {

    TextView headerText;
    Button sim1;
    Button sim2;
    Button sim3;
    Button sim4;
    Button sim5;
    Button sim6;
    Button syncSettingsButton;


    private String APN = "";
    private String AccessPoint = "";
    private String UserName = "";
    private String Password = "";
    private int ReplaceExisting = 0;
    private int MakeDefault = 0;

    // Provides the error type for characteristic-error
    private String errorType = "";

    // Provides the parm name for parm-error
    private String parmName = "";

    // Provides error description
    private String errorDescription = "";

    // Provides error string with type/name + description
    private String errorString = "";



    private void checkForTheApplicationPreConditions() {

        // 1. Is Internet connected. If not connected show dialog
        if (!isInternetConnected()) {
            showDialog("Internet Status", "Internet not available on this device. Please check the internet.");
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Sim section UI
        initializeUI();
        initializeSimTest();
        checkForTelephonyStatePermission();

        addSyncButtonListener();
        checkForTheApplicationPreConditions();

        //getAPNFromContentResolver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }



    private void addSyncButtonListener() {
        syncSettingsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
            }
        });

    }



    public void showDialog(String title, String messaage){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(messaage)
                .setTitle(title);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public boolean isInternetConnected() {
        ConnectivityManager cm =
                (ConnectivityManager)getApplication().getSystemService(getApplicationContext().CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;
    }

    public boolean isMeteredConnection() {
        ConnectivityManager cm =
                (ConnectivityManager)getApplicationContext().getSystemService(getApplicationContext().CONNECTIVITY_SERVICE);
        boolean isMetered = cm.isActiveNetworkMetered();
        return isMetered;
    }

    private void initializeUI() {
        headerText = findViewById(R.id.textView);
        syncSettingsButton = findViewById(R.id.syncSettingsButton);

        sim1 = findViewById(R.id.button1);
        sim2 = findViewById(R.id.button2);
        sim3 = findViewById(R.id.button3);
        sim4 = findViewById(R.id.button4);
        sim5 = findViewById(R.id.button5);
        sim6 = findViewById(R.id.button6);


        //Disable button visibility
        syncSettingsButton.setVisibility(View.INVISIBLE);
        syncSettingsButton.setVisibility(View.GONE);
        sim1.setVisibility(View.GONE);
        sim2.setVisibility(View.GONE);
        sim3.setVisibility(View.GONE);
        sim4.setVisibility(View.GONE);
        sim5.setVisibility(View.GONE);
        sim6.setVisibility(View.GONE);
    }

    public void initializeSimTest() {

        SubscriptionManager subscriptionManager = (SubscriptionManager) this.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();
        int simcount = subscriptionInfos.size();//telMngr.getPhoneCount();
        updatedSimButtonsUI(simcount, subscriptionInfos);

        Toast.makeText(this,""+simcount,Toast.LENGTH_LONG).show();
    }

    private String getSimDisplayName(String name, int index) {
        if(name == null) return "Sim card "+index;
        else return name;

    }
    private void updatedSimButtonsUI(int simcount, List<SubscriptionInfo> subscriptionInfos) {
        for(int i=0; i<simcount; i++) {
            if(i == 0) {
                sim1.setText(getSimDisplayName(subscriptionInfos.get(i).getDisplayName().toString(), i));
                sim1.setVisibility(View.VISIBLE);
                syncSettingsButton.setVisibility(View.VISIBLE);
            }else if(i == 1) {

                sim2.setText(getSimDisplayName(subscriptionInfos.get(i).getDisplayName().toString(), i));
                sim2.setVisibility(View.VISIBLE);
            }else if(i == 2) {
                sim3.setText(getSimDisplayName(subscriptionInfos.get(i).getDisplayName().toString(), i));
                sim3.setVisibility(View.VISIBLE);
            }
            //TODO: Implement other sim names here.
        }

    }

    private static String getDeviceIdBySlot(Context context, String predictedMethodName, int slotID) throws SimSelectionActivity.CustomMethodNotFoundException, CustomMethodNotFoundException {

        String imsi = null;
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class<?> telephonyClass = Class.forName(telephony.getClass().getName());
            Class<?>[] parameter = new Class[1];
            parameter[0] = int.class;
            Method getSimID = telephonyClass.getMethod(predictedMethodName, parameter);

            Object[] obParameter = new Object[1];
            obParameter[0] = slotID;
            Object ob_phone = getSimID.invoke(telephony, obParameter);

            if (ob_phone != null) {
                imsi = ob_phone.toString();

            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SimSelectionActivity.CustomMethodNotFoundException(predictedMethodName);
        }

        return imsi;
    }

    private static class CustomMethodNotFoundException extends Exception {
        private static final long serialVersionUID = -996812356902545308L;

        public CustomMethodNotFoundException(String info) {
            super(info);
        }

    }

    private static String getDeviceIdBySlot2(Context context, String predictedMethodName, int slotID) throws SimSelectionActivity.CustomMethodNotFoundException, CustomMethodNotFoundException {

        String imei = null;

        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        try{

            Class<?> telephonyClass = Class.forName(telephony.getClass().getName());

            Class<?>[] parameter = new Class[1];
            parameter[0] = int.class;
            Method getSimID = telephonyClass.getMethod(predictedMethodName, parameter);

            Object[] obParameter = new Object[1];
            obParameter[0] = slotID;
            Object ob_phone = getSimID.invoke(telephony, obParameter);

            if(ob_phone != null){
                imei = ob_phone.toString();

            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SimSelectionActivity.CustomMethodNotFoundException(predictedMethodName);
        }

        return imei;
    }

    ///Phone state permission
    private final int REQUEST_PERMISSION_PHONE_STATE=1;
    public void checkForTelephonyStatePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE)) {
                showExplanation("Permission Needed", "Rationale", Manifest.permission.READ_PHONE_STATE, REQUEST_PERMISSION_PHONE_STATE);
            } else {
                requestPermission(Manifest.permission.READ_PHONE_STATE, REQUEST_PERMISSION_PHONE_STATE);
            }
        } else {
            initializeSimTest();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_PHONE_STATE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(SimSelectionActivity.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                    initializeSimTest();
                } else {
                    Toast.makeText(SimSelectionActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }

    private void getAPNFromContentResolver() {
        Cursor c = getApplicationContext().getContentResolver().query(Uri.parse("content://telephony/carriers/current"),null, null, null, null);
        Log.e("MainActivity","getColumnNames: "+
                Arrays.toString(c.getColumnNames())); //get the column names from here.
        if (c.moveToFirst()){
            do{
                String data = c.getString(c.getColumnIndex("name")); //one of the column name to get the APN names.
                Log.e("MainActivity","data: "+ data);

            }while(c.moveToNext());
        }
        c.close();
    }


}

