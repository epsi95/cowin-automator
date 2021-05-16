package io.github.epsi95;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
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

import java.security.NoSuchAlgorithmException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class BeneficiaryActivity extends AppCompatActivity {
	private static final String TAG = "BeneficiaryActivity";

	private LinearLayout beneficiaryLinearLayout;
	private TextView beneficiaryMessage;
	private RadioGroup searchTimeRadioButtonGroup;
	private RadioGroup doseNumberRadioButtonGroup;
	private RadioGroup appointmentTypeRadioGroup;
	private CheckBox shouldConsiderDoseWiseSlotCheckBox;

	private String txnId;
	private String otp;
	private ArrayList<Beneficiary> beneficiaries;
	private Handler handler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_beneficiary);

		Intent intent = getIntent();
		otp = intent.getStringExtra("otp");
		txnId = intent.getStringExtra("txnId");

		beneficiaryLinearLayout = findViewById(R.id.beneficiaries_id);
		beneficiaryMessage = findViewById(R.id.message_beneficiary);
		searchTimeRadioButtonGroup = findViewById(R.id.user_preference_time);
		doseNumberRadioButtonGroup = findViewById(R.id.dose_number_radio_group);
		appointmentTypeRadioGroup = findViewById(R.id.appointment_type_radiogroup);
		shouldConsiderDoseWiseSlotCheckBox = findViewById(R.id.checkBox);

		BeneficiaryRunnable beneficiaryRunnable = new BeneficiaryRunnable();
		new Thread(beneficiaryRunnable).start();

	}

	public void getBeneficiaries() {

		App.userPreference.setBeneficiary(beneficiaries.get(0));
		beneficiaryMessage.setText("Beneficiary Found");

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View beneficiaryView = inflater.inflate(R.layout.beneficiaries, null);
		beneficiaryLinearLayout.addView(beneficiaryView, 1);

		Spinner beneficiarySpinner = findViewById(R.id.beneficiaries_spinner);
		ArrayList<String> beneficiaryNames = new ArrayList<>();
		for (Beneficiary b : beneficiaries) {
			beneficiaryNames.add(b.getName());
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
				beneficiaryNames);
		beneficiarySpinner.setAdapter(adapter);

		beneficiarySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int position, long id) {
				Log.d(TAG, beneficiaries.get(position).toString());
				App.userPreference.setBeneficiary(beneficiaries.get(position));
				if(App.userPreference.getBeneficiary().getAppointments().length()>0){
					appointmentTypeRadioGroup.check(R.id.radioButton17);
					App.userPreference.setAppointmentType(AppointmentType.RESCHEDULE);
				}else{
					appointmentTypeRadioGroup.check(R.id.radioButton16);
					App.userPreference.setAppointmentType(AppointmentType.FRESH);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
			}
		});
	}

	public void stopMonitoring(View view) {
		Intent serviceIntent = new Intent(this, ForegroundService.class);
		stopService(serviceIntent);
		Toast.makeText(this, "Monitoring turned off!", Toast.LENGTH_LONG).show();
	}

	public void startMonitoring(View view) {
		if(App.userPreference.getBeneficiary() != null){
			Log.d(TAG, "startMonitoring: Starting monitoring with this user preference " + App.userPreference.toString());
			Intent serviceIntent = new Intent(this, ForegroundService.class);
			serviceIntent.putExtra("otp", "");
			startService(serviceIntent);

			Intent captchaActivityIntent = new Intent(this, CaptchaSubmitActivity.class);
			startActivity(captchaActivityIntent);
		}else{
			Toast.makeText(this, "Beneficiary data missing!", Toast.LENGTH_SHORT).show();
		}

	}

	public void setUserPreferredTime(View view) {
		final String searchTime = ((RadioButton)findViewById(searchTimeRadioButtonGroup.getCheckedRadioButtonId())).getText().toString();
		if("I like to book in the first half of the day".equals(searchTime)){
			App.userPreference.setUserPreferredBookingDirection(1);
		}else{
			App.userPreference.setUserPreferredBookingDirection(-1);
		}
		Log.d(TAG, "setUserPreferredTime: " + App.userPreference.getUserPreferredBookingDirection());
	}

	public void setDoseNumber(View view) {
		final String doseNumber = ((RadioButton)findViewById(doseNumberRadioButtonGroup.getCheckedRadioButtonId())).getText().toString();
		App.userPreference.setDoseNumber(Integer.parseInt(doseNumber));
		Log.d(TAG, "setDoseNumber: " + App.userPreference.getDoseNumber());
	}

	public void setAppointmentType(View view) throws JSONException {
		final String appointmentType = ((RadioButton)findViewById(appointmentTypeRadioGroup.getCheckedRadioButtonId())).getText().toString();
		if("Fresh Appointment".equals(appointmentType)){
			JSONArray appointments = App.userPreference.getBeneficiary().getAppointments();
			if(appointments.length()>0){
				Toast.makeText(this, "You can't do fresh appointment, do reschedule!", Toast.LENGTH_LONG).show();
				appointmentTypeRadioGroup.check(R.id.radioButton17);
				return;
			}
			App.userPreference.setAppointmentType(AppointmentType.FRESH);
		}else{
			JSONArray appointments = App.userPreference.getBeneficiary().getAppointments();
			if(appointments.length()<1){
				Toast.makeText(this, "No past appointments found!", Toast.LENGTH_LONG).show();
				appointmentTypeRadioGroup.check(R.id.radioButton16);
				return;
			}
			App.userPreference.setAppointmentType(AppointmentType.RESCHEDULE);
			App.userPreference.setLastAppointmentId(appointments.getJSONObject(0).getString("appointment_id"));
		}
		Log.d(TAG, "setAppointmentType: " + App.userPreference.getAppointmentType());
	}

	public void changeCheckBoxStatus(View view) {
		if(shouldConsiderDoseWiseSlotCheckBox.isChecked()){
			App.userPreference.setShouldDoseWiseSlotBeConsidered(true);
		}else{
			App.userPreference.setShouldDoseWiseSlotBeConsidered(false);
		}
		Log.d(TAG, "changeCheckBoxStatus: checkbox selected " + App.userPreference.isShouldDoseWiseSlotBeConsidered());
	}

	class BeneficiaryRunnable implements Runnable {

		@Override
		public void run() {
			Http http = new Http(App.userPreference.getUserPhoneNumber());
			try {
				String token = http.validateOtp(txnId, otp);
				beneficiaries = http.getBeneficiaries(token);
				handler.post(new Runnable() {
					@Override
					public void run() {
						getBeneficiaries();
					}
				});
			} catch (JSONException | InterruptedException | TimeoutException | NoSuchAlgorithmException | ExecutionException e) {
				e.printStackTrace();
			}
		}
	}
}