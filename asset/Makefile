
.PHONY: help
# Run "make" or "make help" to get a list of user targets
# Adapted from https://marmelab.com/blog/2016/02/29/auto-documented-makefile.html
help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?##.*$$' $(MAKEFILE_LIST) | awk 'BEGIN { \
	 FS = ":.*?## "; \
	 printf "\033[1m%-30s\033[0m %s\n", "TARGET", "DESCRIPTION" \
	} \
	{ printf "\033[32m%-30s\033[0m %s\n", $$1, $$2 }'


build-asset: ## Build Assets Chain-Code
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


build-libs: ## Build Assets Chain-Code
	rm -r -f ./libs && \
	docker build -t gradle-build-asset-chaincode-image . && \
	mkdir -p ./libs && \
	docker run -t -d --name gradle-build-asset-chaincode gradle-build-asset-chaincode-image && \
	docker exec gradle-build-asset-chaincode gradle getDeps --build-file build-libs.gradle
	docker cp gradle-build-asset-chaincode:/asset/libs/. ./libs && \
	docker stop gradle-build-asset-chaincode && \
	docker rm gradle-build-asset-chaincode && \
	docker rmi gradle-build-asset-chaincode-image