build-auth:
	rm -r -f ./build && \
	rm -r -f ./libs && \
	docker build -t gradle-build-authorization-chaincode-image . && \
	mkdir -p ./build && \
	mkdir -p ./libs && \
	docker run -t -d --name gradle-build-authorization-chaincode gradle-build-authorization-chaincode-image && \
	docker exec gradle-build-authorization-chaincode gradle getDeps --build-file build-libs.gradle
	docker exec gradle-build-authorization-chaincode gradle shadowJar
	docker cp gradle-build-authorization-chaincode:/authorization/libs/. ./libs && \
	docker cp gradle-build-authorization-chaincode:/authorization/build/. ./build && \
	docker stop gradle-build-authorization-chaincode && \
	docker rm gradle-build-authorization-chaincode && \
	docker rmi gradle-build-authorization-chaincode-image

build-libs:
	rm -r -f ./libs && \
	docker build -t gradle-build-authorization-chaincode-image . && \
	mkdir -p ./libs && \
	docker run -t -d --name gradle-build-authorization-chaincode gradle-build-authorization-chaincode-image && \
	docker exec gradle-build-authorization-chaincode gradle getDeps --build-file build-libs.gradle
	docker cp gradle-build-authorization-chaincode:/authorization/libs/. ./libs && \
	docker stop gradle-build-authorization-chaincode && \
	docker rm gradle-build-authorization-chaincode && \
	docker rmi gradle-build-authorization-chaincode-image