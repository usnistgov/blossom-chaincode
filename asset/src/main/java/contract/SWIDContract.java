package contract;

import contract.request.OrderIdAndAccountRequest;
import contract.request.SWIDRequest;
import contract.request.ReportSWIDRequest;
import model.Allocated;
import model.SWID;
import ngac.PDP;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

import static contract.AssetContract.*;
import static model.LicenseKey.allocatedLicenseKey;

@Contract(
        name = "swid",
        info = @Info(
                title = "Blossom asset chaincode SWID contract",
                description = "Chaincode functions to manage SWIDs",
                version = "0.0.1"
        )
)
public class SWIDContract implements ContractInterface {

    public static final String SWID_PREFIX = "swid:";
    public static String swidKey(String orderId, String licenseId) {
        return SWID_PREFIX + orderId + ":" + licenseId;
    }

    public static String getLicenseIdFromKey(String key) {
        return key.split(":")[2];
    }

    @Transaction
    public void ReportSWID(Context ctx) {
        ReportSWIDRequest req = new ReportSWIDRequest(ctx);

        PDP.canWriteSWID(ctx, req.getAccount());

        // check the license has been checked out by the account
        byte[] bytes = ctx.getStub()
                          .getPrivateData(
                                  accountIPDC(req.getAccount()),
                                  allocatedLicenseKey(req.getOrderId(), req.getLicenseId())
                          );
        if (bytes.length == 0) {
            throw new ChaincodeException("license not found");
        }

        // add to account's private data
        ctx.getStub().putPrivateData(
                accountIPDC(req.getAccount()),
                swidKey(req.getOrderId(), req.getLicenseId()),
                new SWID(req.getPrimaryTag(), req.getXml(), req.getOrderId(), req.getLicenseId()).toByteArray()
        );
    }

    @Transaction
    public void DeleteSWID(Context ctx) {
        SWIDRequest req = new SWIDRequest(ctx);

        // check user can delete swid
        PDP.canWriteSWID(ctx, req.getAccount());

        String collection = accountIPDC(req.getAccount());

        // check if swid with id exists
        String key = swidKey(req.getOrderId(), req.getLicenseId());
        byte[] bytes = ctx.getStub().getPrivateData(collection, key);
        if (bytes.length == 0) {
            throw new ChaincodeException("SWID for license " + req.getLicenseId() + " in order " + req.getOrderId() + " does not exist");
        }

        // delete from account's private data
        ctx.getStub().delPrivateData(collection, key);
    }

    @Transaction
    public SWID GetSWID(Context ctx) {
        SWIDRequest req = new SWIDRequest(ctx);

        PDP.canReadSWID(ctx, req.getAccount());

        String collection = accountIPDC(req.getAccount());

        // check if swid with id exists
        String key = swidKey(req.getOrderId(), req.getLicenseId());
        byte[] bytes = ctx.getStub().getPrivateData(collection, key);
        if (bytes.length == 0) {
            throw new ChaincodeException("SWID for license " + req.getLicenseId() + " in order " + req.getOrderId() + " does not exist");
        }

        return SWID.fromByteArray(bytes);
    }

    @Transaction
    public String[] GetLicensesWithSWIDsForOrder(Context ctx) {
        OrderIdAndAccountRequest req = new OrderIdAndAccountRequest(ctx);

        PDP.canReadLicense(ctx, req.getAccount());

        String collection = accountIPDC(req.getAccount());
        String key = swidKey(req.getOrderId(), "");
        try(QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getPrivateDataByRange(collection, key, key + "~")) {
            List<String> licenseIds = new ArrayList<>();
            for (KeyValue next : stateByRange) {
                String k = next.getKey();
                String id = getLicenseIdFromKey(k);
                licenseIds.add(id);
            }

            return licenseIds.toArray(String[]::new);
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }
}
