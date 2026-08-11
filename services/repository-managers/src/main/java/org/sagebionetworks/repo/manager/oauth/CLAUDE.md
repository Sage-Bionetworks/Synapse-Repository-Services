# manager/oauth

OAuth 2.0 / OpenID Connect provider logic — token issuance, consent, and OIDC claim population. Uses `io.jsonwebtoken` (JJWT) for JWT signing/verification, distinct from the rest of the manager layer.

## Claim-provider plugin registry

Each OIDC claim is produced by its own `OIDCClaimProvider` `@Service` bean (`claimprovider/` — e.g. `EmailClaimProvider`, `CompanyClaimProvider`, `GA4GHPassportClaimProvider`). They are assembled into a `Map<OIDCClaimName, OIDCClaimProvider>` in `ManagerConfiguration.claimProviders(List<OIDCClaimProvider>)` — the map is keyed by the provider's declared `OIDCClaimName`.

**To support a new claim, add a new `OIDCClaimProvider` `@Service`** keyed to its `OIDCClaimName`; it is picked up automatically. Do not add claim logic to a central method — the registry is the extension point. A claim with no registered provider is simply never populated.

## Notes

- Consent/claim decisions gate what user data is released into tokens — treat the claim providers as the authorization surface for released PII, not just formatting.
