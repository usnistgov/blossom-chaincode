package contract.request;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class AddAssetRequest {

    private final String name;
    private final String endDate;
    private final Set<LicenseIdWithSaltRequest> licenses;

    public AddAssetRequest(String name, String endDate, Set<LicenseIdWithSaltRequest> licenses) {
        this.name = name;
        this.endDate = endDate;
        this.licenses = licenses;
    }

    public AddAssetRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                AddAssetRequest.class));
    }

    private AddAssetRequest(AddAssetRequest req) {
        this.name = Objects.requireNonNull(req.name, "name cannot be null");
        this.endDate = Objects.requireNonNull(req.endDate, "endDate cannot be null");
        this.licenses = Objects.requireNonNull(req.licenses, "licenses cannot be null");
    }

    public String getName() {
        return name;
    }

    public String getEndDate() {
        return endDate;
    }

    public Set<LicenseIdWithSaltRequest> getLicenses() {
        return licenses;
    }
}
