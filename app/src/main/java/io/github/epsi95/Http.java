package io.github.epsi95;

import android.util.Log;

import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.RequestBody;

public class Http {
	private static final String TAG = "Http";

	public static final String generateOtpUrl = "https://cdn-api.co-vin.in/api/v2/auth/generateMobileOTP";
	public static final String validateOtpUrl = "https://cdn-api.co-vin.in/api/v2/auth/validateMobileOtp";
	public static final String beneficiaryDetailsUrl = "https://cdn-api.co-vin.in/api/v2/appointment/beneficiaries";
	public static final String getCaptchaUrl = "https://cdn-api.co-vin.in/api/v2/auth/getRecaptcha";
	public static final String rescheduleAppointmentUrl = "https://cdn-api.co-vin.in/api/v2/appointment/reschedule";
	public static final String scheduleAppointmentUrl = "https://cdn-api.co-vin.in/api/v2/appointment/schedule";
	private final HashMap<String, String> headers;
	private final String mobileNumber;

	public class MyJsonObjectRequest extends JsonObjectRequest {
		private int statusCode;

		public int getStatusCode() {
			return statusCode;
		}

		public MyJsonObjectRequest(int method, String url, @Nullable JSONObject jsonRequest, Response.Listener<JSONObject> listener, @Nullable Response.ErrorListener errorListener) {
			super(method, url, jsonRequest, listener, errorListener);
		}

		public MyJsonObjectRequest(String url, @Nullable JSONObject jsonRequest, Response.Listener<JSONObject> listener, @Nullable Response.ErrorListener errorListener) {
			super(url, jsonRequest, listener, errorListener);
		}

		@Override
		protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
			Log.d(TAG, "post: STATUS_CODE>>> " + response.statusCode);
			statusCode = response.statusCode;
			return super.parseNetworkResponse(response);
		}
	}

	public Http(String mobileNumber) {
		this.headers = new HashMap<String, String>();
		headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36");
		headers.put("'Content-Type", "application/json");
		this.mobileNumber = mobileNumber;
	}

	public JSONObject post(String url, JSONObject body, final String token) throws InterruptedException, ExecutionException, TimeoutException {
		RequestFuture<JSONObject> requestFuture = RequestFuture.newFuture();

		MyJsonObjectRequest request = new MyJsonObjectRequest(Request.Method.POST,
				url,
				body,
				requestFuture,
				requestFuture) {
			@Override
			public Map<String, String> getHeaders() {
				if (token != null) {
					headers.put("Authorization", "Bearer " + token);
				}
				return headers;
			}
		};
		App.httpRequestQueue.add(request);
		try {
			JSONObject object = requestFuture.get(10, TimeUnit.SECONDS);
			return object;
		} catch (Exception e) {
			Log.d(TAG, "post: STATUS_CODE " + request.getStatusCode());
			throw e;
		}

	}

	public JSONObject get(String url, final String token) throws InterruptedException, ExecutionException, TimeoutException {
		RequestFuture<JSONObject> requestFuture = RequestFuture.newFuture();
		MyJsonObjectRequest request = new MyJsonObjectRequest(Request.Method.GET,
				url,
				null,
				requestFuture,
				requestFuture) {
			@Override
			public Map<String, String> getHeaders() {
				headers.put("Authorization", "Bearer " + token);
				return headers;
			}
		};
		App.httpRequestQueue.add(request);
		JSONObject object = requestFuture.get(10, TimeUnit.SECONDS);
		return object;
	}


	public String sendOtp(String secret) throws JSONException, InterruptedException, ExecutionException, TimeoutException {
		final JSONObject body = new JSONObject();
		body.put("mobile", mobileNumber);
		body.put("secret", secret);
		return post(generateOtpUrl, body, null).getString("txnId");
	}

	public String validateOtp(String transactionId, String otp) throws JSONException, InterruptedException, ExecutionException, TimeoutException, NoSuchAlgorithmException {
		final JSONObject body = new JSONObject();
		body.put("txnId", transactionId);
		body.put("otp", toHexString(getSHA(otp)));
		return post(validateOtpUrl, body, null).getString("token");
	}

	public ArrayList<Beneficiary> getBeneficiaries(String token) throws InterruptedException, ExecutionException, TimeoutException, JSONException {
		JSONArray beneficiariesJSONArray = get(beneficiaryDetailsUrl, token).getJSONArray("beneficiaries");
		ArrayList<Beneficiary> beneficiaries = new ArrayList<>();
		for (int i = 0; i < beneficiariesJSONArray.length(); i++) {
			beneficiaries.add(new Beneficiary(beneficiariesJSONArray.getJSONObject(i)));
		}
		return beneficiaries;
	}

	public JSONArray getCentersByPinCode(String token, String pinCode, String date) throws InterruptedException, ExecutionException, TimeoutException, JSONException {
		String url = calendarByPin(pinCode, date);
		JSONObject response = get(url, token);
		return parseAndFilterCenterData(response);
	}

	public JSONArray getCentersByDistrictId(String token, String districtId, String date) throws InterruptedException, ExecutionException, TimeoutException, JSONException {
		String url = calendarByDistrict(districtId, date);
		JSONObject response = get(url, token);
		return parseAndFilterCenterData(response);
	}

	public JSONArray parseAndFilterCenterData(JSONObject object) throws JSONException {
		Log.d(TAG, "parseAndFilterCenterData: Parsing and filtering JSON " + object);
		JSONArray centersJSONArray = object.getJSONArray("centers");
		JSONArray centersParsed = new JSONArray();

		for (int i = 0; i < centersJSONArray.length(); i++) {
			Center temporaryCenter = new Center(centersJSONArray.getJSONObject(i));
			if (temporaryCenter.getAnySlotAvailable()) {
				if (temporaryCenter.getSessions().size() > 0) {
					centersParsed.put(temporaryCenter.toJSONObject());
				}
			}

		}
		return centersParsed;
	}

	public static String calendarByDistrict(String districtCode, String date) {
		return "https://cdn-api.co-vin.in/api/v2/appointment/sessions/calendarByDistrict?district_id=" + districtCode + "&date=" + date;
	}

	public static String calendarByPin(String pinNumber, String date) {
		return "https://cdn-api.co-vin.in/api/v2/appointment/sessions/calendarByPin?pincode=" + pinNumber + "&date=" + date;
	}

	public String getCaptcha(String token) throws InterruptedException, ExecutionException, TimeoutException, JSONException {
		return deNoiseCaptcha(post(getCaptchaUrl, new JSONObject(), token).getString("captcha"));
	}

	public String deNoiseCaptcha(String captcha) {
		final String regex = "<path d=.*?fill=\"none\"/>";
		final String subst = "";

		final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(captcha);

		// The substituted value will be contained in the result variable
		return matcher.replaceAll(subst);

	}

	public SlotBookingResult rescheduleAppointment(String captcha, String sessionId, ArrayList<String> slots, String token) {
		Log.d(TAG, "post_v2: OK_ HTTP reschedule API gets Called");
		SlotBookingResult slotBookingResult = new SlotBookingResult(false, "");
		if (App.userPreference.getUserPreferredBookingDirection() == 1) {
			for (int i = 0; i < slots.size(); i++) {
				final JSONObject body = new JSONObject();
				try {
					body.put("ben_appointment_id", App.userPreference.getLastAppointmentId());
					body.put("session_id", sessionId);
					body.put("slot", slots.get(i));
					body.put("captcha", captcha);
					body.put("dose", App.userPreference.getDoseNumber());
				} catch (JSONException e) {
					return new SlotBookingResult(false, "JSON exception");
				}

				Log.d(TAG, "rescheduleAppointment: ReschedulingAppointment called" + body);
				slotBookingResult = HttpV2.post_v2(rescheduleAppointmentUrl, body, token);
				if (slotBookingResult.isBookingSuccess()) {
					return slotBookingResult;
				}
			}
		} else {
			for (int i = slots.size() - 1; i >= 0; i--) {
				final JSONObject body = new JSONObject();
				try {
					body.put("ben_appointment_id", App.userPreference.getLastAppointmentId());
					body.put("session_id", sessionId);
					body.put("slot", slots.get(i));
					body.put("captcha", captcha);
					body.put("dose", App.userPreference.getDoseNumber());
				} catch (JSONException e) {
					return new SlotBookingResult(false, "JSON exception");
				}
				Log.d(TAG, "rescheduleAppointment: ReschedulingAppointment called" + body);
				slotBookingResult = HttpV2.post_v2(rescheduleAppointmentUrl, body, token);
				if (slotBookingResult.isBookingSuccess()) {
					return slotBookingResult;
				}
			}
		}
		return slotBookingResult;
	}

	public SlotBookingResult scheduleAppointment(String captcha, String sessionId, ArrayList<String> slots, String token) {
		Log.d(TAG, "post_v2: OK_ HTTP schedule API gets Called");
		SlotBookingResult slotBookingResult = new SlotBookingResult(false, "");
		if (App.userPreference.getUserPreferredBookingDirection() == 1) {
			for (int i = 0; i < slots.size(); i++) {
				JSONArray bf = new JSONArray();
				bf.put(App.userPreference.getBeneficiary().getBeneficiaryReferenceId());
				final JSONObject body = new JSONObject();
				try {
					body.put("session_id", sessionId);
					body.put("slot", slots.get(i));
					body.put("beneficiaries", bf);
					body.put("captcha", captcha);
					body.put("dose", App.userPreference.getDoseNumber());
				} catch (JSONException e) {
					return new SlotBookingResult(false, "JSON exception");
				}
				Log.d(TAG, "rescheduleAppointment: ReschedulingAppointment called" + body);
				slotBookingResult = HttpV2.post_v2(scheduleAppointmentUrl, body, token);
				if (slotBookingResult.isBookingSuccess()) {
					return slotBookingResult;
				}
			}
		} else {
			for (int i = slots.size() - 1; i >= 0; i--) {
				JSONArray bf = new JSONArray();
				bf.put(App.userPreference.getBeneficiary().getBeneficiaryReferenceId());
				final JSONObject body = new JSONObject();
				try {
					body.put("session_id", sessionId);
					body.put("slot", slots.get(i));
					body.put("beneficiaries", bf);
					body.put("captcha", captcha);
					body.put("dose", App.userPreference.getDoseNumber());
				} catch (JSONException e) {
					return new SlotBookingResult(false, "JSON exception");
				}
				Log.d(TAG, "rescheduleAppointment: ReschedulingAppointment called" + body);
				slotBookingResult = HttpV2.post_v2(scheduleAppointmentUrl, body, token);
				if (slotBookingResult.isBookingSuccess()) {
					return slotBookingResult;
				}
			}
		}
		return slotBookingResult;
	}


	// SHA-256 encryption utility
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

}
