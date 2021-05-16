package io.github.epsi95;


import java.util.ArrayList;

public class UserPreference {
	private String userPhoneNumber;
	private Age userAge;
	private VaccineType userSelectedVaccineType;
	private PaidType userSelectedPaidType;
	private SearchByType userSelectedSearchByType;
	private String userPinNumber;
	private int userDistrictCode = -1;
	private int userScanningInterval = 1; // in seconds
	private String userSecret;
	private Beneficiary beneficiary;
	private int doseNumber = 1;
	private int userPreferredBookingDirection=-1; // 1 means morning to evening and -1 means evening to morning
	private AppointmentType appointmentType = AppointmentType.FRESH;
	private String lastAppointmentId;
	private boolean shouldDoseWiseSlotBeConsidered = false;

	public boolean isShouldDoseWiseSlotBeConsidered() {
		return shouldDoseWiseSlotBeConsidered;
	}

	public void setShouldDoseWiseSlotBeConsidered(boolean shouldDoseWiseSlotBeConsidered) {
		this.shouldDoseWiseSlotBeConsidered = shouldDoseWiseSlotBeConsidered;
	}

	public String getLastAppointmentId() {
		return lastAppointmentId;
	}

	public void setLastAppointmentId(String lastAppointmentId) {
		this.lastAppointmentId = lastAppointmentId;
	}

	public AppointmentType getAppointmentType() {
		return appointmentType;
	}

	public void setAppointmentType(AppointmentType appointmentType) {
		this.appointmentType = appointmentType;
	}

	public int getUserPreferredBookingDirection() {
		return userPreferredBookingDirection;
	}

	public void setUserPreferredBookingDirection(int userPreferredBookingDirection) {
		this.userPreferredBookingDirection = userPreferredBookingDirection;
	}

	public int getDoseNumber() {
		return doseNumber;
	}

	public void setDoseNumber(int doseNumber) {
		this.doseNumber = doseNumber;
	}

	public Beneficiary getBeneficiary() {
		return beneficiary;
	}

	public void setBeneficiary(Beneficiary beneficiary) {
		this.beneficiary = beneficiary;
	}

	public String getUserPhoneNumber() {
		return userPhoneNumber;
	}

	public void setUserPhoneNumber(String userPhoneNumber) {
		this.userPhoneNumber = userPhoneNumber;
	}

	public Age getUserAge() {
		return userAge;
	}

	public void setUserAge(Age userAge) {
		this.userAge = userAge;
	}

	public VaccineType getUserSelectedVaccineType() {
		return userSelectedVaccineType;
	}

	public void setUserSelectedVaccineType(VaccineType userSelectedVaccineType) {
		this.userSelectedVaccineType = userSelectedVaccineType;
	}

	public PaidType getUserSelectedPaidType() {
		return userSelectedPaidType;
	}

	public void setUserSelectedPaidType(PaidType userSelectedPaidType) {
		this.userSelectedPaidType = userSelectedPaidType;
	}

	public SearchByType getUserSelectedSearchByType() {
		return userSelectedSearchByType;
	}

	public void setUserSelectedSearchByType(SearchByType userSelectedSearchByType) {
		this.userSelectedSearchByType = userSelectedSearchByType;
	}

	public String getUserPinNumber() {
		return userPinNumber;
	}

	public void setUserPinNumber(String userPinNumber) {
		this.userPinNumber = userPinNumber;
	}

	public int getUserDistrictCode() {
		return userDistrictCode;
	}

	public void setUserDistrictCode(int userDistrictCode) {
		this.userDistrictCode = userDistrictCode;
	}

	public int getUserScanningInterval() {
		return userScanningInterval;
	}

	public void setUserScanningInterval(int userScanningInterval) {
		this.userScanningInterval = userScanningInterval;
	}

	public String getUserSecret() {
		return userSecret;
	}

	public void setUserSecret(String userSecret) {
		this.userSecret = userSecret;
	}

	@Override
	public String toString() {
		return "UserPreference{" +
				"userPhoneNumber='" + userPhoneNumber + '\'' +
				", userAge=" + userAge +
				", userSelectedVaccineType=" + userSelectedVaccineType +
				", userSelectedPaidType=" + userSelectedPaidType +
				", userSelectedSearchByType=" + userSelectedSearchByType +
				", userPinNumber='" + userPinNumber + '\'' +
				", userDistrictCode=" + userDistrictCode +
				", userScanningInterval=" + userScanningInterval +
				", userSecret='" + userSecret + '\'' +
				", beneficiary=" + beneficiary +
				", doseNumber=" + doseNumber +
				", userPreferredBookingDirection=" + userPreferredBookingDirection +
				", appointmentType=" + appointmentType +
				", lastAppointmentId='" + lastAppointmentId + '\'' +
				", shouldDoseWiseSlotBeConsidered=" + shouldDoseWiseSlotBeConsidered +
				'}';
	}


}

enum VaccineType {
	COVISHIELD,
	COVAXIN,
	SPUTNIK_V,
	ALL
}
enum PaidType {
	FREE,
	PAID,
	ALL
}
enum SearchByType {
	BY_PIN,
	BY_DISTRICT,
}
enum Age{
	AGE_18_PLUS,
	AGE_45_PLUS,
	ALL
}

enum AppointmentType{
	FRESH,
	RESCHEDULE
}

