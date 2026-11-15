const owners=new Set();let previous;
export function lockBodyScroll(){
    const owner=Symbol('dialog');if(!owners.size){previous=document.body.style.overflow;document.body.style.overflow='hidden';}owners.add(owner);
    return ()=>{if(!owners.delete(owner))return;if(!owners.size){document.body.style.overflow=previous;previous=undefined;}};
}
