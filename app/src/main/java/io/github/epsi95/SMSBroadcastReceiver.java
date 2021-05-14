package io.github.epsi95;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMSBroadcastReceiver extends BroadcastReceiver {
	final SmsManager sms = SmsManager.getDefault();
	private static final String TAG = "SMSBroadcastReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		final Bundle bundle = intent.getExtras();
		Log.d(TAG, "onReceive: SMS received");

		try {

			if (bundle != null) {

				final Object[] pdusObj = (Object[]) bundle.get("pdus");

				for (int i = 0; i < pdusObj.length; i++) {

					SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
					String phoneNumber = currentMessage.getDisplayOriginatingAddress();

					String senderNum = phoneNumber;
					String message = currentMessage.getDisplayMessageBody();

					Log.d(TAG, "senderNum: "+ senderNum + "; message: " + message);
//					sample
//					senderNum: AX-NHPSMS;
//					message: Your OTP to register/access CoWIN is 351187. It will be valid for 3 minutes. - CoWIN

					if(message.toLowerCase().contains("cowin")){
						final String regex = "([\\d]{6})";

						final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
						final Matcher matcher = pattern.matcher(message);
						if(matcher.find()){
							Intent serviceIntent = new Intent(context, ForegroundService.class);
							serviceIntent.putExtra("otp", matcher.group(0));
							context.startService(serviceIntent);
						}
					}

				} // end for loop
			} // bundle is null

		} catch (Exception e) {
			Log.d(TAG, "Exception smsReceiver" +e);

		}
	}
}
