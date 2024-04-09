import {buildTestUsers, completeVote, ORG_1, ORG_2, ORG_3, submitTx, User, Users} from "./testUtil";
import {Contract, GatewayOptions, TransientMap} from "fabric-network";


let users: Users;
beforeAll(async () => {
    users = await buildTestUsers();

    try {
        await submitTx(users.org1.AO, "Bootstrap", [], [ORG_1], null);
    } catch(e) {}

    // update mou
    await submitTx(users.org1.AO, "UpdateMOU", ["mou1"], [ORG_1], null)

    // sign mou both
    const response = await submitTx(users.org1.AO, "GetMOU", [], [ORG_1], null)
    const version = response.version
    await submitTx(users.org2.AO, "SignMOU", [version], [ORG_1], null)
    await submitTx(users.org3.AO, "SignMOU", [version], [ORG_1], null)

    await completeVote(users, ORG_2, "AUTHORIZED");
    await completeVote(users, ORG_3, "AUTHORIZED");

    // join both
    try {
        await submitTx(users.org2.AO, "Join", [], [ORG_2], null)
    } catch(e) {}

    try {
        await submitTx(users.org3.AO, "Join", [], [ORG_3], null)
    } catch(e) {}
})

describe('test transient inputs are correctly deserialized', () => {
    test("test CreateATO transient input", async () => {
        await submitTx(users.org2.AO, "CreateATO", [], [ORG_2], {
            "memo": "memo2",
            "artifacts": "artifacts2"
        })

        // get ato to ensure all fields were set properly
        const response = await submitTx(users.org2.AO, "GetATO", [ORG_2], [ORG_2], null)
        expect(response.memo).toEqual("memo2")
        expect(response.artifacts).toEqual("artifacts2")
    })

    test("test UpdateATO transient input", async () => {
        await submitTx(users.org2.AO, "UpdateATO", [], [ORG_2], {
            "memo": "memo2-1",
            "artifacts": "artifacts2-1"
        })

        // get ato to ensure all fields were set properly
        const response = await submitTx(users.org2.AO, "GetATO", [ORG_2], [ORG_2], null)
        expect(response.memo).toEqual("memo2-1")
        expect(response.artifacts).toEqual("artifacts2-1")
    })

    test("test SubmitFeedback transient input", async () => {
        await submitTx(users.org1.AO, "SubmitFeedback", [], [ORG_2], {
            "targetAccountId": ORG_2,
            "atoVersion": 2,
            comments: "comments2"
        })
        const response = await submitTx(users.org1.AO, "GetATO", [ORG_2], [ORG_2], null)
        expect(response.memo).toEqual("memo2-1")
        expect(response.artifacts).toEqual("artifacts2-1")
        expect(response.feedback.length).toEqual(1)
        expect(response.feedback[0].atoVersion).toEqual(2)
        expect(response.feedback[0].accountId).toEqual(ORG_1)
        expect(response.feedback[0].comments).toEqual("comments2")
    })
});

describe('test history methods return a complete history', () => {
    let vote1Id: string, vote2Id: string;
    beforeAll(async () => {
        // vote org2 as authorized
        vote1Id = await completeVote(users, ORG_2, "AUTHORIZED")
        // vote 2 as unauthorized
        vote2Id = await completeVote(users, ORG_2, "UNAUTHORIZED")
    })


    test("test GetAccountHistory has full history for account", async () => {
        // check history
        const response = await submitTx(users.org1.AO, "GetAccountHistory", [ORG_2], [ORG_2], null)
        expect(response[0].account.status).toEqual('UNAUTHORIZED')
        expect(response[0].account.id).toEqual(ORG_2)
        expect(response[1].account.status).toEqual('AUTHORIZED')
        expect(response[1].account.id).toEqual(ORG_2)
    })

    test("test GetVoteHistory has full history of votes on Org2", async () => {
        const response = await submitTx(users.org1.AO, "GetVoteHistory", [ORG_2], [ORG_2], null)
        for (const vote of response) {
            if (vote.id == vote1Id) {
                expect(vote.statusChange).toEqual('AUTHORIZED')
                expect(vote.targetAccountId).toEqual(ORG_2)
                expect(vote.initiatingAccountId).toEqual(ORG_1)
                expect(vote.threshold).toEqual("MAJORITY")
            } else if (vote.id == vote2Id) {
                expect(vote.statusChange).toEqual('UNAUTHORIZED')
                expect(vote.targetAccountId).toEqual(ORG_2)
                expect(vote.initiatingAccountId).toEqual(ORG_1)
                expect(vote.threshold).toEqual("MAJORITY")
            }
        }
    })
});

describe('test invokeChaincode across channels works', () => {
    test("test Org2 GetAssets succeeds only when authorized", async () => {
        await completeVote(users, ORG_2, "AUTHORIZED")
        await submitTx(users.org2.TPOC, "GetAssets", [], [ORG_1], null)
        await completeVote(users, ORG_2, "UNAUTHORIZED")
        await submitTx(users.org2.TPOC, "GetAssets", [], [ORG_1], null, "unauthorized")
    });
});

describe('test object serialization for method returns', () => {
    test("test MOU object serialization", async () => {
        const response = await submitTx(users.org2.AO, "GetMOU", [], [ORG_2], null);
        expect(response.text).toBeDefined();
        expect(response.version).toBeDefined();
        expect(response.timestamp).toBeDefined();
    });
    test("test ATO object with Feedback object serialization", async () => {
        const response = await submitTx(users.org2.AO, "GetATO", [ORG_2], [ORG_2], null);
        expect(response.id).toBeDefined();
        expect(response.creationTimestamp).toBeDefined();
        expect(response.lastUpdatedTimestamp).toBeDefined();
        expect(response.version).toBeDefined();
        expect(response.memo).toBeDefined();
        expect(response.artifacts).toBeDefined();
        expect(response.feedback).toBeDefined();
        expect(response.feedback[0].atoVersion).toBeDefined();
        expect(response.feedback[0].accountId).toBeDefined();
        expect(response.feedback[0].comments).toBeDefined();
    });
    test("test Vote object serialization", async () => {
        let response = await submitTx(users.org2.AO, "GetVoteHistory", [ORG_2], [ORG_2], null);
        response = response[0];
        expect(response.id).toBeDefined();
        expect(response.initiatingAccountId).toBeDefined();
        expect(response.targetAccountId).toBeDefined();
        expect(response.statusChange).toBeDefined();
        expect(response.reason).toBeDefined();
        expect(response.threshold).toBeDefined();
        expect(response.voters).toBeDefined();
        expect(response.submittedVotes).toBeDefined();
        expect(response.result).toBeDefined();
    });
    test("test Account object serialization", async () => {
        const response = await submitTx(users.org2.AO, "GetAccount", [ORG_2], [ORG_2], null);
        expect(response.id).toBeDefined();
        expect(response.status).toBeDefined();
        expect(response.mouVersion).toBeDefined();
        expect(response.joined).toBeDefined();
    });
    test("test AccountHistory object serialization", async () => {
        let response = await submitTx(users.org1.AO, "GetAccountHistory", [ORG_2], [ORG_2], null)
        response = response[0];
        expect(response.txID).toBeDefined();
        expect(response.timestamp).toBeDefined();
        expect(response.account).toBeDefined();
        expect(response.account.id).toBeDefined();
        expect(response.account.status).toBeDefined();
        expect(response.account.mouVersion).toBeDefined();
        expect(response.account.joined).toBeDefined();
    });

    let assetId: string, orderId: string;
    beforeAll(async () => {
        let response = await submitTx(users.org1.SO, "AddAsset", [], [ORG_1], {
            "name": "asset1",
            "endDate": "2030-01-01 00:00:00",
            "licenses": [
                {
                    "id": "1",
                    "salt": "1"
                },
            ]
        });
        assetId = response.id;

        await completeVote(users, ORG_2, "AUTHORIZED")

        response = await submitTx(users.org2.TPOC, "GetQuote", [], [ORG_1],
            {
                "account": "Org2MSP",
                "assetId": assetId,
                "amount": 1,
                "duration": 1
            }
        );
        orderId = response.id;

        await submitTx(users.org1.ACQ, "SendQuote", [], [ORG_1],
            {
                "orderId": orderId,
                "price": "100",
                "account": "Org2MSP"
            }
        );

        await submitTx(users.org2.TPOC, "InitiateOrder", [], [ORG_1],
            {
                "orderId": orderId,
                "assetId": assetId,
                "amount": 1,
                "duration": 1,
                "account": "Org2MSP"
            }
        );

        await submitTx(users.org2.ACQ, "ApproveOrder", [], [ORG_1],
            {
                "orderId": orderId,
                "account": "Org2MSP"
            }
        );
    });

    test("test AssetResponse[] serialization", async () => {
        let response = await submitTx(users.org1.SO, "GetAssets", [], [ORG_1], {});
        response = response[0];
        expect(response.numAvailable).toBeDefined();
        expect(response.name).toBeDefined();
        expect(response.id).toBeDefined();
        expect(response.endDate).toBeDefined();
        expect(response.startDate).toBeDefined();
    });

    test("test AssetDetailResponse serialization", async () => {
        const response = await submitTx(users.org1.SO, "GetAsset", [], [ORG_1], {
            "assetId": assetId
        });
        expect(response.numAvailable).toBeDefined();
        expect(response.name).toBeDefined();
        expect(response.id).toBeDefined();
        expect(response.endDate).toBeDefined();
        expect(response.startDate).toBeDefined();
        expect(response.allocatedLicenses).toBeDefined();
        expect(response.totalAmount).toBeDefined();
        expect(response.availableLicenses).toBeDefined();
    });

    test("test AssetDetailResponse w/o details serialization", async () => {
        // set org2 to authorized to use asset chaincode
        await completeVote(users, ORG_2, "AUTHORIZED")

        const response = await submitTx(users.org2.TPOC, "GetAsset", [], [ORG_1], {
            "assetId": assetId
        });
        expect(response.numAvailable).toBeDefined();
        expect(response.name).toBeDefined();
        expect(response.id).toBeDefined();
        expect(response.endDate).toBeDefined();
        expect(response.startDate).toBeDefined();
        expect(response.allocatedLicenses).not.toBeDefined();
        expect(response.totalAmount).not.toBeDefined();
        expect(response.availableLicenses).not.toBeDefined();
    });

    test("test IdResponse serialization", async () => {
        const response = await submitTx(users.org1.SO, "AddAsset", [], [ORG_1], {
            "name": "test_asset",
            "endDate": "2030-01-01 00:00:00",
            "licenses": [
                {
                    "id": "1",
                    "salt": "1"
                },
            ]
        });
        expect(response.id).toBeDefined();
    });

    test("test AllocateLicensesResponse serialization", async () => {
        const response = await submitTx(users.org1.ACQ, "GetLicensesToAllocateForOrder", [], [ORG_1],
            {
                "orderId": orderId,
                "account": "Org2MSP"
            }
        );

        expect(response.orderId).toBeDefined();
        expect(response.account).toBeDefined();
        expect(response.licenses).toBeDefined();
    });

    test("test LicensesRequest", async () => {
        const response = await submitTx(users.org1.ACQ, "AllocateLicenses", [], [ORG_1],
            {
                "orderId": orderId,
                "account": "Org2MSP",
                "licenses": ["1"]
            }
        );

        expect(response.account).toBeDefined();
        expect(response.assetId).toBeDefined();
        expect(response.orderId).toBeDefined();
        expect(response.expiration).toBeDefined();
        expect(response.licenses).toBeDefined();
    })

    test("test Order serialization", async () => {
        const response = await submitTx(users.org2.TPOC, "GetOrder", [], [ORG_1],
            {
                "orderId": orderId,
                "account": "Org2MSP"
            }
        );

        expect(response.id).toBeDefined();
        expect(response.account).toBeDefined();
        expect(response.status).toBeDefined();
        expect(response.initiationDate).toBeDefined();
        expect(response.approvalDate).toBeDefined();
        expect(response.allocatedDate).toBeDefined();
        expect(response.latestRenewalDate).toBeDefined();
        expect(response.assetId).toBeDefined();
        expect(response.amount).toBeDefined();
        expect(response.duration).toBeDefined();
        expect(response.price).toBeDefined();
        expect(response.expiration).toBeDefined();
        expect(response.licenses).toBeDefined();
    })

    test("test Order[] serialization", async () => {
        let response = await submitTx(users.org2.TPOC, "GetOrdersByAccount", [], [ORG_1],
            {
                "account": "Org2MSP"
            }
        );
        response = response[0]
        expect(response.id).toBeDefined();
        expect(response.account).toBeDefined();
        expect(response.status).toBeDefined();
        expect(response.initiationDate).toBeDefined();
        expect(response.approvalDate).toBeDefined();
        expect(response.allocatedDate).toBeDefined();
        expect(response.latestRenewalDate).toBeDefined();
        expect(response.assetId).toBeDefined();
        expect(response.amount).toBeDefined();
        expect(response.duration).toBeDefined();
        expect(response.price).toBeDefined();
        expect(response.expiration).toBeDefined();
        expect(response.licenses).toBeDefined();
    })
});


