import {buildCCPOrg1, buildCCPOrg2, buildCCPOrg3, buildWallet} from "./utils/AppUtil";
import * as path from 'path';
import {Contract, Gateway, GatewayOptions, TransientMap, Wallet} from "fabric-network";

export const org1Gateway = new Gateway();
export const org2Gateway = new Gateway();
export const org3Gateway = new Gateway();

export const org1WalletPath = path.join(__dirname, 'wallet', 'org1');
export const org2WalletPath = path.join(__dirname, 'wallet', 'org2');
export const org3WalletPath = path.join(__dirname, 'wallet', 'org3');

export const AORole = 'Authorizing Official';
export const ACQRole = 'Acquisition Officer';
export const TPOCRole = 'Technical Point of Contact';
export const LORole = 'License Owner';

export interface Orgs {
    org1: Org,
    org2: Org,
    org3: Org,
}

export interface Org {
    id: string,
    wallet: Wallet,
    ccp: Record<string, any>,
    caHostName: string,
    gateway: Gateway
}

export interface Users {
    org1: OrgUsers,
    org2: OrgUsers,
    org3: OrgUsers,
}

export interface OrgUsers {
    AO: User,
    ACQ: User,
    TPOC: User,
    LO: User,
}

export interface User {
    org: Org,
    name: string,
    role: string
}

export async function buildTestUsers(): Promise<Users> {
    const orgs: Orgs = {
        org1: {
            id: "Org1MSP",
            wallet: await buildWallet(org1WalletPath),
            ccp: buildCCPOrg1(),
            caHostName: "ca.org1.example.com",
            gateway: org1Gateway
        },
        org2: {
            id: "Org2MSP",
            wallet: await buildWallet(org2WalletPath),
            ccp: buildCCPOrg2(),
            caHostName: "ca.org2.example.com",
            gateway: org2Gateway
        },
        org3: {
            id: "Org3MSP",
            wallet: await buildWallet(org3WalletPath),
            ccp: buildCCPOrg3(),
            caHostName: "ca.org3.example.com",
            gateway: org3Gateway
        }
    }

    return {
        org1: {
            AO:     {org: orgs.org1, name: "org1_AO", role: AORole},
            ACQ:    {org: orgs.org1, name: "org1_ACQ", role: ACQRole},
            TPOC:   {org: orgs.org1, name: "org1_TPOC", role: TPOCRole},
            LO:     {org: orgs.org1, name: "org1_LO", role: LORole},
        },
        org2: {
            AO:     {org: orgs.org2, name: "org2_AO", role: AORole},
            ACQ:    {org: orgs.org2, name: "org2_ACQ", role: ACQRole},
            TPOC:   {org: orgs.org2, name: "org2_TPOC", role: TPOCRole},
            LO: null,
        },
        org3: {
            AO:     {org: orgs.org3, name: "org3_AO", role: AORole},
            ACQ:    {org: orgs.org3, name: "org3_ACQ", role: ACQRole},
            TPOC:   {org: orgs.org3, name: "org3_TPOC", role: TPOCRole},
            LO: null,
        }
    }
}

export const ASSET = "asset";
export const AUTH = "authorization";

export const ato = "ato";
export const mou = "mou";
export const vote = "vote";
export const account = "account";
export const bootstrap = "bootstrap";
export const asset = "asset";
export const order = "order";
export const swid = "swid";

export const ORG_1 = "Org1MSP";
export const ORG_2 = "Org2MSP";
export const ORG_3 = "Org3MSP";

export async function submitTx(
    user: User,
    func: string,
    params: any[] = [],
    endorsingOrgs: string[] = [ORG_1],
    transientData: any = null,
    expectedErrMsg?: string) {
    const contractName = getContractName(func);
    const chaincode = getChaincodeName(contractName);

    let contract = await getContract(user, chaincode, chaincode, contractName);
    let tx = contract.createTransaction(func);

    tx.setTransient(buildTransientMap(chaincode, transientData));
    tx.setEndorsingOrganizations(...endorsingOrgs)

    let response: any = null;
    await tx.submit(...params)
        .then(buf => {
            if (buf.length != 0) {
                try {
                    response = JSON.parse(buf.toString());
                } catch(e) {
                    response = buf.toString();
                }
            }
        })
        .catch(error => {
            expect(JSON.stringify(error)).toContain(expectedErrMsg);
        });

    return response;
}

function getContractName(func: string): string {
    if(func === "Bootstrap"){
        return bootstrap;
    }

    else if(func === "Join"||func === "GetAccounts"||func === "GetAccount"||func === "GetAccountStatus"||func === "GetAccountHistory"){
        return account;
    }

    else if(func === "UpdateMOU"||func === "GetMOU"||func === "GetMOUHistory"||func === "SignMOU"){
        return mou;
    }

    else if(func === "CreateATO"||func === "UpdateATO"||func === "GetATO"||func === "SubmitFeedback") {
        return ato;
    }

    else if (func === "InitiateVote"||func === "Vote"||func === "CertifyOngoingVote"||func === "GetOngoingVote"||func === "GetVoteHistory") {
        return vote;
    }

    else if(func === "AddAsset"||func === "AddLicenses"||func === "RemoveLicenses"||func === "UpdateEndDate"||func === "RemoveAsset"||func === "GetAssets"||func === "GetAsset"||func === "GetLicenseTxHistory") {
        return asset;
    }

    else if (func === "GetQuote"||func === "SendQuote"||func === "InitiateOrder"||func === "ApproveOrder"||func === "DenyOrder"||func === "GetLicensesToAllocateForOrder"||func === "AllocateLicenses"||func === "GetAllocateRequestForOrder"||func === "SendLicenses"||func === "InitiateReturn"||func === "GetInitiatedReturnForOrder"||func === "DeallocateLicensesFromAccount"||func === "DeallocateLicensesFromSP"||func === "GetOrder"||func === "GetOrdersByAccount"||func === "GetOrdersByAccountAndAsset"||func === "GetOrdersByAsset"||func === "GetAvailableLicensesForOrder"||func === "GetExpiredOrders"){
        return order;
    }

    else if(func === "ReportSWID"||func === "GetSWID"||func === "DeleteSWID"||func === "GetLicensesWithSWIDsForOrder"){
        return swid;
    }

}

function getChaincodeName(contract: string): string {
    if (contract === bootstrap || contract === account || contract === mou || contract === ato || contract === vote) {
        return AUTH;
    } else {
        return ASSET;
    }
}

export async function getContract(user: User, channel: string, chaincode: string, contract: string): Promise<Contract> {
    try {
        const gatewayOpts: GatewayOptions = {
            wallet: user.org.wallet,
            identity: user.name,
            discovery: { enabled: true, asLocalhost: true },
        };

        await user.org.gateway.connect(user.org.ccp, gatewayOpts);
        const network = await user.org.gateway.getNetwork(channel);
        return network.getContract(chaincode, contract);
    } catch (error) {
        console.error(`******** FAILED to run the application: ${error}`);
    }
}

function buildTransientMap(chaincode: string, transientData: any): TransientMap {
    const map: TransientMap = {};

    if (transientData !== null) {

        if (chaincode == 'authorization') {
            for (const key in transientData) {
                const buf = Buffer.from(transientData[key].toString())
                map[key] = buf
            }
        } else {
            const json = JSON.stringify(transientData);
            const buf = Buffer.from(json);
            map["request"] = buf;
        }
    }

    return map;
}

export async function completeVote(users: Users, org: string, statusChange: string): Promise<string> {
    // initiate
    await submitTx(users.org1.AO, "InitiateVote", [org, statusChange, "reason"], [ORG_1], null);

    // vote
    await submitTx(users.org1.AO, "Vote", [true], [ORG_1], null);

    // vote
    try {
        await submitTx(users.org2.AO, "Vote", [true], [ORG_1], null);
    } catch (e) {}

    // vote
    try {
        await submitTx(users.org3.AO, "Vote", [true], [ORG_1], null);
    } catch (e) {}

    // get vote id
    const v = await submitTx(users.org1.AO, "GetOngoingVote", [], [ORG_1], null);
    const id = v.id;

    // certify vote
    await submitTx(users.org1.AO, "CertifyOngoingVote", [], [ORG_1], null);

    return id;
}

