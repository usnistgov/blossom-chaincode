package contract;

import contract.request.ReportSWIDRequest;
import contract.request.SWIDRequest;
import mock.MockContext;
import model.Status;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static mock.MockContextUtil.newTestContext;
import static mock.MockIdentity.ORG1_ACQ;
import static mock.MockIdentity.ORG2_TPOC;
import static org.junit.jupiter.api.Assertions.*;

class SWIDContractTest {

    SWIDContract swidContract = new SWIDContract();

    @Nested
    class ReportSWIDTest {
        @Test
        void test_licenseNotFound_throwsException() {
            MockContext ctx = newTestContext(ORG2_TPOC);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.setTransientData(new ReportSWIDRequest(
                    "Org2MSP", "2", "3", "4", "5"
            ));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> swidContract.ReportSWID(ctx));
            assertEquals("license not found", e.getMessage());
        }
    }

    @Nested
    class DeleteSWIDTest {
        @Test
        void test_licenseNotFound_throwsException() {
            MockContext ctx = newTestContext(ORG2_TPOC);
            ctx.getStub().setAccountStatus(Status.AUTHORIZED);

            ctx.setTransientData(new SWIDRequest(
                    "Org2MSP", "2", "3"
            ));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> swidContract.DeleteSWID(ctx));
            assertEquals("SWID for license 3 in order 2 does not exist", e.getMessage());
        }
    }

}