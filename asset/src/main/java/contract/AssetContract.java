package contract;

import contract.request.*;
import contract.response.AssetDetailResponse;
import contract.response.AssetResponse;
import contract.response.IdResponse;
import model.*;
import ngac.PDP;
import org.hyperledger.fabric.Logger;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.time.Instant;
import java.util.*;

import static model.DateFormatter.isExpired;
import static model.LicenseKey.hashedLicenseKey;
import static model.LicenseKey.licenseKey;
import static ngac.PolicyBuilder.ADMINMSP;

@Contract(
        name = "asset",
        info = @Info(
                title = "Blossom asset chaincode license contract",
                description = "Chaincode functions to manage assets and licenses",
                version = "0.0.1"
        )
)
public class AssetContract implements ContractInterface {

    public static final String  ADMINMSP_IPDC = accountIPDC(ADMINMSP);
    public static final String ASSET_PREFIX = "asset:";

    private static final Logger log = Logger.getLogger(AssetContract.class);

    public static String accountIPDC(String account) {
        return "_implicit_org_" + account;
    }

    public static String assetKey(String assetId) {
        return ASSET_PREFIX + assetId;
    }

    // use transient for license ids
    @Transaction
    public IdResponse AddAsset(Context ctx) {
        AddAssetRequest req = new AddAssetRequest(ctx);

        PDP.canWriteAsset(ctx);

        String id = ctx.getStub().getTxId();
        String startDate = ctx.getStub().getTxTimestamp().toString();

        // check format of end date
        DateFormatter.checkDateFormat(req.getEndDate());

        // build the asset object and write to ADMINMSP_IPDC
        Asset asset = new Asset(id, req.getName(), startDate, req.getEndDate());
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, assetKey(id), asset.toByteArray());

        // create the license keys, writing the hash to the public ledger
        for (LicenseIdWithSaltRequest licenseIdWithSaltRequest : req.getLicenses()) {
            String key = licenseKey(
                    id,
                    licenseIdWithSaltRequest.getId()
            );

            // write to adminmsp -- no need to hash as the key is written to pvt data
            ctx.getStub().putPrivateData(
                    ADMINMSP_IPDC,
                    key,
                    new License(licenseIdWithSaltRequest.getId(), licenseIdWithSaltRequest.getSalt(), null)
                            .toByteArray()
            );

            // write to ledger -- a byte array of length 1 for the value will indicate that it does exist on the ledger
            // but nothing is in the value
            ctx.getStub().putState(hashedLicenseKey(id, licenseIdWithSaltRequest.getId(), licenseIdWithSaltRequest.getSalt()), new byte[]{0});
        }

        return new IdResponse(id);
    }

    @Transaction
    public void AddLicenses(Context ctx) {
        PDP.canWriteAsset(ctx);

        // get asset info from transient data field
        AddLicensesRequest req = new AddLicensesRequest(ctx);

        // get the asset
        Asset asset = getAsset(ctx, req.getAssetId());

        // check that the licenses are not duplicates
        for (LicenseIdWithSaltRequest licenseIdWithSaltRequest : req.getLicenses()) {

            // it's ok to throw an exception here indicating that a license key exists because the cid would have passed
            // the ngac check for adding licenses to get here
            String licenseKey = licenseKey(asset.getId(), licenseIdWithSaltRequest.getId());
            if (ctx.getStub().getPrivateData(ADMINMSP_IPDC, licenseKey).length != 0) {
                throw new ChaincodeException("license " + licenseIdWithSaltRequest.getId() + " already exists");
            }

            // write to adminmsp -- a byte array of length 1 for the value will indicate that it does exist on the ledger but nothing is in the value
            ctx.getStub().putPrivateData(
                    ADMINMSP_IPDC,
                    licenseKey(asset.getId(), licenseIdWithSaltRequest.getId()),
                    new License(licenseIdWithSaltRequest.getId(), licenseIdWithSaltRequest.getSalt(), null).toByteArray()
            );

            // write to ledger
            ctx.getStub().putState(hashedLicenseKey(asset.getId(), licenseIdWithSaltRequest.getId(), licenseIdWithSaltRequest.getSalt()), new byte[]{0});
        }
    }

    @Transaction
    public void RemoveLicenses(Context ctx) {
        PDP.canWriteAsset(ctx);

        // get asset info from transient data field
        RemoveLicensesRequest req = new RemoveLicensesRequest(ctx);

        // get asset
        Asset asset = getAsset(ctx, req.getAssetId());

        // remove licenses from adminmsp ipdc as long as they exist and are not allocated
        for (String licenseId : req.getLicenses()) {
            String key = licenseKey(asset.getId(), licenseId);

            byte[] bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, key);
            if (bytes.length == 0) {
                throw new ChaincodeException("license " + licenseId + " does not exist");
            }

            License license = License.fromByteArray(bytes);
            if (license.getAllocated() != null){
                throw new ChaincodeException("license " + licenseId + " is allocated to " + license.getAllocated().getAccount());
            }

            // delete license key from ADMINMSP
            ctx.getStub().delPrivateData(ADMINMSP_IPDC, key);

            // delete license key from ledger
            ctx.getStub().delState(hashedLicenseKey(asset.getId(), licenseId, license.getSalt()));
        }
    }

    @Transaction
    public void UpdateEndDate(Context ctx) {
        PDP.canWriteAsset(ctx);

        UpdateEndDateRequest req = new UpdateEndDateRequest(ctx);

        // read asset from ledger
        Asset asset = getAsset(ctx, req.getAssetId());

        // update asset object
        DateFormatter.checkDateFormat(req.getNewEndDate());
        asset.setEndDate(req.getNewEndDate());

        // update asset state
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, assetKey(req.getAssetId()), asset.toByteArray());
    }

    @Transaction
    public AssetResponse[] GetAssets(Context ctx) {
        PDP.canReadAssets(ctx);

        List<AssetResponse> assets = new ArrayList<>();

        try(QueryResultsIterator<KeyValue> assetRange = getAssetQueryIterator(ctx, ASSET_PREFIX)) {
            for (KeyValue next : assetRange) {
                byte[] value = next.getValue();

                Asset asset = Asset.fromByteArray(value);

                AssetResponse assetResponse = new AssetResponse(
                        asset.getId(),
                        asset.getName(),
                        getAvailableLicenses(ctx, asset.getId()).size(),
                        asset.getStartDate(),
                        asset.getEndDate()
                );

                assets.add(assetResponse);
            }
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }

        return assets.toArray(AssetResponse[]::new);
    }

    @Transaction
    public AssetDetailResponse GetAsset(Context ctx) {
        AssetIdRequest req = new AssetIdRequest(ctx);

        Asset asset = getAsset(ctx, req.getAssetId());
        List<License> availableLicenses = getAvailableLicenses(ctx, asset.getId());

        // build a map with all allocated licenses: account -> orderId -> licenses
        int total = availableLicenses.size();
        Map<String, Map<String, Set<LicenseWithExpiration>>> allocatedLicenses = getAllocatedLicensesMap(ctx, req.getAssetId());
        for (Map.Entry<String, Map<String, Set<LicenseWithExpiration>>> e : allocatedLicenses.entrySet()) {
            for (Set<LicenseWithExpiration> value : e.getValue().values()) {
                total += value.size();
            }
        }

        Set<String> availableLicenseIds = new HashSet<>();
        for (License license : availableLicenses) {
            availableLicenseIds.add(license.getId());
        }

        AssetDetailResponse response = new AssetDetailResponse(
                asset.getId(),
                asset.getName(),
                availableLicenses.size(),
                asset.getStartDate(),
                asset.getEndDate(),
                total,
                availableLicenseIds,
                allocatedLicenses
        );

        return PDP.filterAssetDetail(ctx, response);
    }
    @Transaction
    public String[] GetLicenseTxHistory(Context ctx) {
        GetLicenseTxHistoryRequest req = new GetLicenseTxHistoryRequest(ctx);

        PDP.canReadAssetDetail(ctx);

        // get from adminmsp
        String key = licenseKey(req.getAssetId(), req.getLicenseId());
        byte[] bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, key);
        if (bytes.length == 0) {
            throw new ChaincodeException("license " + req.getLicenseId() + " does not exist for asset " + req.getAssetId());
        }

        License license = License.fromByteArray(bytes);
        String hash = hashedLicenseKey(req.getAssetId(), req.getLicenseId(), license.getSalt());

        try (QueryResultsIterator<KeyModification> historyForKey = ctx.getStub().getHistoryForKey(hash)) {
            List<String> txIds = new ArrayList<>();
            for (KeyModification next : historyForKey) {
                txIds.add(next.getTxId());
            }

            return txIds.toArray(String[]::new);
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }

    List<License> getAvailableLicenses(Context ctx, String assetId) {
        // provide empty license id to get all licenses for asset
        String key = licenseKey(assetId, "");
        try(QueryResultsIterator<KeyValue> licenseRange = getAssetQueryIterator(ctx, key)) {
            List<License> licenses = new ArrayList<>();

            for (KeyValue next : licenseRange) {
                byte[] nextLicenseBytes = next.getValue();
                License license = License.fromByteArray(nextLicenseBytes);
                if (license.getAllocated() == null) {
                    licenses.add(license);
                }
            }

            return licenses;
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }

    QueryResultsIterator<KeyValue> getAssetQueryIterator(Context ctx, String key) {
        return ctx.getStub().getPrivateDataByRange(ADMINMSP_IPDC, key, key + "~");
    }

    Asset getAsset(Context ctx, String assetId) {
        byte[] bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, assetKey(assetId));
        if (bytes.length == 0) {
            throw new ChaincodeException("asset with id " + assetId + " does not exist");
        }

        return Asset.fromByteArray(bytes);
    }

    private Map<String, Map<String, Set<LicenseWithExpiration>>> getAllocatedLicensesMap(Context ctx, String assetId) {
        String key = licenseKey(assetId, "");
        try(QueryResultsIterator<KeyValue> stateByRange = getAssetQueryIterator(ctx, key)) {
            // account -> order -> licenses
            Map<String, Map<String, Set<LicenseWithExpiration>>> map = new HashMap<>();

            for (KeyValue next : stateByRange) {
                byte[] value = next.getValue();

                License license = License.fromByteArray(value);
                if (license.getAllocated() == null) {
                    continue;
                }

                Map<String, Set<LicenseWithExpiration>> accountLicenses = map.getOrDefault(license.getAllocated().getAccount(), new HashMap<>());
                Set<LicenseWithExpiration> orderLicenses = accountLicenses.getOrDefault(license.getAllocated().getOrderId(), new HashSet<>());
                orderLicenses.add(new LicenseWithExpiration(license.getId(), license.getAllocated().getExpiration()));
                accountLicenses.put(license.getAllocated().getOrderId(), orderLicenses);
                map.put(license.getAllocated().getAccount(), accountLicenses);
            }

            return map;
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }
}
