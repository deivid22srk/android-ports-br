import { initStore, refreshSources } from "./store.js";

initStore();
const result = await refreshSources({ force: true });
console.log(JSON.stringify(result, null, 2));
process.exit(0);
