package model;


import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.util.Objects;

@DataType
public class Feedback implements Serializable {

    @Property
    private int atoVersion;

    @Property
    private String accountId;

    @Property
    private String comments;

    public Feedback() {
    }

    public Feedback(int atoVersion, String accountId, String comments) {
        this.atoVersion = atoVersion;
        this.accountId = accountId;
        this.comments = comments;
    }

    public int getAtoVersion() {
        return atoVersion;
    }

    public void setAtoVersion(int atoVersion) {
        this.atoVersion = atoVersion;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Feedback feedback = (Feedback) o;
        return atoVersion == feedback.atoVersion && Objects.equals(accountId, feedback.accountId) && Objects.equals(
                comments, feedback.comments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(atoVersion, accountId, comments);
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "atoVersion=" + atoVersion +
                ", org='" + accountId + '\'' +
                ", comments='" + comments + '\'' +
                '}';
    }
}
