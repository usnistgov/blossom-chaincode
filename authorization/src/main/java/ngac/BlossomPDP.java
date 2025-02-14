package ngac;

import gov.nist.csd.pm.impl.memory.pap.MemoryPAP;
import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.exception.PMException;
import gov.nist.csd.pm.pap.graph.node.Node;
import gov.nist.csd.pm.pap.graph.relationship.AccessRightSet;
import gov.nist.csd.pm.pap.pml.value.StringValue;
import gov.nist.csd.pm.pap.query.UserContext;
import gov.nist.csd.pm.pap.query.explain.Explain;
import gov.nist.csd.pm.pap.serialization.json.JSONDeserializer;
import gov.nist.csd.pm.pap.serialization.json.JSONSerializer;
import gov.nist.csd.pm.pap.serialization.pml.PMLDeserializer;
import gov.nist.csd.pm.pdp.AdminAdjudicationResponse;
import gov.nist.csd.pm.pdp.Decision;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pdp.exception.UnauthorizedException;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.hyperledger.fabric.Logger;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static gov.nist.csd.pm.pap.graph.node.NodeType.UA;

/**
 * Provides methods for checking access to Blossom resources and updating the policy as needed.
 */
public class BlossomPDP {

    // MODIFY VALUE TO MSPID OF ADMIN MEMBER OF NETWORK
    public static final String ADMINMSP = "Org1MSP";

    private static final String BLOSSOM_TARGET = "blossom_target";
    private static final String BLOSSOM_ROLE_ATTR = "blossom.role";
    private static final String AUTHORIZING_OFFICIAL = "Authorizing Official";
    private static final Logger log = Logger.getLogger(BlossomPDP.class);

    public BlossomPDP() {

    }

    public String[] getAllRoles() {
        return new String[]{"Authorizing Official"};
    }

    public String[] getAllPrivileges() {
        return new String[]{
                "bootstrap",
                "update_mou",
                "get_mou",
                "sign_mou",
                "join",
                "write_ato",
                "read_ato",
                "submit_feedback",
                "initiate_vote",
                "vote",
                "certify_vote",
        };
    }

    /**
     * Initialize the NGAC policy with the given PML string. The requesting user needs the "bootstrap" permission on the
     * blossom target.
     *
     * @param ctx               Chaincode context.
     *
     * @return A PAP object that stores an in memory policy from the given PML.
     *
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void bootstrap(Context ctx) throws IOException {
        UserContext userCtx = getUserCtxFromRequest(ctx);

        // get the policy file defined in PML
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("policy.pml");
        if (resourceAsStream == null) {
            throw new ChaincodeException("could not read policy file");
        }

        String pml = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);

        try {
            // create a new PAP object to compile and execute the PML
            PAP pap = new MemoryPAP();
            pap.setPMLConstants(Map.of("ADMINMSP", new StringValue(ADMINMSP)));
            pap.deserialize(userCtx, pml, new PMLDeserializer());

            // decide if user can bootstrap blossom
            prepareDecision(ctx, userCtx, pap, true);
            decide(pap, userCtx, BLOSSOM_TARGET, "bootstrap", "cid is not authorized to bootstrap Blossom");

            // write policy to world state and ledger
            savePolicy(ctx, pap, userCtx);
        } catch (PMException e) {
            throw new ChaincodeException(e);
        }
    }

    public void join(Context ctx, String account) {
        UserContext userCtx = getUserCtxFromRequest(ctx);

        decide(ctx, userCtx, accountObjectNodeName(account), "join", true, "cid is not authorized to join for account " + account);
    }

    public void readATO(Context ctx, String account) {
        UserContext userCtx = getUserCtxFromRequest(ctx);

        decide(ctx, userCtx, accountObjectNodeName(account), "read_ato", true, "cid is not authorized to read the ATO of account " + account);
    }

    /**
     * Check if the cid has "update_mou" on BLOSSOM_TARGET
     * @param ctx The Fabric context.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void updateMOU(Context ctx) {
        UserContext userCtx = getUserCtxFromRequest(ctx);

        decide(ctx, userCtx, BLOSSOM_TARGET, "update_mou", true, "cid is not authorized to update the Blossom MOU");
    }

    /**
     * Check if the cid has "sign_mou" on BLOSSOM_TARGET. Since this can be called before an account is called, do not
     * assign the user to the account user attribute, just the role.
     *
     * @param ctx The Fabric context.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void signMOU(Context ctx) {
        executeAdminOperation(
                ctx,
                "signMOU",
                Map.of("accountId", ctx.getClientIdentity().getMSPID()),
                false,
                "cid is not authorized to sign the MOU"
        );
    }

    /**
     * Check if the cid has "write_ato" on <account> target.
     *
     * @param ctx The Fabric context.
     * @param account The account id.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void writeATO(Context ctx, String account) {
        String target = accountObjectNodeName(account);
        UserContext userCtx = getUserCtxFromRequest(ctx);

        decide(ctx, userCtx, target, "write_ato", true, "cid is not authorized to write the ATO for account " + account);
    }

    /**
     * Check if the cid has "submit_feedback" on <account> target.
     *
     * @param ctx The Fabric context.
     * @param targetMember The target of the feedback.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void submitFeedback(Context ctx, String targetMember) {
        String target = accountObjectNodeName(targetMember);
        UserContext userCtx = getUserCtxFromRequest(ctx);

        decide(ctx, userCtx, target, "submit_feedback", true, "cid is not authorized to submit feedback on " + targetMember);
    }

    /**
     * Check if the cid has "initiate_vote" on <account> target. If yes, invoke the initiateVote function.
     *
     * @param ctx          Chaincode context.
     * @param targetMember The target member of the vote.
     *
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void initiateVote(Context ctx, String targetMember) {
        executeAdminOperation(
                ctx,
                "initiateVote",
                Map.of(
                        "initiator", ctx.getClientIdentity().getMSPID(),
                        "targetMember", targetMember
                ),
                true,
                "cid is not authorized to initiate a vote on " + targetMember
        );
    }

    /**
     * Check if the cid has "vote" on the vote object.
     *
     * @param ctx Chaincode context.
     * @param targetMember The target member of the vote.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void vote(Context ctx, String targetMember) {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        decide(ctx, userCtx, voteObj(targetMember), "vote", true,
                "cid is not authorized to vote on " + targetMember);
    }

    /**
     * Check if the cid has "certify_vote" on the vote object. if yes, invoke the endVote function.
     *
     * @param ctx          Chaincode context.
     * @param targetMember The target member of the vote.
     * @param passed       If the vote passed or not.
     *
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void certifyVote(Context ctx, String targetMember, String status, boolean passed) {
        executeAdminOperation(
                ctx,
                "certifyVote",
                Map.of("targetMember", targetMember, "status", status, "passed", passed),
                true,
                "cid is not authorized to certify a vote on " + targetMember
        );
    }

    private void executeAdminOperation(Context ctx, String opName, Map<String, Object> operands, boolean assignToAccountUA, String unauthMessage) {
        UserContext userCtx = getUserCtxFromRequest(ctx);

        try {
            // load the policy from the world state
            PAP pap = getPAPState(ctx, userCtx);

            // prepare the decision by assigning the user to their attributes in the policy
            prepareDecision(ctx, userCtx, pap, assignToAccountUA);

            // execute the signMOU operation which will check for privileges before executing
            PDP pdp = new PDP(pap);
            AdminAdjudicationResponse response = pdp.adjudicateAdminOperation(userCtx, opName, operands);
            if (response.getDecision() == Decision.DENY) {
                throw new PMException(unauthMessage);
            }

            // delete the user node from the graph before saving policy state
            pap.modify().graph().deleteNode(userCtx.getUser());

            // save the policy
            savePolicy(ctx, pap, userCtx);
        } catch (PMException e) {
            throw new ChaincodeException(e.getMessage());
        }
    }

    private void prepareDecision(Context ctx, UserContext userCtx, PAP pap, boolean assignToAccountUA) {
        try {
            String mspid = ctx.getClientIdentity().getMSPID();
            String role = ctx.getClientIdentity().getAttributeValue(BLOSSOM_ROLE_ATTR);
            String accountUA = accountUsersNodeName(mspid);

            if (!pap.query().graph().nodeExists(role)) {
                throw new ChaincodeException("unknown user role: " + role);
            } else if (assignToAccountUA && !pap.query().graph().nodeExists(accountUA)) {
                throw new ChaincodeException("account " + mspid + " does not exist");
            }

            // create the calling user in the graph and assign to appropriate attributes
            pap.modify().graph().createUser(userCtx.getUser(), List.of(role));

            // if account UA exists assign the user to it
            if (pap.query().graph().nodeExists(accountUA)) {
                pap.modify().graph().assign(userCtx.getUser(), List.of(accountUA));
            }

            // check if user is blossom admin
            if (ADMINMSP.equals(mspid) && role.equals(AUTHORIZING_OFFICIAL)) {
                pap.modify().graph().assign(userCtx.getUser(), List.of("Blossom Admin"));
            }
        } catch (PMException e) {
            throw new ChaincodeException(e);
        }
    }

    private void decide(Context ctx, UserContext userCtx, String target, String ar, boolean assignToAccountUA, String unauthMessage) {
        PAP pap = getPAPState(ctx, userCtx);

        prepareDecision(ctx, userCtx, pap, assignToAccountUA);

        decide(pap, userCtx, target, ar, unauthMessage);
    }

    private void decide(PAP pap, UserContext userCtx, String target, String ar, String unauthMessage) {
        try {
            AccessRightSet accessRights = pap.query().access().computePrivileges(userCtx, target);

            log.info("user " + userCtx.getUser() + " has privs " + accessRights + " on " + target);

            boolean result = accessRights.contains(ar);
            if (!result) {
                throw new ChaincodeException(unauthMessage);
            }

            pap.modify().graph().deleteNode(userCtx.getUser());
        } catch (PMException e) {
            throw new ChaincodeException(e);
        }
    }

    /**
     * Load the policy from the context into memory.
     * @param ctx The Fabric context.
     * @param userCtx The user context representing the cid.
     * @return The policy in memory.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public static PAP getPAPState(Context ctx, UserContext userCtx) {
        byte[] policy = ctx.getStub().getState("policy");
        if (policy.length == 0) {
            throw new ChaincodeException("ngac policy has not been initialized");
        }

        String json = new String(policy, StandardCharsets.UTF_8);

        try {
            MemoryPAP pap = new MemoryPAP();
            pap.setPMLConstants(Map.of("ADMINMSP", new StringValue(ADMINMSP)));
            pap.deserialize(userCtx, json, new JSONDeserializer());

            return pap;
        } catch (PMException e) {
            throw new ChaincodeException(e);
        }
    }

    private String accountUsersNodeName(String mspid) {
        return mspid + " users";
    }

    private String accountObjectNodeName(String mspid) {
        return mspid + " target";
    }

    private String voteObj(String targetMember) {
        return targetMember + " vote";
    }

    private static String getNGACUserName(Context ctx) {
        ClientIdentity clientIdentity = ctx.getClientIdentity();
        X509Certificate cert = clientIdentity.getX509Certificate();
        String mspid = ctx.getClientIdentity().getMSPID();

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

        return user + ":" + mspid;
    }

    public static UserContext getUserCtxFromRequest(Context ctx) {
        return new UserContext(getNGACUserName(ctx));
    }

    private void savePolicy(Context ctx, PAP pap, UserContext user) throws PMException {
        pap.modify().graph().deleteNode(user.getUser());
        ctx.getStub().putState("policy", pap.serialize(new JSONSerializer()).getBytes(StandardCharsets.UTF_8));
    }
}
