package io.github.epsi95;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class OtpActivity extends AppCompatActivity {

	private EditText otpEditText;
	private String transactionId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_otp);

		Intent intent = getIntent();
		String txnId = intent.getStringExtra("txnId");
		transactionId = txnId;
		otpEditText = findViewById(R.id.otp_edit_text);
	}

	public void submitOTP(View view) {
		final String otp = otpEditText.getText().toString();
		Intent beneficiaryIntent = new Intent(this, BeneficiaryActivity.class);
		beneficiaryIntent.putExtra("otp", otp);
		beneficiaryIntent.putExtra("txnId", transactionId);
		startActivity(beneficiaryIntent);
	}
}