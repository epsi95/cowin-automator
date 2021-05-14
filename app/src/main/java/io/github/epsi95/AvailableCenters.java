package io.github.epsi95;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

public class AvailableCenters extends AppCompatActivity {
	private EditText foundCenterTextView;
	private static final String TAG = "AvailableCenters";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_avaiable_centers);

		foundCenterTextView = findViewById(R.id.foundCenters);
		Intent intent = getIntent();
		String data = intent.getStringExtra("data");
		Log.d(TAG, "onCreate: " + data);
		String newData = "";
		String[] splittedData = data.split("\n");
		for(int i=1; i< splittedData.length; i++){
			newData += "✔ " + splittedData[i] + "\n\n";
		}
		foundCenterTextView.setText(newData);
	}
}