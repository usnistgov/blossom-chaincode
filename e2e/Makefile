.PHONY: fabric-down
fabric-down:
	rm -r -f src/wallet && \
	cd network && \
	./network.sh down

.PHONY: fabric-up
fabric-up:
	cd network && \
	  ./network.sh up createChannel -c authorization -ca -i 2.2.0 && \
      ./network.sh deployCC -ccn authorization -ccp ../../authorization/build/libs/ -ccl java -c authorization && \
	  ./network.sh createChannel -c asset && \
      ./network.sh deployCC -ccn asset -ccp ../../asset/build/libs/ -ccl java -c asset && \
	  ts-node ../src/createWallets.ts

# make update-chaincode CC_NAME=ngac CC_LANG=java VERSION=2 CHANNEL=authorization
.PHONY: update-auth-chaincode
update-auth-chaincode:
	cd network && \
    	./network.sh deployCC -ccn authorization -ccp ../../authorization/build/libs/ -ccl java -c authorization -ccv $(VERSION) -ccs $(VERSION)

.PHONY: update-asset-chaincode
update-asset-chaincode:
	cd network && \
    	./network.sh deployCC -ccn asset -ccp ../../asset/build/libs/ -ccl java -c asset -ccv $(VERSION) -ccs $(VERSION)
