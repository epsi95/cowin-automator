package io.github.epsi95;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

class CenterSession {
	private String sessionId;
	private String date;
	private int availableCapacity;
	private Age minAgeLimit;
	private VaccineType vaccine;
	private ArrayList<String> slots;
	private Boolean isAnySlotAvailable;
	private int numberOfSlotsForDose1;
	private int numberOfSlotsForDose2;

	public CenterSession(String sessionId, String date, int availableCapacity, Age minAgeLimit, VaccineType vaccine, ArrayList<String> slots, Boolean isAnySlotAvailable, int numberOfSlotsForDose1, int numberOfSlotsForDose2) {
		this.sessionId = sessionId;
		this.date = date;
		this.availableCapacity = availableCapacity;
		this.minAgeLimit = minAgeLimit;
		this.vaccine = vaccine;
		this.slots = slots;
		this.isAnySlotAvailable = isAnySlotAvailable;
		this.numberOfSlotsForDose1 = numberOfSlotsForDose1;
		this.numberOfSlotsForDose2 = numberOfSlotsForDose2;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getDate() {
		return date;
	}

	public int getAvailableCapacity() {
		return availableCapacity;
	}

	public Age getMinAgeLimit() {
		return minAgeLimit;
	}

	public VaccineType getVaccine() {
		return vaccine;
	}

	public ArrayList<String> getSlots() {
		return slots;
	}

	public Boolean getAnySlotAvailable() {
		return isAnySlotAvailable;
	}

	public int getNumberOfSlotsForDose1() {
		return numberOfSlotsForDose1;
	}

	public int getNumberOfSlotsForDose2() {
		return numberOfSlotsForDose2;
	}

	@Override
	public String toString() {
		return "CenterSession{" +
				"sessionId='" + sessionId + '\'' +
				", date='" + date + '\'' +
				", availableCapacity=" + availableCapacity +
				", minAgeLimit=" + minAgeLimit +
				", vaccine=" + vaccine +
				", slots=" + slots +
				", isAnySlotAvailable=" + isAnySlotAvailable +
				", numberOfSlotsForDose1=" + numberOfSlotsForDose1 +
				", numberOfSlotsForDose2=" + numberOfSlotsForDose2 +
				'}';
	}

	public JSONObject toJSONObject() throws JSONException {
		JSONObject session = new JSONObject();
		session.put("session_id", sessionId);
		session.put("date", date);
		session.put("available_capacity", availableCapacity);
		session.put("min_age_limit", minAgeLimit == Age.AGE_18_PLUS ? 18 : 45);
		session.put("vaccine", EnumToStringMapper.mapVaccineTypeToString(vaccine));

		JSONArray slots = new JSONArray();
		for(String slot: this.getSlots()){
			slots.put(slot);
		}
		session.put("slots", slots);
		session.put("available_capacity_dose1", this.getNumberOfSlotsForDose1());
		session.put("available_capacity_dose2", this.getNumberOfSlotsForDose2());
		return session;
	}



	/*
	{'session_id': '6bf0accb-563b-49ac-8753-b7df477a48e3',
	 'date': '15-05-2021',
	 'available_capacity': 0,
	 'min_age_limit': 18,
	 'vaccine': 'COVISHIELD',
	 'slots': ['11:00AM-12:00PM',
	  '12:00PM-01:00PM',
	  '01:00PM-02:00PM',
	  '02:00PM-05:00PM']}
	 */
}
