package io.github.epsi95;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/*
	Sample
	{'beneficiaries': [{'beneficiary_reference_id': 'xxxxx',
	'name': 'Probhakar Sarkar',
	'birth_year': 'xxxx',
	'gender': 'Male',
	'mobile_number': 'xxxx',
	'photo_id_type': 'Aadhaar Card',
	'photo_id_number': 'xxxx',
	'comorbidity_ind': 'x',
	'vaccination_status': 'x',
	'vaccine': '',
	'dose1_date': '',
	'dose2_date': '',
	'appointments': []}]}
*/
public class Beneficiary {
	private String beneficiaryReferenceId;
	private String name;
	private String birthYear;
	private String gender;
	private String mobileNumber;
	private String photoIdType;
	private String photoIdNumber;
	private String commorbidityInd;
	private String vaccinationStatus;
	private String vaccine;
	private String does1Status;
	private String dose2Status;
	private JSONArray appointments;

	Beneficiary(JSONObject beneficiary) throws JSONException {
		this.beneficiaryReferenceId = beneficiary.getString("beneficiary_reference_id");
		this.name = beneficiary.getString("name");
		this.birthYear = beneficiary.getString("birth_year");
		this.gender = beneficiary.getString("birth_year");
		this.mobileNumber = beneficiary.getString("mobile_number");
		this.photoIdType = beneficiary.getString("photo_id_type");
		this.photoIdNumber = beneficiary.getString("photo_id_number");
		this.commorbidityInd = beneficiary.getString("comorbidity_ind");
		this.vaccinationStatus = beneficiary.getString("vaccination_status");
		this.vaccine = beneficiary.getString("vaccine");
		this.does1Status = beneficiary.getString("dose1_date");
		this.dose2Status = beneficiary.getString("dose2_date");
		this.appointments = beneficiary.getJSONArray("appointments");
	}

	public String getBeneficiaryReferenceId() {
		return beneficiaryReferenceId;
	}

	public String getName() {
		return name;
	}

	public String getBirthYear() {
		return birthYear;
	}

	public String getGender() {
		return gender;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public String getPhotoIdType() {
		return photoIdType;
	}

	public String getPhotoIdNumber() {
		return photoIdNumber;
	}

	public String getCommorbidityInd() {
		return commorbidityInd;
	}

	public String getVaccinationStatus() {
		return vaccinationStatus;
	}

	public String getVaccine() {
		return vaccine;
	}

	public String getDoes1Status() {
		return does1Status;
	}

	public String getDose2Status() {
		return dose2Status;
	}

	public JSONArray getAppointments() {
		return appointments;
	}

	@Override
	public String toString() {
		return "Beneficiary{" +
				"beneficiaryReferenceId='" + beneficiaryReferenceId + '\'' +
				", name='" + name + '\'' +
				", birthYear='" + birthYear + '\'' +
				", gender='" + gender + '\'' +
				", mobileNumber='" + mobileNumber + '\'' +
				", photoIdType='" + photoIdType + '\'' +
				", photoIdNumber='" + photoIdNumber + '\'' +
				", commorbidityInd='" + commorbidityInd + '\'' +
				", vaccinationStatus='" + vaccinationStatus + '\'' +
				", vaccine='" + vaccine + '\'' +
				", does1Status='" + does1Status + '\'' +
				", dose2Status='" + dose2Status + '\'' +
				", appointments=" + appointments +
				'}';
	}
}
