package io.github.epsi95;

public class VersionCheckStatus {
	private boolean shouldRedirect;
	private String redirectUrl;

	public VersionCheckStatus(boolean shouldRedirect, String redirectUrl) {
		this.shouldRedirect = shouldRedirect;
		this.redirectUrl = redirectUrl;
	}

	public boolean isShouldRedirect() {
		return shouldRedirect;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}
}
