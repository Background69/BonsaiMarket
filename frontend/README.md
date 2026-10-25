# BonsaiMarket frontend

Vue 3, Vue Router and Vite render the Vietnamese P2P marketplace using Direction C. Shared tokens live in
`src/css/foundations.css`; functional workspace destinations live in `src/navigation/marketplace.js`.

Run `npm run dev -- --host 127.0.0.1 --port 5176` for local UI testing. The backend is run separately from IntelliJ;
Vite proxies `/api` to port 8080. Run `npm run build` to validate static imports, dynamic imports and CSS imports.

Browser fixtures: `npm run test:navigation`, `npm run test:management`, `npm run test:catalog`, `npm run test:auth`
and `npm run test:ai`. These suites intercept API traffic and write evidence under the repository's
`artifacts/qa`. They do not establish live MySQL, SMTP, OAuth or Gemini availability.

Management tables use native semantic markup and server pagination. Inventory retains its specialized editor with
`expectedStock` conflict recovery. Product image selection uses the bounded inventory response; only the selected
product opens an upload editor. No new frontend dependency is required.

See [current integration audit](../BONSAIMARKET_POST_MERGE_INTEGRATION_AUDIT.md) for verification and integration
boundaries.

## Discovery and discussion contracts

`GET /api/stores` preserves a bounded legacy array when no query is supplied. Query mode uses
`search,categoryId,page,size` and returns `content,page,size,totalElements,totalPages`; size is 1–50, page is
0–10000. Search normalizes Vietnamese and bounds input to 160 characters/12 words. Only active stores with
active SELLER/ADMIN owners and public product/category relationships qualify; order is stable store ID ascending.
Invalid/unknown category filters return errors; omitted optional filters retain the valid no-query contract.

Homepage newest products use `sort=latest`, based on createdAt, not first-publication time. Recommendations are
deterministic, public, in-stock, at most eight results and one product per store. Product Detail ranks same-category,
other-store products by price proximity/recency/ID; homepage exclusions avoid repeating newest IDs. There is no
personalization, sales/popularity claim or invented verification/rating.

Product Q&A reads `/api/products/{id}/questions?page=0` at size10. Session/CSRF writes use
`POST /api/account/products/{id}/questions {body}`, `PUT /api/seller/product-questions/{id}/answer {body,revision}`
and `DELETE /api/account/product-questions/{id}?revision=...`. Exactly one official answer is stored; even ADMIN
must own the store to answer. Author/ADMIN can soft-delete a question. Question/answer limits are 2000/4000
characters; backend guards allow 20 questions/hour, duplicate text per product within10minutes returns409, and
answer guard counts at most30 answered question rows/seller/hour. Revision conflicts never overwrite newer data.

Community routes are `/community`, `/community/create`, `/community/posts/:id`, `/community/posts/:id/edit`.
Public reads use `/api/community/posts`, detail and comments; writes use `/api/account/community/posts`,
post detail/comments and `/api/account/community/comments/{id}`. Author/ADMIN ownership, CSRF, revision locks
and soft-delete apply. Hidden/removed posts return404 and block comments. Public author DTOs omit private contact
details. Topics are BONSAI/SUCCULENTS/CARE/OTHER; title/body/comment limits are180/12000/3000, page size10,
posts newest-first and comments oldest-first with stable IDs. DB-backed guards allow5posts/hour,30comments/hour,
reject scope duplicates within10minutes, and count removed rows. Moderation queues, hide/restore UI, likes/views,
chat and verified-purchase claims remain outside this contract. Content is escaped plain text, never seller HTML.

Java H2/MockMvc tests protect these boundaries; [QA commands](qa/README.md) exercise isolated frontend fixtures.
MySQL collation/EXPLAIN, deployed HTTP/session and provider behavior need separate live verification.
