package contract.event;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;

@DataType
public class ATOEvent implements Serializable {

    @Property
    private String account;

    public ATOEvent() {
    }

    public ATOEvent(String account) {
        this.account = account;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}