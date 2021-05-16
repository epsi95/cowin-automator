package io.github.epsi95;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;



public class Center {

	private static final String TAG = "Center";

	private int centerId;
	private String name;
	private String address;
	private String stateName;
	private String districtName;
	private String blockName;
	private int pinCode;
	private String from;
	private String to;
	private PaidType feeType;
	private ArrayList<CenterSession> sessions;
	private Boolean isAnySlotAvailable;

	Center(JSONObject centerJsonObject) throws JSONException {
		Log.d(TAG, "Center: Creating Center object from JSON " + centerJsonObject);

		this.centerId = centerJsonObject.getInt("center_id");
		this.name = centerJsonObject.getString("name");
		this.address = centerJsonObject.getString("address");
		this.stateName = centerJsonObject.getString("state_name");
		this.districtName = centerJsonObject.getString("district_name");
		this.blockName = centerJsonObject.getString("block_name");
		this.pinCode = centerJsonObject.getInt("pincode");
		this.from = centerJsonObject.getString("from");
		this.to = centerJsonObject.getString("to");
		this.feeType = centerJsonObject.getString("fee_type").toUpperCase().equals("PAID") ? PaidType.PAID: PaidType.FREE;
		this.sessions = new ArrayList<>();
		this.isAnySlotAvailable = false;

		for(int i=0; i<centerJsonObject.getJSONArray("sessions").length(); i++){
			JSONObject currentSession = centerJsonObject.getJSONArray("sessions").getJSONObject(i);

			boolean thereIsNoAvailableDose = ((App.userPreference.getDoseNumber() == 1 && currentSession.getInt("available_capacity_dose1") < 1)
					||
					(App.userPreference.getDoseNumber() == 2 && currentSession.getInt("available_capacity_dose2") < 1));
			boolean userHasSelectedAllForFilter = (App.userPreference.getUserSelectedPaidType().equals(PaidType.ALL) && App.userPreference.getUserAge().equals(Age.ALL) && App.userPreference.getUserSelectedVaccineType().equals(VaccineType.ALL));
			boolean userPaidTypeFilerMatches = (App.userPreference.getUserSelectedPaidType().equals(this.feeType) || App.userPreference.getUserSelectedPaidType().equals(PaidType.ALL));
			boolean userAgeFilterMatches = (App.userPreference.getUserAge().equals(currentSession.getInt("min_age_limit") == 18 ? Age.AGE_18_PLUS : Age.AGE_45_PLUS) || App.userPreference.getUserAge().equals(Age.ALL));
			boolean userVaccineTypeFilterMatches = (App.userPreference.getUserSelectedVaccineType().equals(StringToEnumMapper.mapStringToVaccinType(currentSession.getString("vaccine").toUpperCase())) || App.userPreference.getUserSelectedVaccineType().equals(VaccineType.ALL));

			//thereIsNoAvailableDose is not used
			// it can be uses just use
			// shouldDelete = shouldDelete || thereIsNoAvailableDose
			boolean shouldDelete = !(userHasSelectedAllForFilter ||
					(userPaidTypeFilerMatches
							&& userAgeFilterMatches
							&& userVaccineTypeFilterMatches
					));
			if(App.userPreference.isShouldDoseWiseSlotBeConsidered()){
				shouldDelete = shouldDelete || thereIsNoAvailableDose;
			}

			if(currentSession.getInt("available_capacity") > 0 && !shouldDelete ){
				ArrayList<String> slots = new ArrayList<>();
				for(int j=0; j<currentSession.getJSONArray("slots").length(); j++){
					slots.add(currentSession.getJSONArray("slots").getString(j));
				}
				if(!isAnySlotAvailable){
					isAnySlotAvailable = true;
				}
				this.sessions.add(new CenterSession(
						currentSession.getString("session_id"),
						currentSession.getString("date"),
						currentSession.getInt("available_capacity"),
						currentSession.getInt("min_age_limit") == 18 ? Age.AGE_18_PLUS : Age.AGE_45_PLUS,
						StringToEnumMapper.mapStringToVaccinType(currentSession.getString("vaccine").toUpperCase().trim()),
						slots,
						currentSession.getInt("available_capacity")>0,
						currentSession.getInt("available_capacity_dose1"),
						currentSession.getInt("available_capacity_dose2")));
			}
		}

		// further processing
		Log.d(TAG, "Center: And the made object is " + this);

	}



	public void deleteSession(CenterSession session){
		this.sessions.remove(session);
	}
	public int getCenterId() {
		return centerId;
	}

	public String getName() {
		return name;
	}

	public String getAddress() {
		return address;
	}

	public String getStateName() {
		return stateName;
	}

	public String getDistrictName() {
		return districtName;
	}

	public String getBlockName() {
		return blockName;
	}

	public int getPinCode() {
		return pinCode;
	}

	public String getFrom() {
		return from;
	}

	public String getTo() {
		return to;
	}

	public PaidType getFeeType() {
		return feeType;
	}

	public ArrayList<CenterSession> getSessions() {
		return sessions;
	}

	public Boolean getAnySlotAvailable() {
		return isAnySlotAvailable;
	}

	@Override
	public String toString() {
		return "Center{" +
				"centerId=" + centerId +
				", name='" + name + '\'' +
				", address='" + address + '\'' +
				", stateName='" + stateName + '\'' +
				", districtName='" + districtName + '\'' +
				", blockName='" + blockName + '\'' +
				", pinCode=" + pinCode +
				", from='" + from + '\'' +
				", to='" + to + '\'' +
				", feeType=" + feeType +
				", sessions=" + sessions +
				", isAnySlotAvailable=" + isAnySlotAvailable +
				'}';
	}

	public JSONObject toJSONObject() throws JSONException {
		JSONObject center = new JSONObject();
		center.put("center_id", centerId);
		center.put("name", name);
		center.put("address", address);
		center.put("state_name", stateName);
		center.put("district_name", districtName);
		center.put("block_name", blockName);
		center.put("pincode", pinCode);
		center.put("from", from);
		center.put("to", to);
		center.put("fee_type", feeType==PaidType.FREE ? "Free" : "Paid");

		JSONArray session = new JSONArray();
		for(CenterSession cs: this.getSessions()){
			session.put(cs.toJSONObject());
		}
		center.put("sessions", session);

		return center;
	}

	/*
	Sample Data

	{'centers': [{'center_id': 605978,
	'name': 'Palra PHC',
	'address': 'Palra PHC',
	'state_name': 'Haryana',
	'district_name': 'Gurgaon',
	'block_name': 'Ghangola',
	'pincode': 122101,
	'lat': 28,
	'long': 77,
	'from': '11:00:00',
	'to': '17:00:00',
	'fee_type': 'Free',
	'sessions': [{'session_id': '6bf0accb-563b-49ac-8753-b7df477a48e3',
	 'date': '15-05-2021',
	 'available_capacity': 0,
	 'min_age_limit': 18,
	 'vaccine': 'COVISHIELD',
	 'slots': ['11:00AM-12:00PM',
	  '12:00PM-01:00PM',
	  '01:00PM-02:00PM',
	  '02:00PM-05:00PM']}]}]}
*/

	/*
	{'center_id': 561647,
	 'name': 'PHC FALKA',
	 'address': 'Phc Fhalka',
	 'state_name': 'Bihar',
	 'district_name': 'Katihar',
	 'block_name': 'Falka',
	 'pincode': 854114,
	 'lat': 25,
	 'long': 87,
	 'from': '10:00:00',
	 'to': '17:00:00',
	 'fee_type': 'Free',
	 'sessions': [{'session_id': '3465f3bc-6701-42cc-ae4b-eff0840c5515',
	   'date': '16-05-2021',
	   'available_capacity': 4,
	   'min_age_limit': 18,
	   'vaccine': 'COVISHIELD',
	   'slots': ['10:00AM-12:00PM',
		'12:00PM-02:00PM',
		'02:00PM-04:00PM',
		'04:00PM-05:00PM'],
	   'available_capacity_dose1': 0,
	   'available_capacity_dose2': 4}]}
	 */
}
