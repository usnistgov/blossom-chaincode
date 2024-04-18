package contract.request;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class SWIDRequest {

    private final String account;
    private final String orderId;
    private final String licenseId;

    public SWIDRequest(String account, String orderId, String licenseId) {
        this.account = account;
        this.orderId = orderId;
        this.licenseId = licenseId;
    }

    public SWIDRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                SWIDRequest.class));
    }

    private SWIDRequest(SWIDRequest req) {
        this.licenseId = Objects.requireNonNull(req.getLicenseId(), "licenseId cannot be null");
        this.account = Objects.requireNonNull(req.getAccount(), "account cannot be null");
        this.orderId = Objects.requireNonNull(req.getOrderId(), "orderId cannot be null");
    }

    public String getLicenseId() {
        return licenseId;
    }

    public String getAccount() {
        return account;
    }

    public String getOrderId() {
        return orderId;
    }
}
