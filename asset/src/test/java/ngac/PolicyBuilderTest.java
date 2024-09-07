package ngac;

import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.exception.PMException;
import gov.nist.csd.pm.pap.graph.relationship.AccessRightSet;
import gov.nist.csd.pm.pap.query.UserContext;
import mock.MockContext;
import mock.MockIdentity;
import model.Status;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static mock.MockContextUtil.newTestContext;
import static ngac.PolicyBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

class PolicyBuilderTest {

    @Test
    void testInvalidRoles() {
        MockContext ctx = newTestContext(MockIdentity.ORG1_TPOC);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        ChaincodeException e = assertThrows(ChaincodeException.class, () -> {
            PolicyBuilder.buildPolicyForAssetDecision(ctx);
        });
        assertEquals(e.getMessage(), "invalid role " + TPOC);
        ctx.setClientIdentity(MockIdentity.ORG1_LO);
        assertDoesNotThrow(() -> {
            PolicyBuilder.buildPolicyForAssetDecision(ctx);
        });
        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        assertDoesNotThrow(() -> {
            PolicyBuilder.buildPolicyForAssetDecision(ctx);
        });

        ctx.setClientIdentity(MockIdentity.ORG2_LO);
        e = assertThrows(ChaincodeException.class, () -> {
            PolicyBuilder.buildPolicyForAssetDecision(ctx);
        });
        assertEquals("invalid role " + LICENSE_OWNER, e.getMessage());
        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        assertDoesNotThrow(() -> {
            PolicyBuilder.buildPolicyForAssetDecision(ctx);
        });
        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        assertDoesNotThrow(() -> {
            PolicyBuilder.buildPolicyForAssetDecision(ctx);
        });
    }

    @Test
    void testPrivilegesOnAssetDecision() throws PMException {
        MockContext ctx = newTestContext(MockIdentity.ORG1_LO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        PAP pap = buildPolicyForAssetDecision(ctx);
        test(ctx, pap, ASSET_TARGET, new AccessRightSet(READ_ASSETS, READ_ASSET_DETAIL, WRITE_ASSET));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        pap = buildPolicyForAssetDecision(ctx);
        test(ctx, pap, ASSET_TARGET, new AccessRightSet(ALLOCATE_LICENSE, READ_ASSETS, READ_ASSET_DETAIL));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        pap = buildPolicyForAssetDecision(ctx);
        test(ctx, pap, ASSET_TARGET, new AccessRightSet(READ_ASSETS));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        pap = buildPolicyForAssetDecision(ctx);
        test(ctx, pap, ASSET_TARGET, new AccessRightSet(READ_ASSETS));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        pap = buildPolicyForAssetDecision(ctx);
        test(ctx, pap, ASSET_TARGET, new AccessRightSet());

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        pap = buildPolicyForAssetDecision(ctx);
        test(ctx, pap, ASSET_TARGET, new AccessRightSet());

        ctx.setClientIdentity(MockIdentity.ORG1_LO);
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        pap = buildPolicyForAssetDecision(ctx);
        test(ctx, pap, ASSET_TARGET, new AccessRightSet());
    }

    @Test
    void testPrivilegesOnAccountDecision() throws PMException {
        MockContext ctx = newTestContext(MockIdentity.ORG1_LO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        PAP pap = buildPolicyForAccountDecision(ctx, "Org2MSP");
        test(ctx, pap, accountTarget("Org2MSP"), new AccessRightSet(READ_ORDER, READ_SWID));
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        pap = buildPolicyForAccountDecision(ctx, "Org2MSP");
        test(ctx, pap, accountTarget("Org2MSP"), new AccessRightSet());

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        pap = buildPolicyForAccountDecision(ctx, "Org2MSP");
        test(ctx, pap, accountTarget("Org2MSP"), new AccessRightSet());
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        pap = buildPolicyForAccountDecision(ctx, "Org2MSP");
        test(ctx, pap, accountTarget("Org2MSP"), new AccessRightSet(READ_ORDER, READ_SWID, READ_LICENSE));


        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        pap = buildPolicyForAccountDecision(ctx, "Org2MSP");
        test(ctx, pap, accountTarget("Org2MSP"), new AccessRightSet(APPROVE_ORDER, READ_ORDER, READ_SWID, DENY_ORDER, READ_LICENSE));
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        pap = buildPolicyForAccountDecision(ctx, "Org2MSP");
        test(ctx, pap, accountTarget("Org2MSP"), new AccessRightSet());

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        pap = buildPolicyForAccountDecision(ctx, "Org3MSP");
        test(ctx, pap, accountTarget("Org3MSP"), new AccessRightSet());

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        pap = buildPolicyForAccountDecision(ctx, "Org2MSP");
        test(ctx, pap, accountTarget("Org2MSP"), new AccessRightSet(READ_ORDER, RETURN_LICENSE, READ_SWID, INITIATE_ORDER, WRITE_SWID, READ_LICENSE));
        ctx.getStub().setAccountStatus(Status.UNAUTHORIZED);
        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        pap = buildPolicyForAccountDecision(ctx, "Org2MSP");
        test(ctx, pap, accountTarget("Org2MSP"), new AccessRightSet());

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        pap = buildPolicyForAccountDecision(ctx, "Org3MSP");
        test(ctx, pap, accountTarget("Org3MSP"), new AccessRightSet());
    }

    private void test(Context ctx, PAP pap, String target, AccessRightSet arset) throws PMException {
        UserContext user = PolicyBuilder.getUserContextFromCID(ctx.getClientIdentity());
        Map<String, AccessRightSet> map = pap
                .query()
                .access()
                .computeCapabilityList(user);

        assertEquals(arset, map.get(target));
    }
}