package search.raman.ilovemarshmallow.Activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;

import search.raman.ilovemarshmallow.Gcm.GcmActivity;
import search.raman.ilovemarshmallow.R;
import search.raman.ilovemarshmallow.Utilities.Globals;
import search.raman.ilovemarshmallow.Utilities.Utility;


/********************************************************************************************************************************
 * Developer : Ramanpreet Singh Khinda
 * <p/>
 * This class is used for registering the user to our Gcm Server.
 * So that our users are able to send and receive Gcm Notifications.
 * <p/>
 * We are using Gcm Notifications for sharing product information among our users
 ********************************************************************************************************************************/
public class ClientLoginActivity extends Activity {
    private Button btnLogin;
    private TextView txtToolbarTitle;
    private EditText editTextEmailId;
    private GcmActivity gcmObject;
    private Typeface romanticTypeface, boldRomanticTypeface;
    private Toolbar loginToolbar;
    private ProgressDialog ringProgressDialog;
    private Utility utility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        utility = new Utility();

        // Creating GCM object for calling Gcm API's
        gcmObject = GcmActivity.getInstance(this);

        //Registering observer with REGISTER_UNREGISTER_LISTENER using LocalBroadcastManager to listen for REGISTER and UNREGISTER events
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegisterUnregisterReceiver,
                new IntentFilter(Globals.REGISTER_UNREGISTER_LISTENER));

        editTextEmailId = (EditText) findViewById(R.id.editText_user_email_id);
        btnLogin = (Button) findViewById(R.id.btn_login);
        setRomanticFont();
    }

    private void setRomanticFont() {
        romanticTypeface = Typeface.createFromAsset(getAssets(), Globals.ROMANTIC_FONT);
        boldRomanticTypeface = Typeface.create(romanticTypeface, Typeface.BOLD);

        loginToolbar = (Toolbar) findViewById(R.id.login_toolbar);
        txtToolbarTitle = (TextView) loginToolbar.findViewById(R.id.login_toolbar_title);
        txtToolbarTitle.setTypeface(boldRomanticTypeface);
    }

    @Override
    protected void onResume() {
        super.onResume();

        editTextEmailId.setText(getAccount());

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                String userEmailId = ((EditText) findViewById(R.id.editText_user_email_id)).getText().toString();
                String userDeviceId = utility.getAndroidID(getApplicationContext());

                if (!utility.isValidEmail(userEmailId)) {
                    showToast("Email id is not valid", Toast.LENGTH_SHORT);
                } else {
                    if (utility.isNetworkAvailable(ClientLoginActivity.this) == true) {
                        launchRingDialog("Connecting to server");
                        // Calling registerAndLogin() API for registering to GCM Server and Logging inn if already registered
                        gcmObject.registerAndLogin(userEmailId, userDeviceId);
                    } else {
                        showToast("Network not available", Toast.LENGTH_SHORT);
                    }
                }
            }
        });
    }

    // Will be called whenever our client app gets response for REGISTER and UNREGISTER events from our GCM Server
    private BroadcastReceiver mRegisterUnregisterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(Globals.TAG, "mRegisterUnregisterReceiver received broadcasted message");

            String message = intent.getStringExtra(Globals.MESSAGE);
            showToast(message, Toast.LENGTH_SHORT);
            dismissProgressDialog();

            //checking if NOT_REGISTERED response is not received and start the app otherwise
            if (!(message.contains(Globals.NOT_REGISTERED) || message.contains(Globals.GOOGLE_PLAY_SERVICE_NOT_VALID))) {
                startApp();
            }
        }
    };

    public void showToast(String msg, int time) {
        Toast.makeText(this, msg, time).show();
    }

    public String getAccount() {
        Account[] accounts = AccountManager.get(this)
                .getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        String[] names = new String[accounts.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = accounts[i].name;
        }
        if (names != null)
            return names[0];
        else
            return "";
    }

    private void launchRingDialog(String displayMessage) {
        ringProgressDialog = ProgressDialog.show(this, "Please wait ...", displayMessage, true);
        ringProgressDialog.setCancelable(true);
    }

    public void dismissProgressDialog() {
        if (ringProgressDialog != null && ringProgressDialog.isShowing()) {
            ringProgressDialog.dismiss();
        }
    }

    public void startApp() {
        String userEmailId = ((EditText) findViewById(R.id.editText_user_email_id)).getText().toString();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Globals.CURRENT_USER_EMAIL_ID, userEmailId);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //fixing issue : prevention of window leak exception
        dismissProgressDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Un register the receiver in onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegisterUnregisterReceiver);
    }

}
