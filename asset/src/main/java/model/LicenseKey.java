package model;

public class LicenseKey {

    public static final String LICENSE_PREFIX = "license:";
    public static String ALLOCATED_PREFIX = "allocated:";

    public static String allocatedLicenseKey(String orderId, String licenseId) {
        return ALLOCATED_PREFIX + orderId + licenseId;
    }

    public static String licenseKey(String assetId, String licenseId) {
        return LICENSE_PREFIX + assetId + licenseId;
    }

    public static String hashedLicenseKey(String assetId, String licenseId, String salt) {
        return SHA256.hashStrToStr(salt + assetId + licenseId);
    }

    private LicenseKey() {}
}
