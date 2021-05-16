package io.github.epsi95;

public class SlotBookingResult {
	private boolean isBookingSuccess;
	private String response;

	public SlotBookingResult(boolean isBookingSuccess, String response) {
		this.isBookingSuccess = isBookingSuccess;
		this.response = response;
	}

	public boolean isBookingSuccess() {
		return isBookingSuccess;
	}

	public String getResponse() {
		return response;
	}
}
