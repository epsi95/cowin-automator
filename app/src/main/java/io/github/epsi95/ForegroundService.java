package io.github.epsi95;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.github.epsi95.App.CHANNEL_ID;

public class ForegroundService extends Service{

	private static final String TAG = "ForegroundService";

	private SMSBroadcastReceiver smsBroadcastReceiver;
	private NotificationManagerCompat manager;

	private volatile String transactionID;
	private volatile boolean isThreadRunning = false;

	private Http http;

	private Handler handler = new Handler();

	@Override
	public void onCreate() {
		super.onCreate();

		IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
		smsBroadcastReceiver =  new SMSBroadcastReceiver();
		registerReceiver(smsBroadcastReceiver, filter);
		manager = NotificationManagerCompat.from(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String otp = intent.getStringExtra("otp");

		// setting up a http object for synchronous network operation
		http = new Http(App.userPreference.getUserPhoneNumber());

		Log.d(TAG, "onStartCommand : " + App.userPreference.getUserPhoneNumber() + " " + App.userPreference.getUserAge() + " " +
				App.userPreference.getUserSelectedVaccineType() + " " + App.userPreference.getUserSelectedPaidType() + " " +
				App.userPreference.getUserSelectedSearchByType() + " " + App.userPreference.getUserPinNumber() + " " + App.userPreference.getUserDistrictCode() +
				" " + App.userPreference.getUserScanningInterval() + " " + App.userPreference.getUserSecret());

		Log.d(TAG, "onStartCommand: onStartCommand received with OTP " + otp);

		if (!isThreadRunning) {
			Notification notification = makeNotification("Cowin Automator", "Foreground service started");
			NetworkHandlerRunnable networkHandlerRunnable = new NetworkHandlerRunnable(otp);
			new Thread(networkHandlerRunnable).start();
			startForeground(1, notification);
		}
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(smsBroadcastReceiver);
		isThreadRunning = false;
	}

	public Notification makeNotification(String title, String text){
		Intent notificationIntent = new Intent(this, CaptchaSubmitActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this,
				0, notificationIntent, 0);


		return new NotificationCompat.Builder(this, CHANNEL_ID)
				.setContentTitle(title)
				.setContentText(text)
				.setSmallIcon(R.drawable.ic_baseline_loop_24)
				.setContentIntent(pendingIntent)
				.setNotificationSilent()
				.build();
	}
	public void sendNotification(String title, String text) {
		manager.notify(1, makeNotification(title, text));
	}
	public void sendAlarmNotification(String foundCenters){
		Log.d(TAG, "getAlarmNotification: " + foundCenters);
		Intent notificationIntent = new Intent(this, CaptchaSubmitActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this,
				0, notificationIntent, 0);


		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setContentTitle("Cowin Automator")
				.setContentText(foundCenters)
				.setSmallIcon(R.drawable.ic_baseline_loop_24)
				.setContentIntent(pendingIntent)
				.setVibrate(new long[] {50, 100, 100, 50})
				.setSound(Settings.System.DEFAULT_ALARM_ALERT_URI)
				.build();
		manager.notify(1, notification);
	}

	public void broadcastCenterFound(String token, JSONArray centerDetails){
		Intent intent = new Intent("CowinCenterFound");
		intent.putExtra("token", token);
		intent.putExtra("data", centerDetails.toString());
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	class NetworkHandlerRunnable implements Runnable {
		private final String otp;

		public NetworkHandlerRunnable(String OTP) {
			this.otp = OTP;
		}

		@Override
		public void run() {
			isThreadRunning = true;

			if (otp.isEmpty()) {

				Log.d(TAG, "run: Thread Started");
				try {
					sendNotification("Cowin Automator", "Requesting OTP...");
					transactionID = http.sendOtp(App.userPreference.getUserSecret());
					Log.d(TAG, "run: requested for transactionID " + transactionID);
				} catch (Exception e) {
					e.printStackTrace();
					sendNotification("Cowin Automator", "Unable to request OTP!");
				}
				isThreadRunning = false;

			} else {

				Log.d(TAG, "run: Thread will execute with OTP");
				sendNotification("Cowin Automator", "OTP " + otp + " read successfully, now monitoring...");

				try {
					final String token = http.validateOtp(transactionID, otp);
					Log.d(TAG, "run: requested for token " + token);
					sendNotification("Cowin Automator", "Token received, monitoring started.");

					//////////////////////////////////////////////////////////
					// this a continuous loop which will check for vaccination center
					while (true){
						// check if isThreadRunning flag is true
						// if it is false then it means somebody asked to stop the thread
						// otherwise wise thread will self kill
						if(!isThreadRunning){
							return;
						}

						//check for beneficiaries details
						ArrayList<Beneficiary> beneficiaries = http.getBeneficiaries(token);
						Log.d(TAG, "run: requesting to get beneficiary details " + beneficiaries);

						sendNotification("Cowin Automator", "Searching vaccine center for " + App.userPreference.getBeneficiary().getName());

						LocalDateTime myDateObj = LocalDateTime.now();
						DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy");
						String formattedDate = myDateObj.format(myFormatObj);

						final JSONArray centerDetails;
						if(App.userPreference.getUserSelectedSearchByType() == SearchByType.BY_PIN){
							centerDetails = http.getCentersByPinCode(token, App.userPreference.getUserPinNumber(), formattedDate);

						}else{
							centerDetails = http.getCentersByDistrictId(token, "" + App.userPreference.getUserDistrictCode(), formattedDate);
						}
						Log.d(TAG, "run: Center details " + centerDetails);
						boolean noSlotFound = true;
						String foundCenters = "Found Availability";

						if(centerDetails.length()>0){
							noSlotFound = false;
						}

						if(!noSlotFound){
							sendAlarmNotification(foundCenters);

							// now will broadcast the available center
							handler.post(new Runnable() {
								@Override
								public void run() {
									broadcastCenterFound(token, centerDetails);
								}
							});
						}
						Log.d(TAG, "run: Sleeping for " + App.userPreference.getUserScanningInterval() + " milliseconds");
						Thread.sleep(App.userPreference.getUserScanningInterval() * 1000);
					}
					////////////////////////////////////////////////////////////

				} catch (InterruptedException | ExecutionException | TimeoutException | JSONException | NoSuchAlgorithmException e) {
					e.printStackTrace();
					sendNotification("Cowin Automator", "Hold on!");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
					isThreadRunning = false;
					NetworkHandlerRunnable networkHandlerRunnable = new NetworkHandlerRunnable("");
					new Thread(networkHandlerRunnable).start();
				}
			}
		}
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
