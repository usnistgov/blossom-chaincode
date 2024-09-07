build-asset:
	rm -r -f ./build && \
	rm -r -f ./libs && \
	docker build -t gradle-build-asset-chaincode-image . && \
	mkdir -p ./build && \
	mkdir -p ./libs && \
	docker run -t -d --name gradle-build-asset-chaincode gradle-build-asset-chaincode-image && \
	docker exec gradle-build-asset-chaincode gradle getDeps --build-file build-libs.gradle
	docker exec gradle-build-asset-chaincode gradle shadowJar
	docker cp gradle-build-asset-chaincode:/asset/libs/. ./libs && \
	docker cp gradle-build-asset-chaincode:/asset/build/. ./build && \
	docker stop gradle-build-asset-chaincode && \
	docker rm gradle-build-asset-chaincode && \
	docker rmi gradle-build-asset-chaincode-image

build-libs:
	rm -r -f ./libs && \
	docker build -t gradle-build-asset-chaincode-image . && \
	mkdir -p ./libs && \
	docker run -t -d --name gradle-build-asset-chaincode gradle-build-asset-chaincode-image && \
	docker exec gradle-build-asset-chaincode gradle getDeps --build-file build-libs.gradle
	docker cp gradle-build-asset-chaincode:/asset/libs/. ./libs && \
	docker stop gradle-build-asset-chaincode && \
	docker rm gradle-build-asset-chaincode && \
	docker rmi gradle-build-asset-chaincode-image