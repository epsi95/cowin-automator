package io.github.epsi95;


import android.os.Handler;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpV2 {
	private static final String TAG = "HttpV2";

	public static SlotBookingResult post_v2(String url, JSONObject body, final String token) {
		OkHttpClient client = new OkHttpClient();
		final MediaType JSON
				= MediaType.parse("application/json; charset=utf-8");

		Request request = new Request.Builder()
				.url(url)
				.post(RequestBody.create(JSON, body.toString()))
				.addHeader("Content-Type", "application/json")
				.addHeader("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36")
				.addHeader("Authorization", "Bearer " + token)
				.build();

		Call call = client.newCall(request);
		try {
			Response response = call.execute();
//			Log.d(TAG, "post_v2: OK_ HTTP " + response.toString());
//			Log.d(TAG, "post_v2: OK_ HTTP " + response.code());
//			Log.d(TAG, "post_v2: OK_ HTTP " + response.body().string());
//			Log.d(TAG, "post_v2: BODY " + body.toString());
			if (response.code() == 200) {
				return new SlotBookingResult(true, response.code() + "\n" + response.body().string());
			} else {
				return new SlotBookingResult(false, response.code() + "\n" + response.body().string());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new SlotBookingResult(false, "Exception in OkHttp " + e.toString());
		}

	}

	public static void versionCheckStatus(final Handler handler, final MainActivity.RedirectToNewAppRunnable runnable) {
		OkHttpClient client = new OkHttpClient();

		Request request = new Request.Builder()
				.url("https://cowinautomator.free.beeceptor.com/")
				.build();

		Call call = client.newCall(request);

		call.enqueue(new Callback() {
			public void onResponse(Call call, Response response) throws IOException {

				JSONObject r = null;
				try {
					r = new JSONObject(response.body().string());
					VersionCheckStatus vcs = new VersionCheckStatus(!r.getString("version").equals("1"), r.getString("redirect_to"));
					if(vcs.isShouldRedirect()){
						runnable.setRedirectionUrl(vcs.getRedirectUrl());
						handler.post(runnable);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			public void onFailure(Call call, IOException e) {
				// pass
				// don't care
			}
		});
	}
}
