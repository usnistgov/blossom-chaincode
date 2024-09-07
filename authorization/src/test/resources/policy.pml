set resource operations [
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
]

create pc "ADMIN_MSP_PC"
create oa ADMINMSP in ["ADMIN_MSP_PC"]

create pc "RBAC"
create ua "Authorizing Official" in ["RBAC"]
create ua "Blossom Admin" in ["Authorizing Official"]

create oa "RBAC/blossom_target" in ["RBAC"]
create oa "blossom_target" in ["RBAC/blossom_target"]
create oa "RBAC/accounts" in ["RBAC"]
create oa "RBAC/votes" in ["RBAC"]

associate "Blossom Admin"         and "RBAC/blossom_target"   with ["bootstrap", "update_mou"]
// this association is with the blossom target because these permissions are never lost
// association directly with the OA applies the association to all policy classes the OA is contained in
associate "Authorizing Official"  and "blossom_target"        with ["get_mou", "sign_mou", "join"]
associate "Authorizing Official"  and "RBAC/votes"            with ["vote", "certify_vote", "initiate_vote"]
associate "Authorizing Official"  and "RBAC/accounts"         with ["initiate_vote", "submit_feedback", "read_ato", "join"]

create pc "Status"
create ua "authorized" in ["Status"]
create ua "pending" in ["Status"]
create ua "unauthorized" in ["pending"]

create oa "Status/blossom_target" in ["Status"]
assign "blossom_target" to ["Status/blossom_target"]
create oa "Status/accounts" in ["Status"]
create oa "Status/votes" in ["Status"]

associate "authorized"    and "Status/blossom_target" with ["*r"]
associate "authorized"    and "Status/accounts"       with ["*r"]
associate "authorized"    and "Status/votes"          with ["*r"]
associate "pending"       and "Status/accounts"       with ["submit_feedback", "read_ato", "write_ato"]

create pc "Votes"
create oa "all votes" in ["Votes"]

associate "Blossom Admin"         and "all votes" with ["certify_vote"]
associate "Authorizing Official"  and "all votes" with ["vote", "initiate_vote"]

// bootstrap adminmsp account
signMOU(ADMINMSP)
updateAccountStatus(ADMINMSP, "AUTHORIZED")

operation initiateVote(string initiator, string targetMember) {
    check "initiate_vote" on accountObjectNodeName(targetMember)
} {
    initiatorUsers  := accountUsersNodeName(initiator)
    voteua          := voteInitiatorAttr(targetMember)

    assign initiatorUsers to [voteua]
}

operation certifyVote(string targetMember, string status, bool passed) {
    check "certify_vote" on voteObj(targetMember)
} {
    voteua  := voteInitiatorAttr(targetMember)

    foreach child in getAdjacentDescendants(voteua) {
        deassign child from [voteua]
    }

    if passed {
        updateAccountStatus(targetMember, status)
    }
}

operation signMOU(string accountId) {
    check "sign_mou" on "blossom_target"
} {
    accountUA := accountUsersNodeName(accountId)
    accountOA := accountAttributeNodeName(accountId)
    accountO  := accountObjectNodeName(accountId)

    if nodeExists(accountUA) {
        return
    }

    // create account ua, container, and object
    create ua accountUA in ["pending"]
    create oa accountOA in ["RBAC/accounts", "Status/accounts"]
    create o accountO in [accountOA]

    associate accountUA and accountOA with ["write_ato", "submit_feedback", "read_ato"]

    // create vote attr and object for this account
    voteua  := voteInitiatorAttr(accountId)
    voteoa  := voteObjAttr(accountId)
    voteobj := voteObj(accountId)

    create ua voteua  in ["Votes"]
    create oa voteoa  in ["RBAC/votes", "Status/votes", "all votes"]
    create o  voteobj in [voteoa]

    associate accountUA and voteoa with ["vote"]
    associate voteua    and voteoa with ["certify_vote"]

    createAccountDeny(accountUA, accountOA)
    createInitiateVoteDeny(accountUA, accountOA)
}

operation updateAccountStatus(string accountId, string status) {
    accountUA := accountUsersNodeName(accountId)
    accountOA := accountAttributeNodeName(accountId)

    if status == "AUTHORIZED" {
        assign      accountUA to    ["authorized"]
        deassign    accountUA from  ["pending", "unauthorized"]

        // delete initiate vote prohibition that prevents the account from initiating votes on
        // other accounts when status is pending
        delete prohibition accountDenyLabel(accountUA)
    } else if status == "PENDING" {
        assign      accountUA to    ["pending"]
        deassign    accountUA from  ["authorized", "unauthorized"]

        delete prohibition accountDenyLabel(accountUA)
        createAccountDeny(accountUA, accountOA)
    } else {
        assign      accountUA to    ["unauthorized"]
        deassign    accountUA from  ["authorized", "pending"]

        delete prohibition accountDenyLabel(accountUA)
        createAccountDeny(accountUA, accountOA)
    }

    // if there are no authorized accounts, grant the ADMINMSP "initiate_vote" on themselves
    accountUA = accountUsersNodeName(ADMINMSP)
    if noAuthorizedAccounts() {
        associate accountUA and "Status/accounts" with ["initiate_vote"]
        delete prohibition initiateVoteDenyLabel(accountUA)
    } else {
        accountOA = accountAttributeNodeName(ADMINMSP)

        dissociate accountUA and "Status/accounts"
        delete prohibition initiateVoteDenyLabel(accountUA)
        createInitiateVoteDeny(accountUA, accountOA)
    }
}

operation noAuthorizedAccounts() bool {
    foreach x in getAdjacentAscendants("authorized") {
        return false
    }

    return true
}

// account deny happens only when not authorized
operation createAccountDeny(string accountUA, string accountOA) {
    create prohibition accountDenyLabel(accountUA)
    deny user attribute accountUA
    access rights ["submit_feedback", "read_ato"]
    on intersection of ["Status/accounts", !accountOA]
}

// initiate vote deny happens all the time except for the ADMINMSP when there are no authorized users
operation createInitiateVoteDeny(string accountUA, string accountOA) {
    create prohibition initiateVoteDenyLabel(accountUA)
    deny user attribute accountUA
    access rights ["initiate_vote"]
    on intersection of ["Status/accounts", accountOA]
}

operation voteObjAttr(string account) string {
    return account + " vote attr"
}

operation voteObj(string account) string {
    return account + " vote"
}

operation voteInitiatorAttr(string account) string {
    return account + " initiator"
}

operation accountUsersNodeName(string accountId) string {
    return accountId + " users"
}

operation accountAttributeNodeName(string account) string {
    return account + " account"
}

operation accountObjectNodeName(string account) string {
    return account + " target"
}

operation accountDenyLabel(string accountUA) string {
    return "deny " + accountUA + " submit_feedback, read_ato except on self"
}

operation initiateVoteDenyLabel(string accountUA) string {
    return "deny " + accountUA + " initiate_vote on self"
}