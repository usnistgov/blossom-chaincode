import {buildTestUsers, completeVote, ORG_1, ORG_2, ORG_3, submitTx, Users} from './testUtil';

let users: Users;
beforeAll(async () => {
    users = await buildTestUsers();
});

describe('e2e',  () => {
    test('authorization', async () => {
        try {
            await submitTx(users.org1.AO, 'Bootstrap', [], [ORG_1], null);
        } catch (e) {
            // ignore
        }

        // set 2 and 3 to unauthorized, ignore errors if they don't exist yet
        try {
            await submitTx(users.org1.AO, 'CertifyOngoingVote');
        } catch (e) {
            // ignore
        }
        try {

            await completeVote(users, ORG_2, 'UNAUTHORIZED');
        } catch (e) {
            // ignore
        }
        try {
            await completeVote(users, ORG_3, 'UNAUTHORIZED');
        } catch (e) {
            // ignore
        }

        await submitTx(users.org1.AO, 'UpdateMOU', ['mou']);

        let response = await submitTx(users.org1.AO, 'GetMOU');
        expect(response.text).toEqual('mou');
        const version1 = response.version;

        await submitTx(users.org1.AO, 'UpdateMOU', ['mou2']);

        response = await submitTx(users.org1.AO, 'GetMOU');
        expect(response.text).toEqual('mou2');
        expect(response.version).toEqual(version1 + 1);
        const version2 = response.version;

        response = await submitTx(users.org1.AO, 'GetMOUHistory');
        expect(response.length).toBeGreaterThanOrEqual(2);
        expect(response[0].text).toEqual('mou2');
        expect(response[0].version).toEqual(version2);
        expect(response[1].text).toEqual('mou');
        expect(response[1].version).toEqual(version1);

        // sign and join as both 2 and 3
        await submitTx(users.org2.AO, 'SignMOU', [version2]);
        await submitTx(users.org3.AO, 'SignMOU', [version2]);

        // vote both as authorized
        // vote org2 authorized
        await submitTx(users.org1.AO, 'InitiateVote', [ORG_2, 'AUTHORIZED', 'reason']);
        await submitTx(users.org1.AO, 'Vote', [true]);
        await submitTx(users.org2.AO, 'Vote', [true]);

        response = await submitTx(users.org1.AO, 'GetOngoingVote');
        expect(response.initiatingAccountId).toEqual(ORG_1);
        expect(response.targetAccountId).toEqual(ORG_2);
        expect(response.statusChange).toEqual('AUTHORIZED');
        expect(response.reason).toEqual('reason');
        expect(response.threshold).toEqual('MAJORITY');
        expect(response.voters.length).toEqual(2);
        expect(response.voters).toEqual(expect.arrayContaining([ORG_1, ORG_2]));
        expect(response.submittedVotes).toEqual({Org1MSP: true, Org2MSP: true});
        expect(response.result).toEqual('ONGOING');
        await submitTx(users.org1.AO, 'CertifyOngoingVote');

        // vote org3 authorized
        await submitTx(users.org1.AO, 'InitiateVote', [ORG_3, 'AUTHORIZED', 'reason']);
        await submitTx(users.org1.AO, 'Vote', [true]);
        await submitTx(users.org2.AO, 'Vote', [true]);
        await submitTx(users.org3.AO, 'Vote', [true]);
        response = await submitTx(users.org1.AO, 'GetOngoingVote');
        expect(response.initiatingAccountId).toEqual(ORG_1);
        expect(response.targetAccountId).toEqual(ORG_3);
        expect(response.statusChange).toEqual('AUTHORIZED');
        expect(response.reason).toEqual('reason');
        expect(response.threshold).toEqual('MAJORITY');
        expect(response.voters).toEqual(expect.arrayContaining([ORG_1, ORG_2, ORG_3]));
        expect(response.submittedVotes).toEqual({Org1MSP: true, Org2MSP: true, Org3MSP: true});
        expect(response.result).toEqual('ONGOING');
        await submitTx(users.org1.AO, 'CertifyOngoingVote');

        // ignore error if already joined
        try {
            await submitTx(users.org2.AO, 'Join');
        } catch (e) {
            expect(e.toString()).toContain('already joined');
        }

        try {
            await submitTx(users.org3.AO, 'Join');
        } catch (e) {
            expect(e.toString()).toContain('already joined');
        }

        response = await submitTx(users.org1.AO, 'GetAccount', [ORG_2]);
        expect(response.mouVersion).toEqual(version2);
        expect(response.joined).toEqual(true);
        expect(response.status).toEqual('AUTHORIZED');
        response = await submitTx(users.org1.AO, 'GetAccount', [ORG_3]);
        expect(response.mouVersion).toEqual(version2);
        expect(response.joined).toEqual(true);
        expect(response.status).toEqual('AUTHORIZED');

        response = await submitTx(users.org1.AO, 'GetAccounts');
        expect(response.length).toEqual(3);
        for (const a of response) {
            expect(a.joined).toEqual(true);
            expect(a.status).toEqual('AUTHORIZED');
        }

        // org 2 ATO
        await submitTx(users.org2.AO, 'CreateATO', [], [ORG_2], {
            memo: 'memo2',
            artifacts: 'artifacts2',
        });
        response = await submitTx(users.org2.AO, 'GetATO', [ORG_2], [ORG_2], null);
        expect(response.memo).toEqual('memo2');
        expect(response.artifacts).toEqual('artifacts2');
        expect(response.version).toEqual(1);

        await submitTx(users.org2.AO, 'UpdateATO', [], [ORG_2], {
            memo: 'memo2-1',
            artifacts: 'artifacts2-1',
        });
        response = await submitTx(users.org2.AO, 'GetATO', [ORG_2], [ORG_2]);
        expect(response.memo).toEqual('memo2-1');
        expect(response.artifacts).toEqual('artifacts2-1');
        expect(response.version).toEqual(2);

        await submitTx(users.org1.AO, 'SubmitFeedback', [], [ORG_2], {
            targetAccountId: ORG_2,
            atoVersion: 2,
            comments: 'comments2',
        });
        response = await submitTx(users.org1.AO, 'GetATO', [ORG_2], [ORG_2]);
        expect(response.memo).toEqual('memo2-1');
        expect(response.artifacts).toEqual('artifacts2-1');
        expect(response.feedback.length).toEqual(1);
        expect(response.feedback[0].atoVersion).toEqual(2);
        expect(response.feedback[0].accountId).toEqual(ORG_1);
        expect(response.feedback[0].comments).toEqual('comments2');

        // org 3 ato
        await submitTx(users.org3.AO, 'CreateATO', [], [ORG_3], {
            memo: 'memo3',
            artifacts: 'artifacts3',
        });
        response = await submitTx(users.org3.AO, 'GetATO', [ORG_3], [ORG_3]);
        expect(response.memo).toEqual('memo3');
        expect(response.artifacts).toEqual('artifacts3');
        expect(response.version).toEqual(1);

        await submitTx(users.org3.AO, 'UpdateATO', [], [ORG_3], {
            memo: 'memo3-1',
            artifacts: 'artifacts3-1',
        });
        response = await submitTx(users.org3.AO, 'GetATO', [ORG_3], [ORG_3]);
        expect(response.memo).toEqual('memo3-1');
        expect(response.artifacts).toEqual('artifacts3-1');
        expect(response.version).toEqual(2);

        await submitTx(users.org1.AO, 'SubmitFeedback', [], [ORG_3], {
            targetAccountId: ORG_3,
            atoVersion: 2,
            comments: 'comments3',
        });
        response = await submitTx(users.org1.AO, 'GetATO', [ORG_3], [ORG_3]);
        expect(response.memo).toEqual('memo3-1');
        expect(response.artifacts).toEqual('artifacts3-1');
        expect(response.feedback.length).toEqual(1);
        expect(response.feedback[0].atoVersion).toEqual(2);
        expect(response.feedback[0].accountId).toEqual(ORG_1);
        expect(response.feedback[0].comments).toEqual('comments3');

        response = await submitTx(users.org3.AO, 'GetAllRoles', [], [ORG_3], {}, '', 'authorization');
        expect(response.length).toEqual(1);
        expect(response[0]).toEqual('Authorizing Official');

        response = await submitTx(users.org3.AO, 'GetAllPrivileges', [], [ORG_3], {}, '', 'authorization');
        expect(response.length).toEqual(11);
        expect(response).toEqual(['bootstrap',
            'update_mou',
            'get_mou',
            'sign_mou',
            'join',
            'write_ato',
            'read_ato',
            'submit_feedback',
            'initiate_vote',
            'vote',
            'certify_vote']);
    });

    test('asset', async () => {
        let response = await submitTx(users.org1.LO, 'AddAsset', [], [ORG_1], {
            name: 'asset1',
            endDate: '2030-01-01 00:00:00',
            licenses: [
                {
                    id: '1',
                    salt: '1',
                },
            ],
        });
        const assetId = response.id;

        response = await submitTx(users.org1.LO, 'GetAsset', [], [ORG_1], {
            assetId,
        });

        expect(response.numAvailable).toEqual(1);
        expect(response.name).toEqual('asset1');
        expect(response.id).toEqual(assetId);
        expect(response.endDate).toEqual('2030-01-01 00:00:00');
        expect(response.startDate).toBeDefined();
        expect(response.allocatedLicenses).toBeDefined();
        expect(response.totalAmount).toEqual(1);
        expect(response.availableLicenses).toEqual(['1']);

        await submitTx(users.org1.LO, 'AddLicenses', [], [ORG_1], {
            assetId,
            licenses: [
                {
                    id: '2',
                    salt: '2',
                },
                {
                    id: '3',
                    salt: '3',
                },
                {
                    id: '4',
                    salt: '4',
                },
                {
                    id: '5',
                    salt: '5',
                },
            ],
        });

        response = await submitTx(users.org1.LO, 'GetAsset', [], [ORG_1], {
            assetId,
        });
        expect(response.numAvailable).toEqual(5);
        expect(response.name).toEqual('asset1');
        expect(response.id).toEqual(assetId);
        expect(response.endDate).toEqual('2030-01-01 00:00:00');
        expect(response.startDate).toBeDefined();
        expect(response.allocatedLicenses).toBeDefined();
        expect(response.totalAmount).toEqual(5);
        expect(response.availableLicenses).toEqual(['1', '2', '3', '4', '5']);

        await submitTx(users.org1.LO, 'RemoveLicenses', [], [ORG_1], {
            assetId,
            licenses: ['5'],
        });

        response = await submitTx(users.org1.LO, 'GetAsset', [], [ORG_1], {
            assetId,
        });
        expect(response.numAvailable).toEqual(4);
        expect(response.name).toEqual('asset1');
        expect(response.id).toEqual(assetId);
        expect(response.endDate).toEqual('2030-01-01 00:00:00');
        expect(response.startDate).toBeDefined();
        expect(response.allocatedLicenses).toBeDefined();
        expect(response.totalAmount).toEqual(4);
        expect(response.availableLicenses).toEqual(['1', '2', '3', '4']);

        await submitTx(users.org1.LO, 'UpdateEndDate', [], [ORG_1], {
            assetId,
            newEndDate: '2040-01-01 00:00:00',
        });

        response = await submitTx(users.org1.LO, 'GetAsset', [], [ORG_1], {
            assetId,
        });
        expect(response.numAvailable).toEqual(4);
        expect(response.name).toEqual('asset1');
        expect(response.id).toEqual(assetId);
        expect(response.endDate).toEqual('2040-01-01 00:00:00');
        expect(response.startDate).toBeDefined();
        expect(response.allocatedLicenses).toBeDefined();
        expect(response.totalAmount).toEqual(4);
        expect(response.availableLicenses).toEqual(['1', '2', '3', '4']);

        // order
        response = await submitTx(users.org2.TPOC, 'GetQuote', [], [ORG_1],
            {
                account: 'Org2MSP',
                assetId,
                amount: 2,
                duration: 1,
            },
        );
        const orderId = response.id;
        response = await submitTx(users.org1.ACQ, 'GetOrder', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });
        expect(response.account).toEqual(ORG_2);
        expect(response.status).toEqual('QUOTE_REQUESTED');
        expect(response.assetId).toEqual(assetId);
        expect(response.amount).toEqual(2);
        expect(response.duration).toEqual(1);

        await submitTx(users.org1.ACQ, 'SendQuote', [], [ORG_1],
            {
                orderId,
                price: '100',
                account: 'Org2MSP',
            },
        );
        response = await submitTx(users.org1.ACQ, 'GetOrder', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });
        expect(response.status).toEqual('QUOTE_RECEIVED');
        expect(response.price).toEqual(100);

        await submitTx(users.org2.TPOC, 'InitiateOrder', [], [ORG_1],
            {
                orderId,
                account: 'Org2MSP',
            },
        );
        response = await submitTx(users.org1.ACQ, 'GetOrder', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });
        expect(response.status).toEqual('INITIATED');

        await submitTx(users.org2.ACQ, 'ApproveOrder', [], [ORG_1],
            {
                orderId,
                account: 'Org2MSP',
            },
        );
        response = await submitTx(users.org1.ACQ, 'GetOrder', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });
        expect(response.status).toEqual('APPROVED');

        const allocatedForOrder = await submitTx(users.org1.ACQ, 'GetLicensesToAllocateForOrder', [], [ORG_1],
            {
                orderId,
                account: 'Org2MSP',
            },
        );
        expect(allocatedForOrder.orderId).toEqual(orderId);
        expect(allocatedForOrder.account).toEqual(ORG_2);
        expect(allocatedForOrder.licenses).toEqual(['1', '2']);

        let allocated = await submitTx(users.org1.ACQ, 'AllocateLicenses', [], [ORG_1],
            allocatedForOrder,
        );
        response = await submitTx(users.org1.ACQ, 'GetOrder', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });
        expect(response.status).toEqual('ALLOCATED');

        response = await submitTx(users.org1.ACQ, 'GetAllocateRequestForOrder', [], [ORG_1],
            {orderId},
        );
        expect(response).toEqual(allocated);
        await submitTx(users.org1.ACQ, 'SendLicenses', [], [ORG_1, ORG_2],
            allocated,
        );

        response = await submitTx(users.org2.TPOC, 'GetAvailableLicensesForOrder', [], [ORG_2],
            {orderId, account: ORG_2},
        );
        expect(response).toEqual(['1', '2']);
        response = await submitTx(users.org2.ACQ, 'GetAvailableLicensesForOrder', [], [ORG_2],
            {orderId, account: ORG_2},
        );
        expect(response).toEqual(['1', '2']);

        await submitTx(users.org2.TPOC, 'GetQuote', [], [ORG_1],
            {
                orderId,
                account: 'Org2MSP',
                assetId,
                amount: 2,
                duration: 1,
            },
        );
        response = await submitTx(users.org1.ACQ, 'GetOrder', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });
        expect(response.account).toEqual(ORG_2);
        expect(response.status).toEqual('RENEWAL_QUOTE_REQUESTED');
        expect(response.assetId).toEqual(assetId);
        expect(response.amount).toEqual(2);
        expect(response.duration).toEqual(1);
        expect(response.price).toEqual(100);

        await submitTx(users.org1.ACQ, 'SendQuote', [], [ORG_1],
            {
                orderId,
                price: '100',
                account: 'Org2MSP',
            },
        );
        response = await submitTx(users.org1.ACQ, 'GetOrder', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });
        expect(response.status).toEqual('RENEWAL_QUOTE_RECEIVED');

        await submitTx(users.org2.TPOC, 'InitiateOrder', [], [ORG_1],
            {
                orderId,
                assetId,
                amount: 2,
                duration: 1,
                account: 'Org2MSP',
            },
        );
        response = await submitTx(users.org1.ACQ, 'GetOrder', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });
        expect(response.status).toEqual('RENEWAL_INITIATED');

        await submitTx(users.org2.ACQ, 'ApproveOrder', [], [ORG_1],
            {
                orderId,
                assetId,
                amount: 2,
                duration: 1,
                account: 'Org2MSP',
            },
        );
        response = await submitTx(users.org1.ACQ, 'GetOrder', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });
        expect(response.status).toEqual('RENEWAL_APPROVED');
        let exp = response.expiration;

        allocated = await submitTx(users.org1.ACQ, 'AllocateLicenses', [], [ORG_1],
            allocatedForOrder,
        );
        response = await submitTx(users.org1.ACQ, 'GetAllocateRequestForOrder', [], [ORG_1], {
            orderId,
        });
        expect(response).toEqual(allocated);

        response = await submitTx(users.org1.ACQ, 'GetOrder', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });
        expect(response.status).toEqual('ALLOCATED');
        expect(response.licenses).toEqual(['1', '2']);
        expect(response.expiration).not.toEqual(exp);
        exp = response.expiration;

        await submitTx(users.org1.ACQ, 'SendLicenses', [], [ORG_1, ORG_2],
            allocated,
        );

        response = await submitTx(users.org2.TPOC, 'GetAvailableLicensesForOrder', [], [ORG_2],
            {orderId, account: ORG_2},
        );
        expect(response).toEqual(['1', '2']);
        response = await submitTx(users.org2.ACQ, 'GetAvailableLicensesForOrder', [], [ORG_2],
            {orderId, account: ORG_2},
        );
        expect(response).toEqual(['1', '2']);

        // check GetAsset
        response = await submitTx(users.org1.ACQ, 'GetAsset', [], [ORG_1], {
            assetId,
        });
        expect(response.availableLicenses).toEqual(expect.arrayContaining(['3', '4']));
        expect(response.allocatedLicenses[ORG_2][orderId]).toEqual(expect.arrayContaining([
            {
                expiration: exp,
                licenseId: '1',
            },
            {
                expiration: exp,
                licenseId: '2',
            },
        ]));

        // deny order
        response = await submitTx(users.org2.TPOC, 'GetQuote', [], [ORG_1],
            {
                orderId,
                account: 'Org2MSP',
                assetId,
                amount: 2,
                duration: 1,
            },
        );
        response = await submitTx(users.org1.ACQ, 'GetOrder', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });
        expect(response.account).toEqual(ORG_2);
        expect(response.status).toEqual('RENEWAL_QUOTE_REQUESTED');
        expect(response.assetId).toEqual(assetId);
        expect(response.amount).toEqual(2);
        expect(response.duration).toEqual(1);
        expect(response.price).toEqual(100);

        await submitTx(users.org1.ACQ, 'SendQuote', [], [ORG_1],
            {
                orderId,
                price: '100',
                account: 'Org2MSP',
            },
        );
        response = await submitTx(users.org1.ACQ, 'GetOrder', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });
        expect(response.status).toEqual('RENEWAL_QUOTE_RECEIVED');

        await submitTx(users.org2.TPOC, 'InitiateOrder', [], [ORG_1],
            {
                orderId,
                assetId,
                amount: 2,
                duration: 1,
                account: 'Org2MSP',
            },
        );
        response = await submitTx(users.org1.ACQ, 'GetOrder', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });
        expect(response.status).toEqual('RENEWAL_INITIATED');

        await submitTx(users.org2.ACQ, 'DenyOrder', [], [ORG_1],
            {
                orderId,
                assetId,
                amount: 2,
                duration: 1,
                account: 'Org2MSP',
            },
        );
        response = await submitTx(users.org1.ACQ, 'GetOrder', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });
        expect(response.status).toEqual('RENEWAL_DENIED');

        // initiate return
        const returned = {
            orderId,
            assetId,
            account: 'Org2MSP',
            licenses: [
                '1',
            ],
            expiration: allocated.expiration,
        };
        await submitTx(users.org2.TPOC, 'InitiateReturn', [], [ORG_1, ORG_2],
            returned,
        );
        response = await submitTx(users.org1.ACQ, 'GetInitiatedReturnForOrder', [], [ORG_1],
            {orderId},
        );
        expect(response).toEqual(returned);

        await submitTx(users.org1.ACQ, 'DeallocateLicensesFromAccount', [], [ORG_2],
            response,
        );
        await submitTx(users.org1.ACQ, 'DeallocateLicensesFromSP', [], [ORG_1],
            {orderId, account: ORG_2},
        );

        response = await submitTx(users.org1.ACQ, 'GetAsset', [], [ORG_1], {
            assetId,
        });
        expect(response.availableLicenses).toEqual(expect.arrayContaining(['1', '3', '4']));
        expect(response.allocatedLicenses[ORG_2][orderId]).toEqual(expect.arrayContaining([
            {
                expiration: exp,
                licenseId: '2',
            },
        ]));

        response = await submitTx(users.org1.ACQ, 'GetOrder', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });
        expect(response.licenses).toEqual(['2']);
        const order = response;

        response = await submitTx(users.org1.ACQ, 'GetOrdersByAccount', [], [ORG_1], {
            orderId,
            account: ORG_2,
        });

        for (const o of response) {
            // ignore other orders
            if (o.id !== orderId) {
                continue;
            }

            expect(o).toEqual(order);
        }

        response = await submitTx(users.org1.ACQ, 'GetOrdersByAccountAndAsset', [], [ORG_1], {
            assetId,
            account: ORG_2,
        });
        for (const o of response) {
            // ignore other orders
            if (o.id !== orderId) {
                continue;
            }

            expect(o).toEqual(order);
        }

        response = await submitTx(users.org1.ACQ, 'GetOrdersByAsset', [], [ORG_1], {
            assetId,
        });
        for (const o of response) {
            // ignore other orders
            if (o.id !== orderId) {
                continue;
            }

            expect(o).toEqual(order);
        }

        const swid = {
            account: ORG_2,
            primaryTag: '<primaryTag></primaryTag>',
            xml: '<xml></xml>',
            orderId,
            licenseId: '2',
        };
        await submitTx(users.org2.TPOC, 'ReportSWID', [], [ORG_2], swid);
        response = await submitTx(users.org2.TPOC, 'GetSWID', [], [ORG_2], {
            account: ORG_2,
            orderId,
            licenseId: '2',
        });
        expect(response.primaryTag).toEqual(swid.primaryTag);
        expect(response.xml).toEqual(swid.xml);
        expect(response.orderId).toEqual(swid.orderId);
        expect(response.licenseId).toEqual(swid.licenseId);

        response = await submitTx(users.org2.TPOC, 'GetLicensesWithSWIDsForOrder', [], [ORG_2], {
            account: ORG_2,
            orderId,
        });
        expect(response).toEqual(['2']);

        await submitTx(users.org2.TPOC, 'DeleteSWID', [], [ORG_2], {
            account: ORG_2,
            orderId,
            licenseId: '2',
        });

        try {
            await submitTx(users.org2.TPOC, 'GetSWID', [], [ORG_2], {
                account: ORG_2,
                orderId,
                licenseId: '2',
            });
        } catch (e) {
            expect(true).toEqual(true);
        }

        response = await submitTx(users.org1.ACQ, 'GetLicenseTxHistory', [], [ORG_1], {
            assetId,
            licenseId: '2',
        });
        expect(response.length).toEqual(3);

        response = await submitTx(users.org3.AO, 'GetAllRoles', [], [ORG_3], {}, '', 'asset');
        expect(response.length).toEqual(3);
        expect(response).toEqual(['Acquisition Officer', 'Technical Point of Contact', 'License Owner']);

        response = await submitTx(users.org3.AO, 'GetAllPrivileges', [], [ORG_3], {}, '', 'asset');
        expect(response.length).toEqual(12);
        expect(response).toContain('write_asset');
        expect(response).toContain('read_assets');
        expect(response).toContain('read_asset_detail');
        expect(response).toContain('initiate_order');
        expect(response).toContain('approve_order');
        expect(response).toContain('deny_order');
        expect(response).toContain('read_order');
        expect(response).toContain('allocate_license');
        expect(response).toContain( 'return_license');
        expect(response).toContain( 'read_license');
        expect(response).toContain('write_swid');
        expect(response).toContain('read_swid');
    });
});
