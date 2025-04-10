# Blossom Smart Contracts

This package contains the code for the Blossom Smart Contracts. There are two Smart Contracts: [Authorization](./authorization) and [Asset](./asset).

## Chaincode Installation
Before installing chaincode, you must update the ADMINMSP constant in **both the asset and authorization source code**.
To do so, navigate to the following files:

- Asset: [./asset/src/main/java/ngac/MSPConfig.java#L7](./asset/src/main/java/ngac/MSPConfig.java#L7)
- Authorization: [./authorization/src/main/java/ngac/MSPConfig.java#L7](./authorization/src/main/java/ngac/MSPConfig.java#L7)

and update the value of the `ADMINMSP` constant in both files to the MSPID of the admin member of the blossom deployment. 
For example:

`public static final String ADMINMSP = "m-A1B2C3D4E5F6G6H7I8J9KALBMCND";`

## E2E Tests
From `./e2e`, run `make fabirc-up` to start a local Fabric test network. There are two tests:

- [./e2e/src/integration.test.ts](./e2e/src/integration.test.ts) - Tests that transient inputs are correctly deserialized 
by the chaincode and model objects are correctly serialized in function returns.
- [./e2e/src/e2e.test.ts](./e2e/src/e2e.test.ts) - Tests every function in both Authorization and Asset chaincodes at least
once.

## User Registration

Below are examples of registering a user with Blossom roles as attributes.

_Note: The users MSPID is determined by the Fabric CA the identity is registered with._

- Using the node sdk

  ```typescript  
  // create an organization Authorizing Official
  const secret = await caClient.register({     
	affiliation: '',     
	enrollmentID: 'org1_AO', 
	role: 'client',
	[         
		{name: 'blossom.role', value: 'Authorizing Offical', ecert: true}
	]  
  }, adminUser);  
  ```  
- Using the CLI

  ```shell
  ./fabric-ca-client register ... --id.attrs 'blossom.role=Authorizing Offical' ...  
  ```

## Endorsement with `--peerAddresses`
- 1 or more peers that have approved the chaincode to target for invoke.
- This is only needed if more than one peer is needed for endorsement or the invocation attempts to read data from another member's private data collections (implicit and explicit).
- If an org did not approve the chaincode, they will need to target a org that did or else an error will occur.
- If an org did approve the chaincode, they do not need to target another peer.
