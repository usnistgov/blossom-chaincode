package ngac;

import gov.nist.csd.pm.impl.memory.pap.MemoryPAP;
import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.exception.PMException;
import gov.nist.csd.pm.pap.graph.relationship.AccessRightSet;
import gov.nist.csd.pm.pap.prohibition.ContainerCondition;
import gov.nist.csd.pm.pap.prohibition.ProhibitionSubject;
import gov.nist.csd.pm.pap.query.UserContext;
import model.Status;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.hyperledger.fabric.Logger;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;

import static gov.nist.csd.pm.pap.op.AdminAccessRights.ALL_ACCESS_RIGHTS;
import static model.Status.*;

public class PolicyBuilder {

    /*
    update to ADMINMSP of deployment
     */
    public static final String ADMINMSP = "Org1MSP";

    // roles
    public static final String BLOSSOM_ROLE_ATTR = "blossom.role";
    public static final String ACQ_OFFICER = "Acquisition Officer";
    public static final String TPOC = "Technical Point of Contact";
    public static final String LICENSE_OWNER = "License Owner";

    public static final String AUTH_CHAINCODE_NAME = "authorization";
    public static final String AUTH_CHANNEL_NAME = "authorization";

    // access rights
    public static final String ASSET_TARGET = "asset_target";
    public static final String WRITE_ASSET = "write_asset";
    public static final String READ_ASSETS = "read_assets";
    public static final String READ_ASSET_DETAIL = "read_asset_detail";
    public static final String READ_ORDER = "read_order";
    public static final String ALLOCATE_LICENSE = "allocate_license";
    public static final String RETURN_LICENSE = "return_license";
    public static final String READ_LICENSE = "read_license";
    public static final String WRITE_SWID = "write_swid";
    public static final String READ_SWID = "read_swid";
    public static final String INITIATE_ORDER = "initiate_order";
    public static final String APPROVE_ORDER = "approve_order";
    public static final String DENY_ORDER = "deny_order";
    public static final AccessRightSet RESOURCE_ARSET = new AccessRightSet(
            WRITE_ASSET,
            READ_ASSETS,
            READ_ASSET_DETAIL,
            INITIATE_ORDER,
            APPROVE_ORDER,
            DENY_ORDER,
            READ_ORDER,
            ALLOCATE_LICENSE,
            RETURN_LICENSE,
            READ_LICENSE,
            WRITE_SWID,
            READ_SWID
    );

    private static final Logger log = Logger.getLogger("PolicyBuilder");

    public static PAP buildPolicyForAssetDecision(Context ctx) throws PMException {
        PAP pap = buildPolicyBase(ctx);

        // create asset target for decisions
        pap.modify().graph().createObject(ASSET_TARGET, List.of("RBAC/asset", "Status/asset"));

        // deny non adminmsp ACQ eleveated privs on assets
        String cidAccount = ctx.getClientIdentity().getMSPID();
        if (!cidAccount.equals(ADMINMSP)) {
            pap.modify().prohibitions().createProhibition(
                    "deny non-adminmsp ACQ",
                    ProhibitionSubject.userAttribute(ACQ_OFFICER),
                    new AccessRightSet(READ_ASSET_DETAIL, ALLOCATE_LICENSE),
                    false,
                    List.of(new ContainerCondition("RBAC/asset", false))
            );
        }

        return pap;
    }

    public static PAP buildPolicyForAccountDecision(Context ctx, String targetAccount) throws PMException {
        PAP pap = buildPolicyBase(ctx);

        String accountTarget = accountTarget(targetAccount);
        String targetAccountOA = accountOA(targetAccount);

        // create target account config
        pap.modify().graph().createObjectAttribute(targetAccountOA, List.of("Account"));
        pap.modify().graph().createObject(accountTarget, List.of(targetAccountOA, "RBAC/account", "Status/account"));

        // if the targetAccountUA exists, then the request is from the same account, associate the ua and oa
        // otherwise do nothing as the accounts dont match, adminmsp will be taken care of in following block
        String targetAccountUA = accountUA(targetAccount);
        if (pap.query().graph().nodeExists(targetAccountUA)) {
            pap.modify().graph().associate(targetAccountUA, targetAccountOA, new AccessRightSet(ALL_ACCESS_RIGHTS));
        }

        // if cid is adminmsp, grant access to target account oa and
        // deny adminmsp elevated privs on account target
        String cidAccount = ctx.getClientIdentity().getMSPID();
        if (cidAccount.equals(ADMINMSP)) {
            String cidAcctUA = accountUA(cidAccount);
            pap.modify().graph().associate(cidAcctUA, targetAccountOA, new AccessRightSet(ALL_ACCESS_RIGHTS));

            pap.modify().prohibitions().createProhibition(
                    "deny non-adminmsp ACQ",
                    ProhibitionSubject.userAttribute(ACQ_OFFICER),
                    new AccessRightSet(APPROVE_ORDER, DENY_ORDER),
                    false,
                    List.of(new ContainerCondition("RBAC/account", false))
            );
        }

        return pap;
    }

    private static PAP buildPolicyBase(Context ctx) throws PMException {
        PAP pap = new MemoryPAP();

        pap.modify().operations().setResourceOperations(RESOURCE_ARSET);

        // build attribute hierarchy
        buildAttributes(pap);

        // build account ua config
        String cidAccount = ctx.getClientIdentity().getMSPID();
        String accountUA = accountUA(cidAccount);
        pap.modify().graph().createUserAttribute(accountUA, List.of("Account"));

        // create user and assign to attributes
        UserContext userContext = getUserContextFromCID(ctx.getClientIdentity());
        String role = getRole(ctx, cidAccount);
        Status status = getAccountStatus(ctx);
        pap.modify().graph().createUser(userContext.getUser(), List.of(role, accountUA, status.toString()));

        log.info("building policy for user " + userContext.getUser() + " with attributes [" + role + ", " + accountUA + ", " + status + "]");

        return pap;
    }

    private static void buildAttributes(PAP pap) throws PMException {
        // RBAC PC
        pap.modify().graph().createPolicyClass("RBAC");

        pap.modify().graph().createObjectAttribute("RBAC/asset", List.of("RBAC"));
        pap.modify().graph().createObjectAttribute("RBAC/account", List.of("RBAC"));

        pap.modify().graph().createUserAttribute(LICENSE_OWNER, List.of("RBAC"));
        pap.modify().graph().createUserAttribute(ACQ_OFFICER, List.of("RBAC"));
        pap.modify().graph().createUserAttribute(TPOC, List.of("RBAC"));

        // LO
        pap.modify().graph().associate(LICENSE_OWNER, "RBAC/asset", new AccessRightSet(
                READ_ASSETS,
                WRITE_ASSET,
                READ_ASSET_DETAIL
        ));
        pap.modify().graph().associate(LICENSE_OWNER, "RBAC/account", new AccessRightSet(
                READ_ORDER,
                READ_SWID
        ));

        // ACQ
        pap.modify().graph().associate(ACQ_OFFICER, "RBAC/asset", new AccessRightSet(
                READ_ASSETS,
                READ_ASSET_DETAIL,
                ALLOCATE_LICENSE
        ));
        pap.modify().graph().associate(ACQ_OFFICER, "RBAC/account", new AccessRightSet(
                READ_ORDER,
                APPROVE_ORDER,
                DENY_ORDER,
                READ_LICENSE,
                READ_SWID
        ));

        // TPOC
        pap.modify().graph().associate(TPOC, "RBAC/asset", new AccessRightSet(READ_ASSETS));
        pap.modify().graph().associate(TPOC, "RBAC/account", new AccessRightSet(
                INITIATE_ORDER,
                READ_ORDER,
                READ_SWID,
                WRITE_SWID,
                READ_LICENSE,
                RETURN_LICENSE
        ));

        // Account PC
        pap.modify().graph().createPolicyClass("Account");

        // Status PC
        pap.modify().graph().createPolicyClass("Status");
        pap.modify().graph().createUserAttribute(AUTHORIZED.toString(), List.of("Status"));
        pap.modify().graph().createUserAttribute(PENDING.toString(), List.of("Status"));
        pap.modify().graph().createUserAttribute(UNAUTHORIZED.toString(), List.of(PENDING.toString()));

        pap.modify().graph().createObjectAttribute("Status/account", List.of("Status"));
        pap.modify().graph().createObjectAttribute("Status/asset", List.of("Status"));

        pap.modify().graph().associate(AUTHORIZED.name(), "Status/account", new AccessRightSet(ALL_ACCESS_RIGHTS));
        pap.modify().graph().associate(AUTHORIZED.name(), "Status/asset", new AccessRightSet(ALL_ACCESS_RIGHTS));
    }

    public static UserContext getUserContextFromCID(ClientIdentity cid) {
        X509Certificate cert = cid.getX509Certificate();
        String mspid = cid.getMSPID();

        String user;
        try {
            JcaX509CertificateHolder jcaX509CertificateHolder = new JcaX509CertificateHolder(cert);
            X500Name subject = jcaX509CertificateHolder.getSubject();
            RDN cnRDN = subject.getRDNs(BCStyle.CN)[0];
            AttributeTypeAndValue first = cnRDN.getFirst();
            user = first.getValue().toString();
        } catch (CertificateEncodingException e) {
            throw new ChaincodeException(e);
        }

        return new UserContext(user + ":" + mspid);
    }

    private static Status getAccountStatus(Context ctx) {
        // invoke the ATO channel chaincode to get the status of the requesting account using GetAccountStatus
        // the MSPID to check is embedded in the request context
        Chaincode.Response response = ctx.getStub()
                                         .invokeChaincode(
                                                 AUTH_CHAINCODE_NAME,
                                                 List.of("account:GetAccountStatus".getBytes(StandardCharsets.UTF_8)),
                                                 AUTH_CHANNEL_NAME
                                         );
        return Status.fromString(response.getStringPayload());
    }

    private static String getRole(Context ctx, String account) {
        String role = ctx.getClientIdentity().getAttributeValue(BLOSSOM_ROLE_ATTR);

        // if adminmsp only SO and ACQ are allowed roles
        // if not adminmsp only TPOC and ACQ are allowed roles
        if ((account.equals(ADMINMSP) && !(role.equals(LICENSE_OWNER) || role.equals(ACQ_OFFICER))) ||
                (!account.equals(ADMINMSP) && !(role.equals(TPOC) || role.equals(ACQ_OFFICER)))) {
            throw new ChaincodeException("invalid role " + role);
        }

        return role;
    }

    public static String accountTarget(String account) {
        return account + "_target";
    }

    private static String accountUA(String account) {
        return account + "_UA";
    }

    private static String accountOA(String account) {
        return account + "_OA";
    }
}
