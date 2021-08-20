/*
* Copyright (C) 2015-2019 Zebra Technologies Corporation and/or its affiliates
* All rights reserved.
*/
package com.symbol.profilegprsmgrsample1;

import java.io.StringReader;
import java.lang.reflect.Method;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.profilegprssample1.R;

import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements EMDKListener{

    TextView headerText;
    Button sim1;
    Button sim2;
    Button sim3;
    Button sim4;
    Button sim5;
    Button sim6;
    Button syncSettingsButton;

    //Assign the profile name used in EMDKConfig.xml
    private String profileName = "GPRSProfile-1";

    //Declare a variable to store ProfileManager object
    private ProfileManager profileManager = null;

    //Declare a variable to store EMDKManager object
    private EMDKManager emdkManager = null;

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

    enum Action
    {
        ADD_REPLACE(1),
        REMOVE(2);

        private int value;

        private Action(int v)
        {
            value = v;
        }

        public int getValue()
        {
            return value;
        }
    };


    private Action action = Action.ADD_REPLACE;


    private void checkForTheApplicationPreConditions() {

        // 1. Is Internet connected. If not connected show dialog
        if (!isInternetConnected()) {
            showDialog("Internet Status", "Internet not available on this device. Please check the internet.");
        }

        //Initialize EMDK SDK
        initializeEMDKSDK();
    }

    private void initializeEMDKSDK() {
        //The EMDKManager object will be created and returned in the callback.
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);

        //Check the return status of EMDKManager object creation.
        if(results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
            //EMDKManager object creation success
        }else {
            showDialog("EMDK SDK Error", results.getExtendedStatusMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Sim section UI
        initializeUI();
        initializeSimTest();

        addSyncButtonListener();
        checkForTheApplicationPreConditions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Clean up the objects created by EMDK manager
        if (profileManager != null)
            profileManager = null;

        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onClosed() {

        //This callback will be issued when the EMDK closes unexpectedly.
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;
        }

    }

    @Override
    public void onOpened(EMDKManager emdkManager) {

       this.emdkManager = emdkManager;

        //Get the ProfileManager object to process the profiles
        profileManager = (ProfileManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.PROFILE);

    }



    private void addSyncButtonListener() {
        syncSettingsButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                modifyProfile_XMLString();
            }
        });

    }

    private void modifyProfile_XMLString() {

        errorType = "";
        parmName = "";
        errorDescription = "";
        errorString = "";

        //Prepare XML to modify the existing profile
        String[] modifyData = new String[1];
        modifyData[0]=
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                        "<characteristic type=\"Profile\">" +
                        "<parm name=\"ProfileName\" value=\"GPRSProfile-1\"/>" +
                        "<characteristic type=\"GprsMgr\" version=\"0.2\">" +
                        "<parm name=\"GprsAction\" value=\"" + 1 + "\"/>";


            modifyData[0] +=    "<parm name=\"GprsCarrier\" value=\"0\"/>" +
                    "<characteristic type=\"gprs-details\">" +
                    "<parm name=\"ApnName\" value=\"" + "internet" + "\"/>" +
                        "<parm name=\"ReplaceIfExisting\" value=\"" + 1 + "\"/>" +
                    "<parm name=\"MakeDefault\" value=\"" + 1 + "\"/>" +
                    "</characteristic>" +
                    "<characteristic type=\"custom-details\">" +
                    "<parm name=\"CustomAccessPoint\" value=\"" + "n.ispsn" + "\"/>" +
                    "<parm name=\"CustomUserName\" value=\"" + "" + "\"/>" +
                    "<parm name=\"CustomPassword\" value=\"" + "" + "\"/>" +
                    "<parm name=\"CustomProtocol\" value=\"" + "3" + "\"/>" +
                    "<parm name=\"CustomRoamingProtocol\" value=\"" + "3" + "\"/>" +
                    "</characteristic>";


        modifyData[0] +=	"</characteristic>" +
                "</characteristic>";

        new ProcessProfileTask().execute(modifyData[0]);
    }

    // Method to parse the XML response using XML Pull Parser
    public void parseXML(XmlPullParser myParser) {
        int event;
        try {
            // Retrieve error details if parm-error/characteristic-error in the response XML
            event = myParser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                String name = myParser.getName();
                switch (event) {
                    case XmlPullParser.START_TAG:

                        if (name.equals("parm-error")) {
                            parmName = myParser.getAttributeValue(null, "name");
                            errorDescription = myParser.getAttributeValue(null, "desc");
                            errorString = " (Name: " + parmName + ", Error Description: " + errorDescription + ")";
                            return;
                        }

                        if (name.equals("characteristic-error")) {
                            errorType = myParser.getAttributeValue(null, "type");
                            errorDescription = myParser.getAttributeValue(null, "desc");
                            errorString = " (Type: " + errorType + ", Error Description: " + errorDescription + ")";
                            return;
                        }

                        break;
                    case XmlPullParser.END_TAG:

                        break;
                }
                event = myParser.next();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ProcessProfileTask extends AsyncTask<String, Void, EMDKResults> {

        @Override
        protected EMDKResults doInBackground(String... params) {

            //Call process profile to modify the profile of specified profile name
            EMDKResults results = profileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.SET, params);

            return results;
        }

        @Override
        protected void onPostExecute(EMDKResults results) {

            super.onPostExecute(results);

            String resultString = "";

            //Check the return status of processProfile
            if(results.statusCode == EMDKResults.STATUS_CODE.CHECK_XML) {

                // Get XML response as a String
                String statusXMLResponse = results.getStatusString();

                try {
                    // Create instance of XML Pull Parser to parse the response
                    XmlPullParser parser = Xml.newPullParser();
                    // Provide the string response to the String Reader that reads
                    // for the parser
                    parser.setInput(new StringReader(statusXMLResponse));
                    // Call method to parse the response
                    parseXML(parser);

                    if ( TextUtils.isEmpty(parmName) && TextUtils.isEmpty(errorType) && TextUtils.isEmpty(errorDescription) ) {

                        resultString = "Profile update success.";

                        Toast.makeText(getApplicationContext(), "", Toast.LENGTH_LONG).show();
                    }
                    else {

                        resultString = "Profile update failed." + errorString;
                    }

                    Toast.makeText(getApplicationContext(), resultString, Toast.LENGTH_LONG).show();

                } catch (XmlPullParserException e) {
                    resultString =  e.getMessage();
                }
            }

        }
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


//////////////////  sim section



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
        GsmCellLocation cl = (GsmCellLocation) CellLocation.getEmpty();
        CellLocation.requestLocationUpdate();


        TelephonyManager telMngr = ((TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE));

        int simcount = telMngr.getPhoneCount();
        updatedSimButtonsUI(simcount, telMngr);
        String s1 = telMngr.getSimOperatorName();
        int s2 = telMngr.getPhoneCount();

        Toast.makeText(this,s1,Toast.LENGTH_LONG).show();
        Toast.makeText(this,""+s2,Toast.LENGTH_LONG).show();



        try {
            s1 = getDeviceIdBySlot2(this, "getNetworkOperatorName", 0);
            Toast.makeText(this,s1,Toast.LENGTH_LONG).show();
        } catch (CustomMethodNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void updatedSimButtonsUI(int simcount, TelephonyManager telephonyManager) {
        for(int i=0; i<simcount; i++) {

            if(i == 0) {
                sim1.setText(telephonyManager.getNetworkOperatorName());
                sim1.setVisibility(View.VISIBLE);
                syncSettingsButton.setVisibility(View.VISIBLE);
            }else if(i == 1) {
                String sim2name = "Unknown"; // getDeviceIdBySlot(this, "getNetworkOperatorName",  i-1);
                sim2.setText(sim2name);
                sim2.setVisibility(View.VISIBLE);
            }else if(i == 2) {
                String sim3name = "Unknown"; // getDeviceIdBySlot(this, "getNetworkOperatorName",  i-1);
                sim3.setText(sim3name);
                sim3.setVisibility(View.VISIBLE);
            }
            //TODO: Implement other sim names here.

        }

    }

    private static String getDeviceIdBySlot(Context context, String predictedMethodName, int slotID) throws CustomMethodNotFoundException {

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
            throw new CustomMethodNotFoundException(predictedMethodName);
        }

        return imsi;
    }

    private static class CustomMethodNotFoundException extends Exception {
        private static final long serialVersionUID = -996812356902545308L;

        public CustomMethodNotFoundException(String info) {
            super(info);
        }

    }

    private static String getDeviceIdBySlot2(Context context, String predictedMethodName, int slotID) throws CustomMethodNotFoundException {

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
            throw new CustomMethodNotFoundException(predictedMethodName);
        }

        return imei;
    }
}
