package contract;

import contract.request.*;
import mock.MockContext;
import model.Allocated;
import model.License;
import model.Order;
import model.Status;
import ngac.UnauthorizedException;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static contract.AssetContract.ADMINMSP_IPDC;
import static contract.AssetContract.accountIPDC;
import static contract.OrderContract.orderKey;
import static contract.request.LicensesRequest.allocateRequestKey;
import static mock.MockContextUtil.newTestContext;
import static mock.MockIdentity.*;
import static model.LicenseKey.allocatedLicenseKey;
import static model.LicenseKey.licenseKey;
import static org.junit.jupiter.api.Assertions.*;

class OrderContractTest {

    AssetContract assetContract = new AssetContract();
    OrderContract orderContract = new OrderContract();

    @Nested
    class GetQuoteTest {
        @Test
        void test_assetDoesNotExist_throwsException() {
            MockContext ctx = newTestContext(ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.setClientIdentity(ORG2_TPOC);
            ctx.setTransientData(new QuoteRequest(
                    "Org2MSP", "123", 2, 2
            ));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.GetQuote(ctx));
            assertEquals("asset 123 does not exist", e.getMessage());
        }
    }

    @Nested
    class SendQuoteTest {
        @Test
        void test_orderDoesNotExist_throwsException() {
            MockContext ctx = newTestContext(ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.setClientIdentity(ORG1_ACQ);
            ctx.setTransientData(new QuoteRequest(
                    "123", "Org2MSP", 100.0
            ));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.SendQuote(ctx));
            assertEquals("order with ID 123 and account Org2MSP does not exist", e.getMessage());
        }

        @Test
        void test_orderStatusIsNotQuoteRequested_throwsException() {
            MockContext ctx = newTestContext(ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, orderKey("Org2MSP", "123"), new Order(
                    "123", "Org2MSP", Order.Status.INITIATED, "", "", "", "", "",
                    0, 0, 0, "", List.of()
            ).toByteArray());

            ctx.setClientIdentity(ORG1_ACQ);
            ctx.setTransientData(new QuoteRequest("123", "Org2MSP", 2));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.SendQuote(ctx));
            assertEquals("a quote request for order 123 does not exist", e.getMessage());
        }
    }

    @Nested
    class InitiateOrderTest {
        @Test
        void test_orderDoesNotExist_throwsException() {
            MockContext ctx = newTestContext(ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.setClientIdentity(ORG2_TPOC);
            ctx.setTransientData(new OrderIdAndAccountRequest("123", "Org2MSP"));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.InitiateOrder(ctx));
            assertEquals("order with ID 123 and account Org2MSP does not exist", e.getMessage());
        }
        @Test
        void test_quoteNotReceived_throwsException() {
            MockContext ctx = newTestContext(ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, orderKey("Org2MSP", "123"), new Order(
                    "123", "Org2MSP", Order.Status.QUOTE_REQUESTED, "", "", "", "", "",
                    0, 0, 0, "", List.of()
            ).toByteArray());

            ctx.setClientIdentity(ORG2_TPOC);
            ctx.setTransientData(new OrderIdAndAccountRequest(
                    "123", "Org2MSP"
            ));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.InitiateOrder(ctx));
            assertEquals("order 123 has not received a quote", e.getMessage());
        }
    }

    @Nested
    class ApproveOrderTest {
        @Test
        void test_orderDoesNotExist_throwsException() {
            MockContext ctx = newTestContext(ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            // approve
            ctx.setClientIdentity(ORG2_ACQ);
            ctx.setTransientData(new OrderIdAndAccountRequest(
                    "1234", "Org2MSP"
            ));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.ApproveOrder(ctx));
            assertEquals("order with ID 1234 and account Org2MSP does not exist", e.getMessage());
        }
        @Test
        void test_orderNotUpForApproval_throwsException() {
            MockContext ctx = newTestContext(ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, orderKey("Org2MSP", "123"), new Order(
                    "123", "Org2MSP", Order.Status.APPROVED, "", "", "", "", "",
                    0, 0, 0, "", List.of()
            ).toByteArray());

            // approve
            ctx.setClientIdentity(ORG2_ACQ);
            ctx.setTransientData(new OrderIdAndAccountRequest(
                    "123", "Org2MSP"
            ));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.ApproveOrder(ctx));
            assertEquals("order 123 is not up for approval", e.getMessage());
        }
    }

    @Nested
    class DenyOrderTest {
        @Test
        void test_orderDoesNotExist_throwsException() {
            MockContext ctx = newTestContext(ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            // approve
            ctx.setClientIdentity(ORG2_ACQ);
            ctx.setTransientData(new OrderIdAndAccountRequest(
                    "1234", "Org2MSP"
            ));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.DenyOrder(ctx));
            assertEquals("order with ID 1234 and account Org2MSP does not exist", e.getMessage());
        }
        @Test
        void test_orderNotUpForApproval_throwsException() {
            MockContext ctx = newTestContext(ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, orderKey("Org2MSP", "123"), new Order(
                    "123", "Org2MSP", Order.Status.APPROVED, "", "", "", "", "",
                    0, 0, 0, "", List.of()
            ).toByteArray());

            // deny
            ctx.setClientIdentity(ORG2_ACQ);
            ctx.setTransientData(new OrderIdAndAccountRequest(
                    "123", "Org2MSP"
            ));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.DenyOrder(ctx));
            assertEquals("order 123 is not up for approval", e.getMessage());
        }
    }

    @Nested
    class GetLicensesToAllocateForOrderTest {
        @Test
        void test_renewal_throwsException() {
            MockContext ctx = newTestContext(ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, orderKey("Org2MSP", "123"), new Order(
                    "123", "Org2MSP", Order.Status.RENEWAL_APPROVED, "", "", "", "", "",
                    0, 0, 0, "", List.of()
            ).toByteArray());

            ctx.setClientIdentity(ORG1_ACQ);
            ctx.setTransientData(new OrderIdAndAccountRequest("123", "Org2MSP"));
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> orderContract.GetLicensesToAllocateForOrder(ctx)
            );
            assertEquals("cannot get licenses to allocate for an order that is being renewed", e.getMessage());
        }
        @Test
        void test_notApproved_throwsException() {
            MockContext ctx = newTestContext(ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, orderKey("Org2MSP", "123"), new Order(
                    "123", "Org2MSP", Order.Status.INITIATED, "", "", "", "", "",
                    0, 0, 0, "", List.of()
            ).toByteArray());

            ctx.setClientIdentity(ORG1_ACQ);
            ctx.setTransientData(new OrderIdAndAccountRequest("123", "Org2MSP"));
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> orderContract.GetLicensesToAllocateForOrder(ctx)
            );
            assertEquals("cannot get licenses to allocate for an order that has not been approved", e.getMessage());
        }

        @Test
        void test_notEnoughLicenses_throwsException() {
            MockContext ctx = newTestContext(ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.setTxId("123");
            ctx.setTimestamp(Instant.now());
            ctx.setTransientData(new AddAssetRequest(
                    "asset1", "2024-01-01 00:00:00", Set.of(new LicenseIdWithSaltRequest("1", "1"))
            ));
            assetContract.AddAsset(ctx);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, orderKey("Org2MSP", "123"), new Order(
                    "123", "Org2MSP", Order.Status.APPROVED, "", "", "", "", "",
                    2, 0, 0, "", List.of()
            ).toByteArray());

            ctx.setClientIdentity(ORG1_ACQ);
            ctx.setTransientData(new OrderIdAndAccountRequest("123", "Org2MSP"));
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> orderContract.GetLicensesToAllocateForOrder(ctx)
            );
            assertEquals("not enough available licenses to complete order 123", e.getMessage());
        }
    }

    @Nested
    class AllocateLicensesTest {
        @Test
        void test_notApproved_throwsException() {
            MockContext ctx = newTestContext(ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.setTxId("123");
            ctx.setTimestamp(Instant.now());
            ctx.setTransientData(new AddAssetRequest(
                    "asset1", "2024-01-01 00:00:00", Set.of(new LicenseIdWithSaltRequest("1", "1"))
            ));
            assetContract.AddAsset(ctx);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, orderKey("Org2MSP", "123"), new Order(
                    "123", "Org2MSP", Order.Status.INITIATED, "", "", "", "", "123",
                    1, 0, 0, "2025-01-01 00:00:00", List.of()
            ).toByteArray());

            ctx.setClientIdentity(ORG1_ACQ);
            ctx.setTransientData(new AllocateLicensesRequest("123", "Org2MSP", List.of("1")));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.AllocateLicenses(ctx));
            assertEquals("cannot allocate licenses for an order that has not been approved", e.getMessage());
        }
        @Test
        void test_duplicateLicenses_throwsException() {
            MockContext ctx = newTestContext(ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.setTxId("123");
            ctx.setTimestamp(Instant.now());
            ctx.setTransientData(new AddAssetRequest(
                    "asset1", "2024-01-01 00:00:00", Set.of(new LicenseIdWithSaltRequest("1", "1"))
            ));
            assetContract.AddAsset(ctx);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, orderKey("Org2MSP", "123"), new Order(
                    "123", "Org2MSP", Order.Status.APPROVED, "", "", "", "", "123",
                    1, 0, 0, "2025-01-01 00:00:00", List.of()
            ).toByteArray());

            ctx.setClientIdentity(ORG1_ACQ);
            ctx.setTransientData(new AllocateLicensesRequest("123", "Org2MSP", List.of("1", "1")));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.AllocateLicenses(ctx));
            assertEquals("duplicate licenses are not allowed", e.getMessage());
        }
        @Test
        void test_alreadyAllocatedLicense_throwsException() {
            MockContext ctx = newTestContext(ORG1_SO);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.setTxId("123");
            ctx.setTimestamp(Instant.now());
            ctx.setTransientData(new AddAssetRequest(
                    "asset1", "2024-01-01 00:00:00", Set.of(new LicenseIdWithSaltRequest("1", "1"))
            ));
            assetContract.AddAsset(ctx);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, orderKey("Org2MSP", "123"), new Order(
                    "123", "Org2MSP", Order.Status.APPROVED, "", "", "", "", "123",
                    1, 0, 0, "2025-01-01 00:00:00", List.of()
            ).toByteArray());

            // manually add license to state
            ctx.getStub().putPrivateData(ADMINMSP_IPDC, licenseKey("123", "1"),
                                         new License("1", "1", new Allocated(
                                                 "", "", "", ""
                                         )).toByteArray());

            ctx.setClientIdentity(ORG1_ACQ);
            ctx.setTransientData(new AllocateLicensesRequest("123", "Org2MSP", List.of("1")));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.AllocateLicenses(ctx));
            assertEquals("license 1 is already allocated", e.getMessage());
        }
    }

    @Nested
    class GetAllocateRequestForOrderTest {
        @Test
        void test_noRequestExists_throwsException() {
            MockContext ctx = newTestContext(ORG1_ACQ);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);
            ctx.setTransientData(new OrderIdRequest("123"));
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> orderContract.GetAllocateRequestForOrder(ctx)
            );
            assertEquals("no allocate request exists for order 123", e.getMessage());
        }
    }

    @Nested
    class SendLicensesTest {
        @Test
        void test_providedLicensesDoNotMatchRequest_throwsException() {
            MockContext ctx = newTestContext(ORG1_ACQ);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, allocateRequestKey(LicensesRequest.ACTION.ALLOCATE, "123"),
                                         new byte[]{0});

            LicensesRequest licensesRequest = new LicensesRequest("Org2MSP", "123", "123", "exp", List.of("1", "2"));
            ctx.setTransientData(licensesRequest);
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.SendLicenses(ctx));
            assertEquals("provided licenses to send do not match the licenses allocated", e.getMessage());
        }
    }

    @Nested
    class InitiateReturnTest {
        @Test
        void test_alreadyActiveRequest_throwsException() {
            MockContext ctx = newTestContext(ORG1_ACQ);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, allocateRequestKey(LicensesRequest.ACTION.DEALLOCATE, "123"),
                                         new byte[]{});

            ctx.setClientIdentity(ORG2_TPOC);
            LicensesRequest licensesRequest = new LicensesRequest("Org2MSP", "123", "123", "exp", List.of("1", "2"));
            ctx.setTransientData(licensesRequest);
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.InitiateReturn(ctx));
            assertEquals("a request to return licenses for order 123 is already active", e.getMessage());
        }
        @Test
        void test_licenseNotCheckedOutByAccount_throwsException() {
            MockContext ctx = newTestContext(ORG1_ACQ);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, allocateRequestKey(LicensesRequest.ACTION.DEALLOCATE, "123"),
                                         new byte[]{0});
            ctx.getStub().putPrivateData(accountIPDC("Org2MSP"), allocatedLicenseKey("123", "1"),
                                         new byte[]{});

            ctx.setClientIdentity(ORG2_TPOC);
            LicensesRequest licensesRequest = new LicensesRequest("Org2MSP", "123", "123", "exp", List.of("1", "2"));
            ctx.setTransientData(licensesRequest);
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.InitiateReturn(ctx));
            assertEquals("license 1 is not leased by Org2MSP", e.getMessage());
        }
    }

    @Nested
    class GetInitiatedReturnForOrderTest {
        @Test
        void test_noRequestExists_throwsException() {
            MockContext ctx = newTestContext(ORG1_ACQ);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, allocateRequestKey(LicensesRequest.ACTION.DEALLOCATE, "123"),
                                         new byte[]{});

            ctx.setClientIdentity(ORG1_ACQ);
            ctx.setTransientData(new OrderIdRequest("123"));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.GetInitiatedReturnForOrder(ctx));
            assertEquals("no deallocate request exists for order 123", e.getMessage());
        }
    }

    @Nested
    class DeallocateLicensesFromAccountTest {
        @Test
        void test_requestDoesNotMatch_throwsException() {
            MockContext ctx = newTestContext(ORG1_ACQ);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, allocateRequestKey(LicensesRequest.ACTION.DEALLOCATE, "123"),
                                         new byte[]{0});

            ctx.setClientIdentity(ORG1_ACQ);
            LicensesRequest licensesRequest = new LicensesRequest("Org2MSP", "123", "123", "exp", List.of("1", "2"));
            ctx.setTransientData(licensesRequest);
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.DeallocateLicensesFromAccount(ctx));
            assertEquals("provided deallocation request does not match the one initiated", e.getMessage());
        }
    }

    @Nested
    class DeallocateLicensesFromSPTest {
        @Test
        void test_requestDoesNotExist_throwsException() {
            MockContext ctx = newTestContext(ORG1_ACQ);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.getStub().putPrivateData(ADMINMSP_IPDC, orderKey("Org2MSP", "123"), new Order(
                    "123", "Org2MSP", Order.Status.ALLOCATED, "", "", "", "", "",
                    0, 0, 0, "", List.of()
            ).toByteArray());
            ctx.getStub().putPrivateData(ADMINMSP_IPDC, allocateRequestKey(LicensesRequest.ACTION.DEALLOCATE, "123"),
                                         new byte[]{});

            ctx.setClientIdentity(ORG1_ACQ);
            ctx.setTransientData(new OrderIdAndAccountRequest("123", "Org2MSP"));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> orderContract.DeallocateLicensesFromSP(ctx));
            assertEquals("no deallocate request exists for order 123", e.getMessage());
        }
    }

    @Nested
    class GetExpiredOrdersTest {
        @Test
        void test_ok() {
            MockContext ctx = newTestContext(ORG1_ACQ);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);
            ctx.setTimestamp(Instant.now());
            Order order = new Order(
                    "1", "Org2MSP", Order.Status.ALLOCATED, "", "", "", "", "",
                    0, 0, 0, "2026-01-01 00:00:00", List.of()
            );
            ctx.getStub().putPrivateData(ADMINMSP_IPDC, orderKey("Org2MSP", "1234"), order.toByteArray());

            Order order2 = new Order(
                    "2", "Org2MSP", Order.Status.ALLOCATED, "", "", "", "", "",
                    0, 0, 0, "2024-01-01 00:00:00", List.of()
            );

            Order order3 = new Order(
                    "3", "Org3MSP", Order.Status.ALLOCATED, "", "", "", "", "",
                    0, 0, 0, "2024-01-01 00:00:00", List.of()
            );
            ctx.getStub().putPrivateData(ADMINMSP_IPDC, orderKey("Org2MSP", "1"), order.toByteArray());
            ctx.getStub().putPrivateData(ADMINMSP_IPDC, orderKey("Org2MSP", "2"), order2.toByteArray());
            ctx.getStub().putPrivateData(ADMINMSP_IPDC, orderKey("Org3MSP", "3"), order3.toByteArray());

            Order[] orders = orderContract.GetExpiredOrders(ctx);
            assertEquals(2, orders.length);
            List<Order> ordersList = Arrays.asList(orders);
            assertTrue(ordersList.contains(order2));
            assertTrue(ordersList.contains(order3));

            ctx.setClientIdentity(ORG3_ACQ);
            orders = orderContract.GetExpiredOrders(ctx);
            assertEquals(1, orders.length);
            assertEquals(order3, orders[0]);
        }
    }
}