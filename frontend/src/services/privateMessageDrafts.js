// Private content stays in this tab's memory, never in web storage or a public cache.
export const privateMessageDrafts=new Map();
export const privateMessageScrolls=new Map();
let actor;
export function guardPrivateDraftActor(id){if(actor!==id){privateMessageDrafts.clear();privateMessageScrolls.clear();actor=id;}}
function clear(){actor=undefined;privateMessageDrafts.clear();privateMessageScrolls.clear();}
if(typeof window!=='undefined'){window.addEventListener('bonsai:logged-out',clear);window.addEventListener('bonsai:session-expired',clear);}
