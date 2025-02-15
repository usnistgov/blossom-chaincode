package contract.request;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ReportSWIDRequest {

    private final String account;
    private final String primaryTag;
    private final String xml;
    private final String orderId;
    private final String licenseId;

    public ReportSWIDRequest(String account, String primaryTag, String xml, String orderId, String licenseId) {
        this.account = account;
        this.primaryTag = primaryTag;
        this.xml = xml;
        this.orderId = orderId;
        this.licenseId = licenseId;
    }

    public ReportSWIDRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                ReportSWIDRequest.class));
    }

    private ReportSWIDRequest(ReportSWIDRequest req) {
        this.account = Objects.requireNonNull(req.getAccount(), "account cannot be null");
        this.primaryTag = Objects.requireNonNull(req.getPrimaryTag(), "primaryTag cannot be null");
        this.xml = Objects.requireNonNull(req.getXml(), "xml cannot be null");
        this.orderId = Objects.requireNonNull(req.getOrderId(), "orderId cannot be null");
        this.licenseId = Objects.requireNonNull(req.getLicenseId(), "licenseId cannot be null");
    }

    public String getAccount() {
        return account;
    }

    public String getPrimaryTag() {
        return primaryTag;
    }

    public String getXml() {
        return xml;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getLicenseId() {
        return licenseId;
    }
}
