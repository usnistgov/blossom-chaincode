import {buildTestUsers, OrgUsers} from './testUtil';
import {buildCAClient, enrollAdmin, registerAndEnrollUser} from './utils/CAUtil';

async function createUsers() {
    const users = await buildTestUsers();

    try {
        await createOrgUsers(users.org1);
        await createOrgUsers(users.org2);
        await createOrgUsers(users.org3);
    } catch (e) {
        console.error(`******** FAILED to setup application: ${e}`);
    }
}

async function createOrgUsers(orgUsers: OrgUsers) {
    const org = orgUsers.AO.org;

    const caClient = buildCAClient(org.ccp, org.caHostName);

    // enroll admin user
    await enrollAdmin(caClient, org.wallet, org.id);

    // enroll test users
    let user = orgUsers.AO;
    await registerAndEnrollUser(caClient, org.wallet, org.id, user.name, [
        {name: 'blossom.role', value: user.role, ecert: true},
    ]);

    user = orgUsers.ACQ;
    await registerAndEnrollUser(caClient, org.wallet, org.id, user.name, [
        {name: 'blossom.role', value: user.role, ecert: true},
    ]);

    user = orgUsers.TPOC;
    await registerAndEnrollUser(caClient, org.wallet, org.id, user.name, [
        {name: 'blossom.role', value: user.role, ecert: true},
    ]);

    user = orgUsers.LO;
    if (user !== null) {
        await registerAndEnrollUser(caClient, org.wallet, org.id, user.name, [
            {name: 'blossom.role', value: user.role, ecert: true},
        ]);
    }
}

// create user identities in wallets
createUsers()
    .then(() => {
        console.log('users created');
    });
