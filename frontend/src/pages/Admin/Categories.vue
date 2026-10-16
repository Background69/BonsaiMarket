<template>
  <AdminLayout><div class="management-page">
    <header><h1>Danh mục sản phẩm</h1><p>Quản lý tên, mô tả và hiển thị danh mục trên marketplace.</p></header>
    <form class="management-toolbar" @submit.prevent="search">
      <label>Tìm danh mục<input v-model="term" type="search" maxlength="150" :disabled="busy"/></label>
      <button class="button button-outline" :disabled="busy">Tìm kiếm</button>
      <button ref="addButton" type="button" class="button button-primary" :disabled="busy" @click="edit(null,$event)">Thêm danh mục</button>
    </form>
    <FormFeedback prefix="category" :error="error" :success="success" :fields="fields"/>
    <form v-if="editing" class="management-form management-editor" :aria-busy="busy" @submit.prevent="save">
      <h2>{{ selected?'Sửa danh mục':'Thêm danh mục' }}</h2>
      <ManagedField id="category-name" v-model="draft.name" label="Tên danh mục" :max="150" required :disabled="busy" :error="fields.name"/>
      <ManagedField id="category-description" v-model="draft.description" label="Mô tả" :max="4000" multiline :disabled="busy" :error="fields.description"/>
      <label class="checkbox-label"><input v-model="draft.active" type="checkbox" :disabled="busy"/>Hiển thị danh mục và sản phẩm thuộc danh mục</label>
      <p v-if="selected" class="muted">Đường dẫn: {{ selected.slug }}. Đổi tên không làm thay đổi đường dẫn.</p>
      <div class="management-actions"><button class="button button-primary" :disabled="busy||conflict">{{ busy?'Đang lưu…':'Lưu danh mục' }}</button><button type="button" class="button button-outline" :disabled="busy" @click="cancel">Hủy</button></div>
      <p v-if="conflict">Danh mục đã thay đổi. Bản nháp được giữ lại để bạn đối chiếu.</p>
      <button v-if="conflict" type="button" class="button button-outline" :disabled="busy" @click="recover">Bỏ bản nháp và tải danh mục mới</button>
    </form>
    <ManagementTable caption="Danh mục sản phẩm: tên, mô tả, trạng thái và thao tác" :columns="columns" :rows="data.content" :loading="loading" :error="listError" empty-title="Không có danh mục phù hợp." @retry="load">
      <template #name="{row}">{{ row.name }}<small>{{ row.slug }}</small></template>
      <template #description="{row}"><span class="management-table-copy">{{ row.description || 'Chưa có mô tả' }}</span></template>
      <template #active="{row}"><span class="management-status">{{ row.active?'Đang hiển thị':'Đã ẩn' }}</span></template>
      <template #actions="{row}"><div class="management-actions"><button class="button button-outline" :disabled="busy" :aria-label="'Sửa danh mục '+row.name" @click="edit(row,$event)">Sửa</button><button class="button button-outline" :disabled="busy" :aria-label="'Xóa danh mục '+row.name" @click="remove(row)">Xóa</button></div></template>
    </ManagementTable>
    <ManagementPagination v-if="!listError" :page="data.page" :size="data.size" :total-pages="data.totalPages" :total-elements="data.totalElements" :count="data.content.length" noun="danh mục" :loading="loading" :busy="loading||busy"/>
  </div></AdminLayout>
</template>
<script setup>
import {ref,watch,nextTick} from 'vue';import {useRoute,useRouter} from 'vue-router';import AdminLayout from '../../layouts/AdminLayout.vue';import ManagedField from '../../components/marketplace/ManagedField.vue';import FormFeedback from '../../components/marketplace/FormFeedback.vue';import ManagementTable from '../../components/marketplace/ManagementTable.vue';import ManagementPagination from '../../components/marketplace/ManagementPagination.vue';
import {useServerList} from '../../composables/useServerList.js';import {useManagedForm} from '../../composables/useManagedForm.js';import {marketplaceRequest as request} from '../../services/marketplace.js';
const columns=[{key:'name',label:'Danh mục'},{key:'description',label:'Mô tả'},{key:'active',label:'Trạng thái'},{key:'actions',label:'Thao tác'}];
const addButton=ref(null);let opener;
const route=useRoute(),router=useRouter(),term=ref(''),selected=ref(null),editing=ref(false),conflict=ref(false),empty={name:'',description:'',active:true,revision:0};
const {draft,busy,dirty,error,success,fields,reset,submit}=useManagedForm(empty,'category');const {data,loading,error:listError,load}=useServerList('/admin/categories',q=>({search:q.search||''}));
watch(()=>route.query.search,s=>term.value=s||'',{immediate:true});
function search(){router.push({path:route.path,query:term.value.trim()?{search:term.value.trim()}: {}});}
async function edit(c,event){if(dirty.value&&!window.confirm('Bỏ thay đổi chưa lưu?'))return;opener=event?.currentTarget;selected.value=c;reset(c?{name:c.name,description:c.description||'',active:c.active,revision:c.revision}:empty);success.value='';conflict.value=false;editing.value=true;await nextTick();document.getElementById('category-name')?.focus();}
async function cancel(){if(dirty.value&&!window.confirm('Bỏ thay đổi chưa lưu?'))return;reset(empty);editing.value=false;selected.value=null;conflict.value=false;await nextTick();(opener?.isConnected?opener:addButton.value)?.focus();}
async function recover(){if(!window.confirm('Bỏ bản nháp và tải danh mục mới?'))return;const id=selected.value?.id;reset(empty);editing.value=false;conflict.value=false;await load();const current=data.value.content.find(c=>c.id===id);if(current)await edit(current);}
function save(){submit(async()=>{try{await request('/admin/categories'+(selected.value?'/'+selected.value.id:''),selected.value?'PUT':'POST',{...draft});reset({...draft});await cancel();success.value='Đã lưu danh mục.';await load();}catch(e){conflict.value=e.status===409&&e.code==='DATA_CONFLICT';throw e;}});}
function remove(c){if(dirty.value){error.value='Lưu hoặc hủy bản nháp trước khi xóa danh mục.';return;}if(!window.confirm('Xóa danh mục “'+c.name+'”? Danh mục đang được tham chiếu sẽ không bị xóa.'))return;submit(async()=>{await request('/admin/categories/'+c.id+'?revision='+c.revision,'DELETE');success.value='Đã xóa danh mục.';await load();});}
</script>
