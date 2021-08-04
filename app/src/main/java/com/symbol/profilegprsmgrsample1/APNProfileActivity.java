package com.symbol.profilegprsmgrsample1;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.ProfileManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.StringReader;

public class APNProfileActivity extends Activity implements EMDKManager.EMDKListener {

    private static String TAG = "APNSettings";
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


    @Override
    public void onOpened(EMDKManager emdkManager) {
        this.emdkManager = emdkManager;

        //Get the ProfileManager object to process the profiles
        profileManager = (ProfileManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.PROFILE);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //The EMDKManager object will be created and returned in the callback.
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);

        //Check the return status of EMDKManager object creation.
        if (results.statusCode == EMDKResults.STATUS_CODE.SUCCESS) {
            //EMDKManager object creation success
        } else {
            //EMDKManager object creation failed
        }
        updateProfile_XMLString();
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

    private void updateProfile_XMLString() {
        errorType = "";
        parmName = "";
        errorDescription = "";
        errorString = "";


        APN = "internet";
        String action = "1";
        AccessPoint = "n.ispsn";
        UserName = "";
        Password = "";
        ReplaceExisting = 1;
        MakeDefault = 1;


        //Prepare XML to modify the existing profile
        String[] updateData = new String[1];
        updateData[0] =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                        "<characteristic type=\"Profile\">" +
                        "<parm name=\"ProfileName\" value=\"GPRSProfile-1\"/>" +
                        "<characteristic type=\"GprsMgr\" version=\"0.2\">" +
                        "<parm name=\"GprsAction\" value=\"" + 1 + "\"/>";


        updateData[0] += "<parm name=\"GprsCarrier\" value=\"0\"/>" +
                "<characteristic type=\"gprs-details\">" +
                "<parm name=\"ApnName\" value=\"" + APN + "\"/>" +
                "<parm name=\"ReplaceIfExisting\" value=\"" + ReplaceExisting + "\"/>" +
                "<parm name=\"MakeDefault\" value=\"" + MakeDefault + "\"/>" +
                "</characteristic>" +
                "<characteristic type=\"custom-details\">" +
                "<parm name=\"CustomAccessPoint\" value=\"" + AccessPoint + "\"/>" +
                "<parm name=\"CustomUserName\" value=\"" + UserName + "\"/>" +
                "<parm name=\"CustomPassword\" value=\"" + Password + "\"/>" +
                "</characteristic>";

        updateData[0] += "</characteristic>" +
                "</characteristic>";

        new ProcessUpdatedAPNProfileTask().execute(updateData[0]);
    }


    private class ProcessUpdatedAPNProfileTask extends AsyncTask<String, Void, EMDKResults> {

        @Override
        protected EMDKResults doInBackground(String... params) {

            //Call process profile to modify the profile of specified profile name
            EMDKResults results = profileManager.processProfile(profileName, ProfileManager.PROFILE_FLAG.GET, params);

            return results;
        }

        @Override
        protected void onPostExecute(EMDKResults results) {

            super.onPostExecute(results);

            String resultString = "";

            //Check the return status of processProfile
            if (results.statusCode == EMDKResults.STATUS_CODE.CHECK_XML) {

                // Get XML response as a String
                String statusXMLResponse = results.getStatusString();

                try {
                    // Create instance of XML Pull Parser to parse the response
                    XmlPullParser parser = Xml.newPullParser();
                    // Provide the string response to the String Reader that reads
                    // for the parser
                    parser.setInput(new StringReader(statusXMLResponse));
                    // Call method to parse the response
                    parseUpdatedAPNResponse(parser);

                    if (TextUtils.isEmpty(parmName) && TextUtils.isEmpty(errorType) && TextUtils.isEmpty(errorDescription)) {

                        resultString = "Profile updated success.";
                    } else {
                        resultString = "Profile updated failed." + errorString;
                    }
                    Log.d(TAG, resultString);

                } catch (XmlPullParserException e) {
                    resultString = e.getMessage();
                    Log.d(TAG, resultString);
                }
            }
        }
    }

    // Method to parse the APN settings XML response using XML Pull Parser
    public void parseUpdatedAPNResponse(XmlPullParser myParser) {
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
}
