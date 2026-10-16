import {computed, nextTick, onBeforeUnmount, onMounted, reactive, ref} from 'vue';
import {onBeforeRouteLeave, onBeforeRouteUpdate} from 'vue-router';
import {useAuth} from './useAuth.js';
export function useManagedForm(initial, prefix = 'form') {
    const draft=reactive({...initial}),saved=ref(JSON.stringify(initial)),busy=ref(false),error=ref(''),success=ref(''),fields=ref({});
    const dirty=computed(()=>JSON.stringify(draft)!==saved.value),{user}=useAuth();
    function reset(value){Object.assign(draft,value);saved.value=JSON.stringify(draft);fields.value={};error.value='';}
    async function fail(e){error.value=e.message||'Không thể lưu. Vui lòng thử lại.';fields.value=e.fields||{};await nextTick();document.getElementById(prefix+'-errors')?.focus();}
    async function submit(action){if(busy.value)return;busy.value=true;error.value='';success.value='';fields.value={};try{await action();}catch(e){await fail(e);}finally{busy.value=false;}}
    function leave(){return !user.value||(!busy.value&&(!dirty.value||window.confirm('Rời trang và bỏ thay đổi chưa lưu?')));}
    onBeforeRouteLeave(leave);onBeforeRouteUpdate(leave);
    function unload(e){if(busy.value||dirty.value){e.preventDefault();e.returnValue='';}}
    onMounted(()=>window.addEventListener('beforeunload',unload));onBeforeUnmount(()=>window.removeEventListener('beforeunload',unload));
    return {draft,busy,dirty,error,success,fields,reset,fail,submit};
}
