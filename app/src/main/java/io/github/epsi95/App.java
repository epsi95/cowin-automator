package io.github.epsi95;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class App extends Application {
	public static RequestQueue httpRequestQueue;
	public static final String CHANNEL_ID = "cowin_automate_app_channel";
	public static UserPreference userPreference;
	@Override
	public void onCreate() {
		super.onCreate();
		userPreference = new UserPreference();
		httpRequestQueue = Volley.newRequestQueue(this);
		createNotificationChannel();
	}
	private void createNotificationChannel() {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
			NotificationChannel notificationChannel = new NotificationChannel(
					CHANNEL_ID,
					"Cowin Automator",
					NotificationManager.IMPORTANCE_DEFAULT
			);

			NotificationManager manager = getSystemService(NotificationManager.class);
			manager.createNotificationChannel(notificationChannel);
		}
	}
}
