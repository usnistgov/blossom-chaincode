package contract.response;


import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.List;

@DataType
public class AllocateLicensesResponse {

    @Property
    private String orderId;
    @Property
    private String account;
    @Property
    private List<String> licenses;

    public AllocateLicensesResponse(String orderId, String account, List<String> licenses) {
        this.orderId = orderId;
        this.account = account;
        this.licenses = licenses;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public List<String> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<String> licenses) {
        this.licenses = licenses;
    }
}
