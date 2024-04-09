package ngac;

import contract.response.AssetDetailResponse;
import mock.MockContext;
import mock.MockIdentity;
import model.LicenseWithExpiration;
import model.Status;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static mock.MockContextUtil.newTestContext;
import static org.junit.jupiter.api.Assertions.*;

class PDPTest {

    @Test
    void canWriteAsset() {
        MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        assertDoesNotThrow(() -> PDP.canWriteAsset(ctx));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        assertThrows(UnauthorizedException.class, () -> PDP.canWriteAsset(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        assertThrows(UnauthorizedException.class, () -> PDP.canWriteAsset(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        assertThrows(UnauthorizedException.class, () -> PDP.canWriteAsset(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canWriteAsset(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canWriteAsset(ctx));

        ctx.setClientIdentity(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canWriteAsset(ctx));
    }

    @Test
    void canReadAssetDetail() {
        MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        assertDoesNotThrow(() -> PDP.canWriteAsset(ctx));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        assertDoesNotThrow(() -> PDP.canReadAssetDetail(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadAssetDetail(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadAssetDetail(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadAssetDetail(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadAssetDetail(ctx));

        ctx.setClientIdentity(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadAssetDetail(ctx));
    }

    @Test
    void filterAssetDetail() {
        AssetDetailResponse full = new AssetDetailResponse(
                "123",
                "asset1",
                5,
                "01-01-2000 01:01:01",
                "02-02-2000 01:01:01",
                10,
                Set.of("1", "2"),
                Map.of("1", Map.of("1", Set.of(new LicenseWithExpiration("1", "1"))))
        );
        AssetDetailResponse basic = new AssetDetailResponse(
                "123",
                "asset1",
                5,
                "01-01-2000 01:01:01",
                "02-02-2000 01:01:01",
                null,
                null,
                null
        );

        MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        assertEquals(full, PDP.filterAssetDetail(ctx, full));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        assertEquals(full, PDP.filterAssetDetail(ctx, full));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        assertEquals(basic, PDP.filterAssetDetail(ctx, full));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        assertEquals(basic, PDP.filterAssetDetail(ctx, full));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.filterAssetDetail(ctx, full));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.filterAssetDetail(ctx, full));

        ctx.setClientIdentity(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.filterAssetDetail(ctx, full));
    }

    @Test
    void canReadAssets() {
        MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        assertDoesNotThrow(() -> PDP.canReadAssets(ctx));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        assertDoesNotThrow(() -> PDP.canReadAssets(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        assertDoesNotThrow(() -> PDP.canReadAssets(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        assertDoesNotThrow(() -> PDP.canReadAssets(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadAssets(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadAssets(ctx));

        ctx.setClientIdentity(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadAssets(ctx));
    }

    @Test
    void canAllocateLicense() {
        MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canAllocateLicense(ctx));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        assertDoesNotThrow(() -> PDP.canAllocateLicense(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        assertThrows(UnauthorizedException.class, () -> PDP.canAllocateLicense(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        assertThrows(UnauthorizedException.class, () -> PDP.canAllocateLicense(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canAllocateLicense(ctx));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canAllocateLicense(ctx));

        ctx.setClientIdentity(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canAllocateLicense(ctx));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canAllocateLicense(ctx));
    }

    @Test
    void canReadOrder() {
        MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        assertDoesNotThrow(() -> PDP.canReadOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        assertDoesNotThrow(() -> PDP.canReadOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        assertDoesNotThrow(() -> PDP.canReadOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        assertDoesNotThrow(() -> PDP.canReadOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG3_TPOC);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadOrder(ctx, "Org2MSP"));
    }

    @Test
    void canInitiateOrder() {
        MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canInitiateOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        assertThrows(UnauthorizedException.class, () -> PDP.canInitiateOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        assertThrows(UnauthorizedException.class, () -> PDP.canInitiateOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        assertDoesNotThrow(() -> PDP.canInitiateOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG3_TPOC);
        assertThrows(UnauthorizedException.class, () -> PDP.canInitiateOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canInitiateOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canInitiateOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canInitiateOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canInitiateOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG3_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canInitiateOrder(ctx, "Org2MSP"));

    }

    @Test
    void canApproveOrder() {
        MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canApproveOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        assertThrows(UnauthorizedException.class, () -> PDP.canApproveOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        assertDoesNotThrow(() -> PDP.canApproveOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        assertThrows(UnauthorizedException.class, () -> PDP.canApproveOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG3_TPOC);
        assertThrows(UnauthorizedException.class, () -> PDP.canApproveOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canApproveOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canApproveOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canApproveOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canApproveOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG3_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canApproveOrder(ctx, "Org2MSP"));
    }

    @Test
    void canDenyOrder() {
        MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canDenyOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        assertThrows(UnauthorizedException.class, () -> PDP.canDenyOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        assertDoesNotThrow(() -> PDP.canDenyOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        assertThrows(UnauthorizedException.class, () -> PDP.canDenyOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG3_TPOC);
        assertThrows(UnauthorizedException.class, () -> PDP.canDenyOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canDenyOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canDenyOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canDenyOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canDenyOrder(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG3_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canDenyOrder(ctx, "Org2MSP"));
    }

    @Test
    void canReturnLicense() {
        MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReturnLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        assertThrows(UnauthorizedException.class, () -> PDP.canReturnLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        assertThrows(UnauthorizedException.class, () -> PDP.canReturnLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        assertDoesNotThrow(() -> PDP.canReturnLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG3_TPOC);
        assertThrows(UnauthorizedException.class, () -> PDP.canReturnLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReturnLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReturnLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReturnLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReturnLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG3_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReturnLicense(ctx, "Org2MSP"));
    }

    @Test
    void canReadLicense() {
        MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        assertDoesNotThrow(() -> PDP.canReadLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        assertDoesNotThrow(() -> PDP.canReadLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        assertDoesNotThrow(() -> PDP.canReadLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG3_TPOC);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadLicense(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG3_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadLicense(ctx, "Org2MSP"));
    }

    @Test
    void canReadSWID() {
        MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        assertDoesNotThrow(() -> PDP.canReadSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        assertDoesNotThrow(() -> PDP.canReadSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        assertDoesNotThrow(() -> PDP.canReadSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        assertDoesNotThrow(() -> PDP.canReadSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG3_TPOC);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG3_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canReadSWID(ctx, "Org2MSP"));
    }

    @Test
    void canWriteSWID() {
        MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        assertThrows(ChaincodeException.class, () -> PDP.canWriteSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        assertThrows(ChaincodeException.class, () -> PDP.canWriteSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        assertThrows(ChaincodeException.class, () -> PDP.canWriteSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        assertDoesNotThrow(() -> PDP.canWriteSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG3_TPOC);
        assertThrows(UnauthorizedException.class, () -> PDP.canWriteSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canWriteSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canWriteSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canWriteSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canWriteSWID(ctx, "Org2MSP"));

        ctx.setClientIdentity(MockIdentity.ORG3_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        assertThrows(UnauthorizedException.class, () -> PDP.canWriteSWID(ctx, "Org2MSP"));
    }
}