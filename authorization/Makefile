
.PHONY: help
# Run "make" or "make help" to get a list of user targets
# Adapted from https://marmelab.com/blog/2016/02/29/auto-documented-makefile.html
help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?##.*$$' $(MAKEFILE_LIST) | awk 'BEGIN { \
	 FS = ":.*?## "; \
	 printf "\033[1m%-30s\033[0m %s\n", "TARGET", "DESCRIPTION" \
	} \
	{ printf "\033[32m%-30s\033[0m %s\n", $$1, $$2 }'



build-auth: ## Build Authorization Chain-Code
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

build-libs: ## Build Libraries and Dependencies for Chain-Code
	rm -r -f ./libs && \
	docker build -t gradle-build-authorization-chaincode-image . && \
	mkdir -p ./libs && \
	docker run -t -d --name gradle-build-authorization-chaincode gradle-build-authorization-chaincode-image && \
	docker exec gradle-build-authorization-chaincode gradle getDeps --build-file build-libs.gradle
	docker cp gradle-build-authorization-chaincode:/authorization/libs/. ./libs && \
	docker stop gradle-build-authorization-chaincode && \
	docker rm gradle-build-authorization-chaincode && \
	docker rmi gradle-build-authorization-chaincode-image