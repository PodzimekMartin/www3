# Release strategie

## Verzovani

Aplikace pouziva semantic versioning. Maven verze a Docker tag jsou sladene, napr. `1.0.0`.

## Tagovani

```bash
git tag v1.0.0
git push origin v1.0.0
```

CI pri tagu vytvori Docker image s odpovidajicim semver tagem.

## Canary varianta

Pro canary release se v Kubernetes docasne nasadi druha Deployment varianta s nizsim poctem replik a novym image tagem. Service zustava stejna a provoz se deli podle poctu podu.

Postup:

1. Nasadit novy image do staging.
2. Provest smoke test `/actuator/health/readiness` a zakladni tok v UI/API.
3. Zvysit production image na novy tag u jedne repliky.
4. Sledovat HTTP chyby, latenci a restarty.
5. Po overeni rolloutnout vsechny production repliky.

## Rollback

```bash
kubectl -n course-production rollout undo deployment/course-reservations-production
kubectl -n course-production rollout status deployment/course-reservations-production
```

Rollback je proveditelny, protoze Deployment uchovava rollout historii a image je tagovana.
