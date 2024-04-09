# Blossom Smart Contracts

This package contains the code for the Blossom Smart Contracts. There are two Smart Contracts: [Authorization](./authorization) and [Asset](./asset).

## User Registration

Below are examples of registering a user with Blossom roles as attributes.

_Note: The users MSPID is determined by the Fabric CA the identity is registered with._

- Using the node sdk

  ```typescript  
  // create an organization system owner  
  const secret = await caClient.register({     
	affiliation: '',     
	enrollmentID: 'org1_sys_owner', 
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
