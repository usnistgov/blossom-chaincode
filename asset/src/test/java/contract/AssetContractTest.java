package contract;

import contract.request.*;
import contract.response.AllocateLicensesResponse;
import contract.response.AssetDetailResponse;
import contract.response.IdResponse;
import mock.MockContext;
import mock.MockIdentity;
import model.Status;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static mock.MockContextUtil.newTestContext;
import static mock.MockContextUtil.newTestContextWithAuthorized;
import static org.junit.jupiter.api.Assertions.*;

class AssetContractTest {

    AssetContract assetContract = new AssetContract();
    OrderContract orderContract = new OrderContract();

    @Nested
    class AddLicenses {
        @Test
        void test_licenseAlreadyExists_throwsException() {
            MockContext ctx = newTestContextWithAuthorized(MockIdentity.ORG1_SO);
            ctx.setTxId("123");
            ctx.setTimestamp(Instant.now());
            ctx.setTransientData(new AddAssetRequest(
                    "asset1", "2024-01-01 00:00:00", Set.of(new LicenseIdWithSaltRequest("1", "1"))
            ));
            assetContract.AddAsset(ctx);

            ctx.setTransientData(new AddLicensesRequest(
                    "123", Set.of(new LicenseIdWithSaltRequest("1", "1"))
            ));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> assetContract.AddLicenses(ctx));
            assertEquals("license 1 already exists", e.getMessage());
        }
    }

    @Nested
    class RemoveLicenses {
        @Test
        void test_licenseDoesNotExist_throwsException() {
            MockContext ctx = newTestContextWithAuthorized(MockIdentity.ORG1_SO);
            ctx.setTxId("123");
            ctx.setTimestamp(Instant.now());
            ctx.setTransientData(new AddAssetRequest(
                    "asset1", "2024-01-01 00:00:00", Set.of(new LicenseIdWithSaltRequest("1", "1"))
            ));
            assetContract.AddAsset(ctx);

            ctx.setTransientData(new RemoveLicensesRequest(
                    "123", Set.of("2")
            ));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> assetContract.RemoveLicenses(ctx));
            assertEquals("license 2 does not exist", e.getMessage());
        }
        @Test
        void test_licenseAllocated_throwsException() {
            MockContext ctx = newTestContextWithAuthorized(MockIdentity.ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);
            ctx.setTxId("123");
            ctx.setTimestamp(Instant.now());
            ctx.setTransientData(new AddAssetRequest(
                    "asset1", "2024-01-01 00:00:00", Set.of(new LicenseIdWithSaltRequest("1", "1"))
            ));
            assetContract.AddAsset(ctx);

           // get
            ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
            ctx.setTransientData(new QuoteRequest(
                    null,
                    "Org2MSP",
                    "123",
                    1,
                    1
            ));
            orderContract.GetQuote(ctx);

            // send
            ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
            ctx.setTransientData(new QuoteRequest(
                    "123",
                    "Org2MSP",
                    100.0
            ));
            orderContract.SendQuote(ctx);

            // initiate
            ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
            OrderIdAndAccountRequest request = new OrderIdAndAccountRequest(
                    "123",
                    "Org2MSP"
            );
            ctx.setTransientData(request);
            orderContract.InitiateOrder(ctx);

            // approve
            ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
            ctx.setTransientData(request);
            orderContract.ApproveOrder(ctx);

            // allocate
            ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
            ctx.setTransientData(request);
            AllocateLicensesResponse resp = orderContract.GetLicensesToAllocateForOrder(ctx);

            ctx.setTransientData(new AllocateLicensesRequest(
                    resp.getOrderId(),
                    resp.getAccount(),
                    resp.getLicenses()
            ));
            LicensesRequest licensesRequest = orderContract.AllocateLicenses(ctx);

            // send
            ctx.setTransientData(licensesRequest);
            orderContract.SendLicenses(ctx);

            // test
            ctx.setClientIdentity(MockIdentity.ORG1_SO);
            ctx.setTransientData(new RemoveLicensesRequest(
                    "123", Set.of("1")
            ));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> assetContract.RemoveLicenses(ctx));
            assertEquals("license 1 is allocated to Org2MSP", e.getMessage());
        }
    }

    @Nested
    class GetAsset {
        @Test
        void test_GetAssetReturnsBasicInfo() {
            MockContext ctx = newTestContextWithAuthorized(MockIdentity.ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);
            ctx.setTxId("123");
            ctx.setTimestamp(Instant.now());
            ctx.setTransientData(new AddAssetRequest(
                    "asset1", "2024-01-01 00:00:00", Set.of(new LicenseIdWithSaltRequest("1", "1"))
            ));
            assetContract.AddAsset(ctx);

            ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
            ctx.setTransientData(new AssetIdRequest("123"));
            AssetDetailResponse actual = assetContract.GetAsset(ctx);
            assertEquals("123", actual.getId());
            assertEquals("asset1", actual.getName());
            assertEquals(1, actual.getNumAvailable());
            assertEquals("2024-01-01 00:00:00", actual.getEndDate());
            assertNull(actual.getTotalAmount());
            assertNull(actual.getAvailableLicenses());
            assertNull(actual.getAllocatedLicenses());
        }
    }

    @Nested
    class GetLicenseTxHistory {
        @Test
        void test_LicenseDoesNotExist_throwsException() {
            MockContext ctx = newTestContextWithAuthorized(MockIdentity.ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);
            ctx.setTxId("123");
            ctx.setTimestamp(Instant.now());
            ctx.setTransientData(new AddAssetRequest(
                    "asset1", "2024-01-01 00:00:00", Set.of(new LicenseIdWithSaltRequest("1", "1"))
            ));
            assetContract.AddAsset(ctx);

            ctx.setTransientData(new GetLicenseTxHistoryRequest("123", "2"));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> assetContract.GetLicenseTxHistory(ctx));
            assertEquals("license 2 does not exist for asset 123", e.getMessage());
        }
    }
}