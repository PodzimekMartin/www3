.PHONY: test run docker-up smoke k8s-staging k8s-production

test:
	mvn verify

run:
	mvn spring-boot:run

docker-up:
	docker compose up --build

smoke:
	BASE_URL=$${BASE_URL:-http://localhost:8080} ./scripts/smoke-test.sh

k8s-staging:
	kubectl apply -k k8s/staging

k8s-production:
	kubectl apply -k k8s/production
