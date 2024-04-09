package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;

@DataType
public class SWID implements Serializable {

    @Property
    private String primaryTag;
    @Property
    private String xml;
    @Property
    private String orderId;
    @Property
    private String licenseId;

    public SWID(@JsonProperty String primaryTag,
                @JsonProperty String xml,
                @JsonProperty String orderId,
                @JsonProperty String licenseId) {
        this.primaryTag = primaryTag;
        this.xml = xml;
        this.orderId = orderId;
        this.licenseId = licenseId;
    }

    public String getPrimaryTag() {
        return primaryTag;
    }

    public void setPrimaryTag(String primaryTag) {
        this.primaryTag = primaryTag;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public byte[] toByteArray() {
        return SerializationUtils.serialize(this);
    }

    public static SWID fromByteArray(byte[] bytes) {
        return SerializationUtils.deserialize(bytes);
    }
}
