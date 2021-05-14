package io.github.epsi95;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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

public class ForegroundService extends Service {

	private static final String TAG = "ForegroundService";
	SMSBroadcastReceiver smsBroadcastReceiver = new SMSBroadcastReceiver();
	NotificationManagerCompat manager;
	private volatile boolean isThreadRunning = false;
	String userPhoneNumber;
	String userSelectedAge;
	String userSelectedVaccineType;
	String userSelectedPaidType;
	String userSelectedSearchByType;
	String userPinNumber;
	int userDistrictCode = -1;
	int userScanningInterval = 60;
	String userSecret;
	private volatile String transactionID;

	@Override
	public void onCreate() {
		super.onCreate();
		IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
		registerReceiver(smsBroadcastReceiver, filter);
		manager = NotificationManagerCompat.from(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String otp = intent.getStringExtra("otp");

		userPhoneNumber = intent.getStringExtra("userPhoneNumber") != null ?
				intent.getStringExtra("userPhoneNumber") : userPhoneNumber;
		userSelectedAge = intent.getStringExtra("userSelectedAge") != null ?
				intent.getStringExtra("userSelectedAge") : userSelectedAge;
		userSelectedVaccineType = intent.getStringExtra("userSelectedVaccineType") != null ?
				intent.getStringExtra("userSelectedVaccineType") : userSelectedVaccineType;
		userSelectedPaidType = intent.getStringExtra("userSelectedPaidType") != null ?
				intent.getStringExtra("userSelectedPaidType") : userSelectedPaidType;
		userSelectedSearchByType = intent.getStringExtra("userSelectedSearchByType") != null ?
				intent.getStringExtra("userSelectedSearchByType") : userSelectedSearchByType;
		userPinNumber = intent.getStringExtra("userPinNumber") != null ?
				intent.getStringExtra("userPinNumber") : userPinNumber;
		userDistrictCode = intent.getIntExtra("userDistrictCode", userDistrictCode);
		userScanningInterval = intent.getIntExtra("userScanningInterval", userScanningInterval);
		userSecret = intent.getStringExtra("userSecret") != null ?
				intent.getStringExtra("userSecret") : userSecret;

		Log.d(TAG, "onStartCommand: " + userPhoneNumber + " " + userSelectedAge + " " +
				userSelectedVaccineType + " " + userSelectedPaidType + " " +
				userSelectedSearchByType + " " + userPinNumber + " " + userDistrictCode +
				" " + userScanningInterval + " " + userSecret);

		Log.d(TAG, "onStartCommand: onStartCommand received with OTP " + otp);
		if (!isThreadRunning) {
			Notification notification = getNotification("Cowin Automator", "OTP reading complete " + otp);
			manager.notify(1, notification);
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

	public Notification getNotification(String title, String text) {
		Intent notificationIntent = new Intent(Intent.ACTION_VIEW);
		notificationIntent.setData(Uri.parse("https://selfregistration.cowin.gov.in/dashboard"));
		PendingIntent pendingIntent = PendingIntent.getActivity(this,
				0, notificationIntent, 0);

		Intent mainActivityIntent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent2 = PendingIntent.getActivity(this,
				0, mainActivityIntent, 0);

		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setContentTitle(title)
				.setContentText(text)
				.setSmallIcon(R.drawable.ic_baseline_loop_24)
				.setContentIntent(pendingIntent)
				.addAction(R.drawable.ic_baseline_loop_24, "Back to app", pendingIntent2)
				.setNotificationSilent()
				.build();
		return notification;
	}
	public Notification getAlarmNotification(String foundCenters){
		Log.d(TAG, "getAlarmNotification: " + foundCenters);
		Intent notificationIntent = new Intent(Intent.ACTION_VIEW);
		notificationIntent.setData(Uri.parse("https://selfregistration.cowin.gov.in/dashboard"));
		PendingIntent pendingIntent = PendingIntent.getActivity(this,
				0, notificationIntent, 0);

		Intent availableActivityIntent = new Intent(this, AvailableCenters.class);
		availableActivityIntent.putExtra("data", foundCenters);
		PendingIntent pendingIntent2 = PendingIntent.getActivity(this,
				0, availableActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT);

		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setContentTitle("Cowin Automator")
				.setContentText(foundCenters)
				.setSmallIcon(R.drawable.ic_baseline_loop_24)
				.setContentIntent(pendingIntent)
				.addAction(R.drawable.ic_baseline_loop_24, "See available centers", pendingIntent2)
				.setVibrate(new long[] {50, 100, 100, 50})
				.setSound(Settings.System.DEFAULT_ALARM_ALERT_URI)
				.build();
		return notification;
	}
	class NetworkHandlerRunnable implements Runnable {
		private final String OTP;

		public NetworkHandlerRunnable(String OTP) {
			this.OTP = OTP;
		}

		@Override
		public void run() {
			isThreadRunning = true;
			if (OTP.isEmpty()) {
				Log.d(TAG, "run: Thread Started");

				RequestFuture<JSONObject> requestFuture = RequestFuture.newFuture();
				// firstly we will get the transaction id
				final String transactionURL = "https://cdn-api.co-vin.in/api/v2/auth/generateMobileOTP";
				final JSONObject transactionBody = new JSONObject();
				try {
					transactionBody.put("mobile", userPhoneNumber);
					transactionBody.put("secret", userSecret);
				} catch (JSONException e) {
					e.printStackTrace();
				}

				JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
						transactionURL,
						transactionBody,
						requestFuture,
						requestFuture) {
					@Override
					public Map<String, String> getHeaders() throws AuthFailureError {
						HashMap<String, String> headers = new HashMap<String, String>();
						//headers.put("Content-Type", "application/json");
						headers.put("user-agent", "'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36");
						headers.put("'Content-Type", "application/json");
						return headers;
					}
				};
				App.queue.add(request);
				JSONObject object = null;
				try {
					Notification notification = getNotification("Cowin Automator", "OTP requested, waiting to read...");
					manager.notify(1, notification);
					object = requestFuture.get(10, TimeUnit.SECONDS);
					Log.d(TAG, "run: " + object);
					transactionID = object.getString("txnId");
				} catch (Exception e) {
					e.printStackTrace();
					Notification notification = getNotification("Cowin Automator", "Unable to request OTP!");
					manager.notify(1, notification);
				}
				isThreadRunning = false;
				return;
			} else {
				Log.d(TAG, "run: Thread will execute with OTP");
				Notification notification = getNotification("Cowin Automator", "OTP " + OTP + " read successfully, now monitoring...");
				manager.notify(1, notification);

				RequestFuture<JSONObject> requestFuture = RequestFuture.newFuture();

				// we will get the token
				final String transactionURL = "https://cdn-api.co-vin.in/api/v2/auth/validateMobileOtp";
				final JSONObject transactionBody = new JSONObject();
				try {
					transactionBody.put("txnId", transactionID);
					transactionBody.put("otp", toHexString(getSHA(OTP)));
				} catch (Exception e) {
					e.printStackTrace();
				}

				JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
						transactionURL,
						transactionBody,
						requestFuture,
						requestFuture) {
					@Override
					public Map<String, String> getHeaders() throws AuthFailureError {
						HashMap<String, String> headers = new HashMap<String, String>();
						//headers.put("Content-Type", "application/json");
						headers.put("user-agent", "'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36");
						headers.put("'Content-Type", "application/json");
						return headers;
					}
				};
				App.queue.add(request);
				try {
					JSONObject object = requestFuture.get(10, TimeUnit.SECONDS);
					Log.d(TAG, "run: " + object);
					final String token = object.getString("token");
					notification = getNotification("Cowin Automator", "Token received, monitoring started.");
					manager.notify(1, notification);

					//////////////////////////////////////////////////////////
					while (true){
						if(!isThreadRunning){
							return;
						}
						//check for beneficiaries details
						final String beneficiaryURL = "https://cdn-api.co-vin.in/api/v2/appointment/beneficiaries";
						RequestFuture<JSONObject> requestFuture2 = RequestFuture.newFuture();
						request = new JsonObjectRequest(Request.Method.GET,
								beneficiaryURL,
								null,
								requestFuture2,
								requestFuture2) {
							@Override
							public Map<String, String> getHeaders() throws AuthFailureError {
								HashMap<String, String> headers = new HashMap<String, String>();
								//headers.put("Content-Type", "application/json");
								headers.put("user-agent", "'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36");
								headers.put("'Content-Type", "application/json");
								headers.put("Authorization", "Bearer " + token);
								return headers;
							}
						};
						App.queue.add(request);
						JSONObject object2 = requestFuture2.get(10, TimeUnit.SECONDS);
						Log.d(TAG, "run: "	+ object);
						String beneficiaryNames = "";
						if(!object2.has("beneficiaries")){
							Log.d(TAG, "run: Sorry no beneficiary" + object2);
							notification = getNotification("Cowin Automator", "Monitoring in progress!\nNo beneficiaries added in this number " + userPhoneNumber);
						}else{
							for(int i=0; i<object2.getJSONArray("beneficiaries").length(); i++){
								beneficiaryNames += object2.getJSONArray("beneficiaries").getJSONObject(i).getString("name");
								beneficiaryNames += " ";
							}
							notification = getNotification("Cowin Automator", "Monitoring in progress!\nBeneficiaries found:  " + beneficiaryNames);
						}
						manager.notify(1, notification);
						Log.d(TAG, "run: Beneficiaries " + object2);

						LocalDateTime myDateObj = LocalDateTime.now();
						DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy");
						String formattedDate = myDateObj.format(myFormatObj);
						JSONObject centerDetails = getCenterDetails(token, formattedDate);
						Boolean noSlotFound = true;
						String foundCenters = "Found Availability\n";

						for(int i = 0; i< centerDetails.getJSONArray("centers").length(); i++){
							JSONObject eachCenter = centerDetails.getJSONArray("centers").getJSONObject(i);
							String paidType = eachCenter.getString("fee_type"); //Free Paid
							String centerName = eachCenter.getString("name");
							JSONArray session = eachCenter.getJSONArray("sessions");
							for(int j=0; j< session.length(); j++){
								JSONObject eachSession = session.getJSONObject(j);
								if(eachSession.getInt("available_capacity") > 0){
									String vaccineType = eachSession.getString("vaccine");
									String date = eachSession.getString("date");
									int minAge = eachSession.getInt("min_age_limit");

									int userSelectedAgeParsed = userSelectedAge.equals("Age 18+") ? 18 : 45;

									if((userSelectedPaidType.equals("All") && userSelectedAge.equals("All") && userSelectedVaccineType.equals("All")) ||
											((userSelectedVaccineType.toLowerCase().equals(vaccineType.toLowerCase()) || userSelectedVaccineType.equals("All"))
													&& (userSelectedAgeParsed==minAge || userSelectedAge.equals("All"))
													&& (userSelectedPaidType.toLowerCase().equals(paidType.toLowerCase()) || userSelectedPaidType.equals("All")))){
										noSlotFound = false;
										foundCenters +=  centerName + "on " + date + "-" + vaccineType + "-" + minAge + "-" + paidType +
												" at " + eachCenter.getString("address") +"\n";
									}
								}
							}

						}
//						Log.d(TAG, "run: foundCenters" + foundCenters);
						if(!noSlotFound){
							notification = getAlarmNotification(foundCenters);
							manager.notify(1, notification);
						}

						Thread.sleep(userScanningInterval * 1000);
					}
					////////////////////////////////////////////////////////////

				} catch (InterruptedException | ExecutionException | TimeoutException | JSONException e) {
					e.printStackTrace();
					notification = getNotification("Cowin Automator", "Something bad just happened 😓, will try after 1 min! Can happen if no beneficiary added!");
					manager.notify(1, notification);
					try {
						Thread.sleep(60*1000);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
					isThreadRunning = false;
					NetworkHandlerRunnable networkHandlerRunnable = new NetworkHandlerRunnable("");
					new Thread(networkHandlerRunnable).start();
					return;
				}
			}
		}
	}

	public JSONObject getCenterDetails(final String token, String date) throws InterruptedException, ExecutionException, TimeoutException {
		RequestFuture<JSONObject> requestFuture = RequestFuture.newFuture();
		final String centerURL = userSelectedSearchByType.equals("By District") ?
				"https://cdn-api.co-vin.in/api/v2/appointment/sessions/calendarByDistrict?district_id=" + userDistrictCode + "&date=" + date :
				"https://cdn-api.co-vin.in/api/v2/appointment/sessions/calendarByPin?pincode=" + userPinNumber + "&date=" + date;

		Log.d(TAG, "getCenterDetails: " + centerURL + " " + date);

		JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,
				centerURL,
				null,
				requestFuture,
				requestFuture) {
			@Override
			public Map<String, String> getHeaders() throws AuthFailureError {
				HashMap<String, String> headers = new HashMap<String, String>();
				//headers.put("Content-Type", "application/json");
				headers.put("user-agent", "'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36");
				headers.put("Authorization", "Bearer " + token);
				return headers;
			}
		};
		App.queue.add(request);
		JSONObject object = requestFuture.get(10, TimeUnit.SECONDS);
		Log.d(TAG, "getCenterDetails: " + object);
		return object;
	}

	public static byte[] getSHA(String input) throws NoSuchAlgorithmException {
		// Static getInstance method is called with hashing SHA
		MessageDigest md = MessageDigest.getInstance("SHA-256");

		// digest() method called
		// to calculate message digest of an input
		// and return array of byte
		return md.digest(input.getBytes(StandardCharsets.UTF_8));
	}

	public static String toHexString(byte[] hash) {
		// Convert byte array into signum representation
		BigInteger number = new BigInteger(1, hash);

		// Convert message digest into hex value
		StringBuilder hexString = new StringBuilder(number.toString(16));

		// Pad with leading zeros
		while (hexString.length() < 32) {
			hexString.insert(0, '0');
		}

		return hexString.toString();
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
