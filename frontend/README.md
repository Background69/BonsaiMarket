# BonsaiMarket frontend

Vue 3, Vue Router and Vite render the Vietnamese P2P marketplace using Direction C. Shared tokens live in
`src/css/foundations.css`; functional workspace destinations live in `src/navigation/marketplace.js`.

Run `npm run dev -- --host 127.0.0.1 --port 5176` for local UI testing. The backend is run separately from IntelliJ;
Vite proxies `/api` to port 8080. Run `npm run build` to validate static imports, dynamic imports and CSS imports.

Browser fixtures: `npm run test:navigation`, `npm run test:management`, `npm run test:catalog`, `npm run test:auth`
and `npm run test:ai`. These suites intercept API traffic and write evidence under the repository's
`artifacts/productization`. They do not establish live MySQL, SMTP, OAuth or Gemini availability.

Management tables use native semantic markup and server pagination. Inventory retains its specialized editor with
`expectedStock` conflict recovery. Product image selection uses the bounded inventory response; only the selected
product opens an upload editor. No new frontend dependency is required.

See [the implementation report](../BONSAIMARKET_FRONTEND_PRODUCTIZATION_REPORT.md) for verification and integration
boundaries.
