package io.github.epsi95;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class CaptchaSubmitActivity extends AppCompatActivity {

	private static final String TAG = "CaptchaSubmitActivity";

	private volatile String token;
	private volatile ArrayList<CenterSession> availableSession = new ArrayList<>();
	private volatile boolean isWaitingForUserInput = false;
	private volatile ArrayList<String> sessionNameInStringFormat = new ArrayList<>();
	private volatile CenterSession userSelectedSession;

	private ImageView captchaImageView;
	private Drawable imageDrawable;
	private LinearLayout availableCenterLinearLayout;
	private Button waitingButton;
	private EditText captchaEditText;

	private Handler handler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_captcha_submit);

		captchaImageView = findViewById(R.id.captcha_image_view);
		waitingButton = findViewById(R.id.waiting_button);
		availableCenterLinearLayout = findViewById(R.id.available_centers);
		captchaEditText = findViewById(R.id.captcha_text);
	}

	@Override
	protected void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(this).registerReceiver(
				centerFoundBroadcastReceiver, new IntentFilter("CowinCenterFound"));
	}
	public void showToast(String message){
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	public void tryToBook(View view) {
		if(!(userSelectedSession != null && token != null && captchaEditText.getText().toString().trim().length()>0)){
			Toast.makeText(this, "Not possible now!", Toast.LENGTH_SHORT).show();
			return;
		}
		Log.d(TAG, "tryToBook: Trying to book with these preferences" + App.userPreference.toString() + " " + userSelectedSession);
		if(App.userPreference.getAppointmentType() == AppointmentType.RESCHEDULE){
			new Thread(new Runnable() {
				@Override
				public void run() {
					Http http = new Http(App.userPreference.getUserPhoneNumber());
					final SlotBookingResult result = http.rescheduleAppointment(captchaEditText.getText().toString().trim(),
							userSelectedSession.getSessionId(),
							userSelectedSession.getSlots(),
							token);
					handler.post(new Runnable() {
						@Override
						public void run() {
							if (result.isBookingSuccess()){
								showToast("✔ Booking successful \n" + result.getResponse());
							}else{
								showToast("🔴 Booking was not successful \n" + result.getResponse());
								isWaitingForUserInput = false;
							}
						}
					});
				}
			}).start();
		}else{
			// normal booking
			new Thread(new Runnable() {
				@Override
				public void run() {
					Http http = new Http(App.userPreference.getUserPhoneNumber());
					final SlotBookingResult result = http.scheduleAppointment(captchaEditText.getText().toString().trim(),
							userSelectedSession.getSessionId(),
							userSelectedSession.getSlots(),
							token);
					handler.post(new Runnable() {
						@Override
						public void run() {
							if (result.isBookingSuccess()){
								showToast("✔ Booking successful \n" + result.getResponse());
							}else{
								showToast("🔴 Booking was not successful \n" + result.getResponse());
								isWaitingForUserInput = false;
							}
						}
					});
				}
			}).start();
		}
	}

	public void populateSessions(){
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View availableCenterView = inflater.inflate(R.layout.beneficiaries, null);
		for(int i=0; i<availableCenterLinearLayout.getChildCount(); i++){
			availableCenterLinearLayout.removeViewAt(i);
		}
		availableCenterLinearLayout.addView(availableCenterView, 0);

		Spinner captchaSpinner = findViewById(R.id.beneficiaries_spinner);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
				sessionNameInStringFormat);
		captchaSpinner.setAdapter(adapter);

		captchaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int position, long id) {
				Log.d(TAG, availableSession.get(position).toString());
				userSelectedSession = availableSession.get(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
			}
		});
	}

	public void toggleWaitingStatus(View view) {
		waitingButton.setText("Listening to availability...");
		isWaitingForUserInput = false;
	}

	public void showDeveloperPage(View view) {
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://epsi95.github.io/my-website/"));
		startActivity(browserIntent);
	}

	class ShowCaptchaRunnable implements Runnable{

		@Override
		public void run() {
			Http http = new Http(App.userPreference.getUserPhoneNumber());
			try {
				String captcha = http.getCaptcha(token);
				SVG svgCaptcha = SVGParser.getSVGFromString(captcha);
				imageDrawable = svgCaptcha.createPictureDrawable();
				handler.post(new Runnable() {
					@Override
					public void run() {
						if(imageDrawable != null){
							captchaImageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
							captchaImageView.setScaleType(ImageView.ScaleType.FIT_XY);
							captchaImageView.setAdjustViewBounds(true);
							captchaImageView.setImageDrawable(imageDrawable);
						}
					}
				});

			} catch (InterruptedException | ExecutionException | TimeoutException | JSONException e) {
				e.printStackTrace();
			}
		}
	}

	private BroadcastReceiver centerFoundBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(!isWaitingForUserInput){
				try {
					waitingButton.setText("Tap to Listening to availability...");
					isWaitingForUserInput = true;
					captchaEditText.setText("");

					token = intent.getStringExtra("token");
					JSONArray arr = new JSONArray(intent.getStringExtra("data"));
					arr = shuffleJsonArray(arr);

					Log.d(TAG, "onReceive: Received Broadcast from ForegroundService " + arr);

					for(int i=0; i<arr.length(); i++){
						Center tempCenter = new Center(arr.getJSONObject(i));
						for(CenterSession cs: tempCenter.getSessions()){
							availableSession.add(cs);
							sessionNameInStringFormat.add(tempCenter.getName() + "--" + tempCenter.getAddress()
									+ "--" + cs.getDate());
						}
						userSelectedSession = availableSession.get(0);
					}
					new Thread(new ShowCaptchaRunnable()).start();
					handler.post(new Runnable() {
						@Override
						public void run() {
							populateSessions();
						}
					});

				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	};

	// https://stackoverflow.com/questions/5531130/an-efficient-way-to-shuffle-a-json-array-in-java
	public static JSONArray shuffleJsonArray (JSONArray array) throws JSONException {
		// Implementing Fisher–Yates shuffle
		Random rnd = new Random();
		for (int i = array.length() - 1; i >= 0; i--)
		{
			int j = rnd.nextInt(i + 1);
			// Simple swap
			Object object = array.get(j);
			array.put(j, array.get(i));
			array.put(i, object);
		}
		return array;
	}
	@Override
	protected void onPause() {
		super.onPause();
		try {
			unregisterReceiver(centerFoundBroadcastReceiver);
		}catch (Exception e){
			e.printStackTrace();
			//pass
		}

	}
}