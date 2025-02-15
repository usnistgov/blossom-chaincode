package contract;

import model.Account;
import model.AccountHistorySnapshot;
import ngac.BlossomPDP;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Chaincode functions to manage Blossom accounts.
 */
@Contract(
        name = "account",
        info = @Info(
                title = "Blossom Authorization Account Contract",
                description = "Functions to retrieve information about Blossom accounts",
                version = "0.0.1"
        )
)
public class AccountContract implements ContractInterface {

    private static final String ACCOUNT_PREFIX = "account:";

    public static String accountKey(String accountId) {
        return ACCOUNT_PREFIX + accountId;
    }

    public static String accountImplicitDataCollection(String accountId) {
        return "_implicit_org_" + accountId;
    }

    /**
     * Join signals the members intent to join the information exchange. This function does not have any impact on the
     * status of the member as that is completed during the voting process before this.
     *
     * NGAC: the member must have already been voted as AUTHORIZED in order to join.
     *
     * event:
     *  - name: "Join"
     *  - payload: a serialized Account object
     *
     * @param ctx Fabric context object.
     * @throws ChaincodeException If the cid MSPID has not signed the MOU first.
     * @throws ChaincodeException If the cid MSPID has already joined.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    @Transaction
    public void Join(Context ctx) {
        String accountId = ctx.getClientIdentity().getMSPID();

        new BlossomPDP().join(ctx, accountId);

        Account account = new AccountContract().GetAccount(ctx, accountId);
        if (account.isJoined()) {
            throw new ChaincodeException(accountId + " has already joined");
        }

        account.setJoined(true);

        // update the account
        byte[] bytes = SerializationUtils.serialize(account);
        ctx.getStub().putState(accountKey(accountId), bytes);

        ctx.getStub().setEvent("Join", new byte[]{});
    }

    /**
     * Get all accounts in Blossom and their statuses.
     * <p>
     * NGAC: none.
     *
     * @param ctx Fabric context object.
     *
     * @return A list of Accounts.
     */
    @Transaction
    public Account[] GetAccounts(Context ctx) {
        List<Account> accounts = new ArrayList<>();

        try(QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getStateByRange("", "")) {
            for (KeyValue next : stateByRange) {
                if (!next.getKey().startsWith(ACCOUNT_PREFIX)) {
                    continue;
                }

                byte[] value = next.getValue();
                accounts.add(SerializationUtils.deserialize(value));
            }
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }

        return accounts.toArray(Account[]::new);
    }

    /**
     * Get the account with the given name.
     *
     * NGAC: none.
     *
     * @param ctx Fabric context object.
     * @param accountId The account to return.
     * @return The account information of the account with the given MSPID.
     * @throws ChaincodeException if an account with the given MSPID does not exist.
     */
    @Transaction
    public Account GetAccount(Context ctx, String accountId) {
        byte[] bytes = ctx.getStub().getState(accountKey(accountId));
        if (bytes.length == 0) {
            throw new ChaincodeException("an account with id " + accountId + " does not exist");
        }

        return SerializationUtils.deserialize(bytes);
    }

    /**
     * Get the status of the account that the requesting CID belongs to.
     * <p>
     * NGAC: none.
     *
     * @param ctx Fabric context object.
     *
     * @return The status of the account.
     *
     * @throws ChaincodeException If the account in the CID does not exist.
     */
    @Transaction
    public String GetAccountStatus(Context ctx) {
        String accountId = ctx.getClientIdentity().getMSPID();
        Account account = GetAccount(ctx, accountId);

        return account.getStatus().toString();
    }

    /**
     * Get the history for a given account identified by the mspid. Each historySnapshot will contain the TxID and
     * timestamp of the update and the account information at the point in time.
     * <p>
     * NGAC: none.
     *
     * @param ctx       Fabric context object.
     * @param accountId The MSPID of the account to get the history for.
     *
     * @return A list of HistorySnapshots containing the history of the given account.
     */
    @Transaction
    public AccountHistorySnapshot[] GetAccountHistory(Context ctx, String accountId) {
        List<AccountHistorySnapshot> accountHistorySnapshots = new ArrayList<>();
        QueryResultsIterator<KeyModification> historyForKey =
                ctx.getStub().getHistoryForKey(accountKey(accountId));
        for (KeyModification next : historyForKey) {
            String txID = next.getTxId();
            Instant timestamp = next.getTimestamp();
            byte[] value = next.getValue();

            accountHistorySnapshots.add(
                    new AccountHistorySnapshot(txID, timestamp.toString(), SerializationUtils.deserialize(value))
            );
        }

        return accountHistorySnapshots.toArray(AccountHistorySnapshot[]::new);
    }
}
