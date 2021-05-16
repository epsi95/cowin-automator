package io.github.epsi95;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "MainActivity";

	private EditText phoneNumberEditText;
	private RadioGroup ageGroupRadioGroup;
	private RadioGroup vaccineTypeRadioGroup;
	private RadioGroup paidTypeRadioGroup;
	private RadioGroup searchByTypeRadioGroup;
	private LinearLayout userAddressInputFieldLinearLayout;
	private TextView scanningIntervalTextView;
	private EditText secretEditText;

	private int selectedDistrictCode;
	private String searchByTypeString = "By PIN";
	private int userScanningInterval = 1;

	private Handler handler = new Handler();

	private final int PERMISSION_REQUEST_READ_SMS = 100;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		checkSMSPermission();

		phoneNumberEditText = findViewById(R.id.editTextPhone);
		ageGroupRadioGroup = findViewById(R.id.age_group);
		vaccineTypeRadioGroup = findViewById(R.id.vaccine_type);
		paidTypeRadioGroup = findViewById(R.id.paid_type);
		searchByTypeRadioGroup = findViewById(R.id.search_by_type);
		userAddressInputFieldLinearLayout = findViewById(R.id.user_address_input_field);
		scanningIntervalTextView = findViewById(R.id.scanning_interval);
		secretEditText = findViewById(R.id.secret);

		HttpV2.versionCheckStatus(handler, new RedirectToNewAppRunnable());
	}

	class RedirectToNewAppRunnable implements Runnable{
		private String redirectionUrl;

		@Override
		public void run() {
			Toast.makeText(MainActivity.this, "New Version available, please download!", Toast.LENGTH_LONG).show();
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(redirectionUrl));
			startActivity(browserIntent);
			finish();
		}

		public void setRedirectionUrl(String redirectionUrl) {
			this.redirectionUrl = redirectionUrl;
		}
	}



	public void generateOtp(View view) throws InterruptedException, ExecutionException, TimeoutException, JSONException {
		EditText pinNumber = findViewById(R.id.pin_number);

		final String userPhoneNumber = phoneNumberEditText.getText().toString();
		final String userSelectedAge = ((RadioButton)findViewById(ageGroupRadioGroup.getCheckedRadioButtonId())).getText().toString();
		final String userSelectedVaccineType = ((RadioButton)findViewById(vaccineTypeRadioGroup.getCheckedRadioButtonId())).getText().toString();
		final String userSelectedPaidType = ((RadioButton)findViewById(paidTypeRadioGroup.getCheckedRadioButtonId())).getText().toString();
		final String userSelectedSearchByType = ((RadioButton)findViewById(searchByTypeRadioGroup.getCheckedRadioButtonId())).getText().toString();
		final String userPinNumber = searchByTypeString == "By PIN" ? pinNumber.getText().toString() : "";
		final int userDistrictCode = searchByTypeString == "By District" ? selectedDistrictCode : -1;

		// setting user preference
		App.userPreference.setUserPhoneNumber(userPhoneNumber);
		App.userPreference.setUserAge("Age 18+".equals(userSelectedAge) ? Age.AGE_18_PLUS : "Age 45+".equals(userSelectedAge) ? Age.AGE_45_PLUS : Age.ALL );
		App.userPreference.setUserSelectedVaccineType(StringToEnumMapper.mapStringToVaccinType(userSelectedVaccineType));
		App.userPreference.setUserSelectedPaidType("Paid".equals(userSelectedPaidType) ? PaidType.PAID : "Free".equals(userSelectedPaidType) ? PaidType.FREE : PaidType.ALL );
		App.userPreference.setUserSelectedSearchByType("By PIN".equals(searchByTypeString) ? SearchByType.BY_PIN : SearchByType.BY_DISTRICT);
		App.userPreference.setUserPinNumber(userPinNumber);
		App.userPreference.setUserDistrictCode(userDistrictCode);
		App.userPreference.setUserSecret(secretEditText.getText().toString());
		App.userPreference.setUserScanningInterval(userScanningInterval);

		Log.d(TAG, "startMonitoring: " + App.userPreference.toString());

		if(isAllFieldsValid(userPhoneNumber, userPinNumber, userDistrictCode)){
			HttpAsyncRunnable httpAsyncRunnable = new HttpAsyncRunnable(this);
			new Thread(httpAsyncRunnable).start();

		}else{
			Toast.makeText(this, "Some of the fields missing or not valid!", Toast.LENGTH_LONG).show();
		}

	}

	public Boolean isAllFieldsValid(String userPhoneNumber, String userPinNumber, int userDistrictCode){
		if(userPhoneNumber.length() != 10)
			return false;
		if(searchByTypeString == "By PIN"){
			if(userPinNumber.isEmpty())
				return false;
		}else{
			if(userDistrictCode == -1)
				return false;
		}
		return true;
	}

	public void provideCompatibleSearch(View view) {
		final String userSelectedSearchByType = ((RadioButton)findViewById(searchByTypeRadioGroup.getCheckedRadioButtonId())).getText().toString();
		for(int i = 0; i< userAddressInputFieldLinearLayout.getChildCount(); i++){
			userAddressInputFieldLinearLayout.removeViewAt(i);
		}

		if("By PIN".equals(userSelectedSearchByType)){
			searchByTypeString = "By PIN";
			LayoutInflater inflater=(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View pinView=inflater.inflate(R.layout.user_address_input_field, null);
			userAddressInputFieldLinearLayout.addView(pinView, 0);
		}else{
			// that means search by district
			searchByTypeString = "By District";
			LayoutInflater inflater=(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View statesDistrictView=inflater.inflate(R.layout.states_and_district, null);
			userAddressInputFieldLinearLayout.addView(statesDistrictView, 0);
			provideStates(this);
		}
	}

	public void provideStates(final Context mainActivityContext) {
		String url = "https://cdn-api.co-vin.in/api/v2/admin/location/states";
		JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, url, null,
				new Response.Listener<JSONObject>()
				{
					@Override
					public void onResponse(JSONObject response) {
						//Log.d(TAG, response.toString());
						try {
							JSONArray states = response.getJSONArray("states");
							Spinner stateSpinner = findViewById(R.id.states);
							final HashMap<String, Integer> map = new HashMap<String, Integer>();
							for (int i=0; i<states.length(); i++){
								map.put(states.getJSONObject(i).getString("state_name")
										, states.getJSONObject(i).getInt("state_id"));
							}
							ArrayAdapter<String> adapter = new ArrayAdapter<String>(mainActivityContext, android.R.layout.simple_spinner_item,  new ArrayList<>(map.keySet()));

							stateSpinner.setAdapter(adapter);

							stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
								@Override
								public void onItemSelected(AdapterView<?> parent, View view,
														   int position, long id) {
									Log.d(TAG, (String) parent.getItemAtPosition(position));
									String selectedState = (String) parent.getItemAtPosition(position);
									provideDistrict(mainActivityContext, map.get(selectedState));
								}

								@Override
								public void onNothingSelected(AdapterView<?> parent) {
									// TODO Auto-generated method stub
								}
							});

						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				},
				new Response.ErrorListener()
				{
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.d(TAG, error.toString());
					}
				}
		);

		App.httpRequestQueue.add(getRequest);

	}
	public void provideDistrict(final Context mainActivityContext, int stateCode) {
		String url = "https://cdn-api.co-vin.in/api/v2/admin/location/districts/" + stateCode;
		JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, url, null,
				new Response.Listener<JSONObject>()
				{
					@Override
					public void onResponse(JSONObject response) {
						//Log.d(TAG, response.toString());
						try {
							JSONArray districts = response.getJSONArray("districts");
							Log.d(TAG, "onResponse: " + districts);
							Spinner stateSpinner = findViewById(R.id.districts);
							final HashMap<String, Integer> map = new HashMap<String, Integer>();
							for (int i=0; i<districts.length(); i++){
								map.put(districts.getJSONObject(i).getString("district_name")
										, districts.getJSONObject(i).getInt("district_id"));
							}
							ArrayAdapter<String> adapter = new ArrayAdapter<String>(mainActivityContext, android.R.layout.simple_spinner_item,  new ArrayList<>(map.keySet()));

							stateSpinner.setAdapter(adapter);

							stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
								@Override
								public void onItemSelected(AdapterView<?> parent, View view,
														   int position, long id) {
									Log.d(TAG, (String) parent.getItemAtPosition(position));
									String selectedDistrict = (String) parent.getItemAtPosition(position);
									selectedDistrictCode = map.get(selectedDistrict);
								}

								@Override
								public void onNothingSelected(AdapterView<?> parent) {
									// TODO Auto-generated method stub
								}
							});

						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				},
				new Response.ErrorListener()
				{
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.d(TAG, error.toString());
					}
				}
		);

		App.httpRequestQueue.add(getRequest);

	}

	public void decreaseInterval(View view) {
		int scanningIntervalInSeconds = Integer.parseInt(scanningIntervalTextView.getText().toString());
		if(scanningIntervalInSeconds-1 > 0){
			scanningIntervalInSeconds-=1;
		}
		scanningIntervalTextView.setText("" + scanningIntervalInSeconds);
		userScanningInterval = scanningIntervalInSeconds;
	}

	public void increaseInterval(View view) {
		int scanningIntervalInSeconds = Integer.parseInt(scanningIntervalTextView.getText().toString());
		scanningIntervalInSeconds+=1;
		scanningIntervalTextView.setText("" + scanningIntervalInSeconds);
		userScanningInterval = scanningIntervalInSeconds;
	}

	public void startMonitoring(View view) {
		Intent serviceIntent = new Intent(this, ForegroundService.class);
		stopService(serviceIntent);
	}

	class HttpAsyncRunnable implements Runnable{
		final Context mainActivityContext;

		HttpAsyncRunnable(Context mainActivityContext) {
			this.mainActivityContext = mainActivityContext;
		}

		@Override
		public void run() {
			Http http = new Http(App.userPreference.getUserPhoneNumber());
			String txnId = null;
			try {
				txnId = http.sendOtp(App.userPreference.getUserSecret());
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
			Intent otpActivity = new Intent(mainActivityContext, OtpActivity.class);
			otpActivity.putExtra("txnId", txnId);
			startActivity(otpActivity);
		}
	}

	private void checkSMSPermission() {
		int smsReadPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);
		int smsReceivePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
		int internetPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
		int foregroundServicePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE);

		if(smsReadPermission == PackageManager.PERMISSION_DENIED){
			ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_SMS}, PERMISSION_REQUEST_READ_SMS );
		}
		if(smsReceivePermission == PackageManager.PERMISSION_DENIED){
			ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECEIVE_SMS}, 101 );
		}
		if(internetPermission == PackageManager.PERMISSION_DENIED){
			ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.INTERNET}, 102 );
		}
		if(foregroundServicePermission == PackageManager.PERMISSION_DENIED){
			ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.FOREGROUND_SERVICE}, 103 );
		}

	}

	@Override
	public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
		if (requestCode == PERMISSION_REQUEST_READ_SMS) {
			if (grantResults.length > 0
					&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Toast.makeText(this, "Request granted successfully.",
						Toast.LENGTH_LONG).show();
				Intent intent = getIntent();
				finish();
				startActivity(intent);
			} else {
				Toast.makeText(this,
						"SMS Read Request failed! App won't work.", Toast.LENGTH_LONG).show();
				Intent intent = getIntent();
				finish();
			}
		}

	}
}