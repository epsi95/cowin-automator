package io.github.epsi95;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {
	private EditText phoneNumberEditText;
	private RadioGroup ageGroup;
	private RadioGroup vaccineType;
	private RadioGroup paidType;
	private RadioGroup searchByType;
	private LinearLayout userAddressInputField;
	private EditText pinNumber;
	private TextView scanningInterval;
	private EditText secretEditText;
	private static final String TAG = "MainActivity";
	private int selectedDistrictCode;
	private String searchByTypeString = "By PIN";
	private int userScanningInterval = 60;
	private final int PERMISSION_REQUEST_READ_SMS = 100;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

//		checkSMSPermission();

		phoneNumberEditText = findViewById(R.id.editTextPhone);
		ageGroup = findViewById(R.id.age_group);
		vaccineType = findViewById(R.id.vaccine_type);
		paidType = findViewById(R.id.paid_type);
		searchByType = findViewById(R.id.search_by_type);
		userAddressInputField = findViewById(R.id.user_address_input_field);
		scanningInterval = findViewById(R.id.scanning_interval);
		secretEditText = findViewById(R.id.secret);

	}

	public void startMonitoring(View view) {
		pinNumber = findViewById(R.id.pin_number);

		final String userPhoneNumber = phoneNumberEditText.getText().toString();
		final String userSelectedAge = ((RadioButton)findViewById(ageGroup.getCheckedRadioButtonId())).getText().toString();
		final String userSelectedVaccineType = ((RadioButton)findViewById(vaccineType.getCheckedRadioButtonId())).getText().toString();
		final String userSelectedPaidType = ((RadioButton)findViewById(paidType.getCheckedRadioButtonId())).getText().toString();
		final String userSelectedSearchByType = ((RadioButton)findViewById(searchByType.getCheckedRadioButtonId())).getText().toString();
		final String userPinNumber = searchByTypeString == "By PIN" ? pinNumber.getText().toString() : "";
		final int userDistrictCode = searchByTypeString == "By District" ? selectedDistrictCode : -1;

		Log.d(TAG, "startMonitoring: " + userPhoneNumber + " " + userSelectedAge + " " +
				userSelectedVaccineType + " " + userSelectedPaidType + " " +
				userSelectedSearchByType + " " + userPinNumber + " " + userDistrictCode +
				" " + userScanningInterval);

		if(isAllFieldsValid(userPhoneNumber, userPinNumber, userDistrictCode)){
			Log.d(TAG, "startMonitoring: Start Background Service");

			Intent serviceIntent = new Intent(this, ForegroundService.class);
			serviceIntent.putExtra("otp", "");
			serviceIntent.putExtra("userPhoneNumber", userPhoneNumber);
			serviceIntent.putExtra("userSelectedAge", userSelectedAge);
			serviceIntent.putExtra("userSelectedVaccineType", userSelectedVaccineType);
			serviceIntent.putExtra("userSelectedPaidType", userSelectedPaidType);
			serviceIntent.putExtra("userSelectedSearchByType", userSelectedSearchByType);
			serviceIntent.putExtra("userPinNumber", userPinNumber);
			serviceIntent.putExtra("userDistrictCode", userDistrictCode);
			serviceIntent.putExtra("userScanningInterval", userScanningInterval);
			serviceIntent.putExtra("userSecret", secretEditText.getText().toString());
			startService(serviceIntent);
			Toast.makeText(this, "Monitoring has been started in the background. Check notification tray. You can close the app.", Toast.LENGTH_LONG).show();
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
		final String userSelectedSearchByType = ((RadioButton)findViewById(searchByType.getCheckedRadioButtonId())).getText().toString();
		for(int i=0; i<userAddressInputField.getChildCount(); i++){
			userAddressInputField.removeViewAt(i);
		}

		if("By PIN".equals(userSelectedSearchByType)){
			searchByTypeString = "By PIN";
			LayoutInflater inflater=(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View pinView=inflater.inflate(R.layout.user_address_input_field, null);
			userAddressInputField.addView(pinView, 0);
		}else{
			// that means search by district
			searchByTypeString = "By District";
			LayoutInflater inflater=(LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			final View statesDistrictView=inflater.inflate(R.layout.states_and_district, null);
			userAddressInputField.addView(statesDistrictView, 0);
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

		App.queue.add(getRequest);

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

		App.queue.add(getRequest);

	}

	public void decreaseInterval(View view) {
		int scanningIntervalInSeconds = Integer.parseInt(scanningInterval.getText().toString());
		if(scanningIntervalInSeconds-10 >= 10){
			scanningIntervalInSeconds-=10;
		}
		scanningInterval.setText("" + scanningIntervalInSeconds);
		userScanningInterval = scanningIntervalInSeconds;
	}

	public void increaseInterval(View view) {
		int scanningIntervalInSeconds = Integer.parseInt(scanningInterval.getText().toString());
		scanningIntervalInSeconds+=10;
		scanningInterval.setText("" + scanningIntervalInSeconds);
		userScanningInterval = scanningIntervalInSeconds;
	}

	public void stopMonitoring(View view) {
		Intent serviceIntent = new Intent(this, ForegroundService.class);
		stopService(serviceIntent);
		Toast.makeText(this, "Monitoring turned off!", Toast.LENGTH_LONG).show();
	}
//	private void checkSMSPermission() {
//		int smsReadPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS);
//		int smsReceivePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
//		int internetPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
//		int foregroundServicePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE);
//
//		if(smsReadPermission == PackageManager.PERMISSION_DENIED){
//			ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_SMS}, 100 );
//		}
//		if(smsReceivePermission == PackageManager.PERMISSION_DENIED){
//			ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECEIVE_SMS}, 101 );
//		}
//		if(internetPermission == PackageManager.PERMISSION_DENIED){
//			ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.INTERNET}, 102 );
//		}
//		if(foregroundServicePermission == PackageManager.PERMISSION_DENIED){
//			ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.FOREGROUND_SERVICE}, 103 );
//		}
//
//	}
}