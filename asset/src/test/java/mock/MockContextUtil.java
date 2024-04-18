package mock;

import contract.AssetContract;
import model.Status;

import java.time.Instant;

import static mock.MockOrgs.*;

public class MockContextUtil {

    public static MockContext newTestContext(MockIdentity initialIdentity) {
        MockContext mockContext = new MockContext(MockIdentity.ORG1_LO);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG1_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG2_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG3_MSP);

        mockContext.setClientIdentity(initialIdentity);

        return mockContext;
    }

    public static MockContext newTestContextWithAuthorized(MockIdentity initialIdentity) {
        MockContext mockContext = new MockContext(MockIdentity.ORG1_LO);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG1_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG2_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG3_MSP);

        mockContext.setClientIdentity(initialIdentity);

        mockContext.getStub().setAccountStatus(Status.AUTHORIZED);

        return mockContext;
    }

    public static MockContext newTestContextWithAsset(MockIdentity initialIdentity) {
        MockContext mockContext = new MockContext(MockIdentity.ORG1_LO);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG1_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG2_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG3_MSP);

        mockContext.setClientIdentity(MockIdentity.ORG1_LO);
        mockContext.setTxId("123");
        mockContext.setTimestamp(Instant.now());
        //mockContext.setTransientData(new AddAssetRequest("123", "2000-01-01"));
        new AssetContract().AddAsset(mockContext);

        mockContext.setClientIdentity(initialIdentity);

        return mockContext;
    }
}
