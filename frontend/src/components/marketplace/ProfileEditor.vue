<template>
  <section class="management-page"><p v-if="loading" role="status">Đang tải hồ sơ…</p>
    <FormFeedback prefix="profile" :error="error" :fields="fields" :success="success"/>
    <button v-if="!profile&&!loading" class="button button-outline" @click="load">Tải lại hồ sơ</button>
    <template v-if="profile"><dl v-if="!editing" class="management-detail-list"><dt>Họ tên</dt><dd>{{ profile.name }}</dd><dt>Email</dt><dd>{{ profile.email }}</dd><dt>Số điện thoại</dt><dd>{{ profile.phone || 'Chưa bổ sung' }}</dd><dt>Giới thiệu</dt><dd>{{ profile.bio || 'Chưa bổ sung' }}</dd><dt>Xác thực email</dt><dd>{{ profile.emailVerified?'Đã xác thực':'Chưa xác thực' }}</dd></dl>
      <button v-if="!editing" ref="editButton" class="button button-outline" @click="edit">Chỉnh sửa hồ sơ</button>
      <form v-else class="management-form" :aria-busy="busy" @submit.prevent="save">
        <ManagedField id="profile-name" v-model="draft.name" label="Họ và tên" :max="150" required autocomplete="name" :disabled="busy" :error="fields.name"/>
        <ManagedField id="profile-phone" v-model="draft.phone" label="Số điện thoại" :max="30" type="tel" autocomplete="tel" :disabled="busy" :error="fields.phone"/>
        <ManagedField id="profile-bio" v-model="draft.bio" label="Giới thiệu" :max="2000" multiline :disabled="busy" :error="fields.bio"/>
        <p class="muted">Email tài khoản: {{ profile.email }}</p>
        <div class="management-actions"><button class="button button-primary" :disabled="busy||!dirty">{{ busy?'Đang lưu…':'Lưu hồ sơ' }}</button><button type="button" class="button button-outline" :disabled="busy" @click="cancel">Hủy</button></div>
      </form>
    </template>
  </section>
</template>
<script setup>
import {ref,onMounted,nextTick} from 'vue';
import {useManagedForm} from '../../composables/useManagedForm.js';
import {restoreSession} from '../../composables/useAuth.js';
import {marketplaceRequest as request} from '../../services/marketplace.js';
import ManagedField from './ManagedField.vue';import FormFeedback from './FormFeedback.vue';
const profile=ref(null),editing=ref(false),loading=ref(true),editButton=ref(null);
const {draft,busy,dirty,error,success,fields,reset,fail,submit}=useManagedForm({name:'',phone:'',bio:''},'profile');
function values(p){return {name:p.name||'',phone:p.phone||'',bio:p.bio||''};}
async function load(){loading.value=true;try{profile.value=await request('/account/profile');reset(values(profile.value));}catch(e){await fail(e);}finally{loading.value=false;}}
async function edit(){reset(values(profile.value));success.value='';editing.value=true;await nextTick();document.getElementById('profile-name')?.focus();}
async function cancel(){if(dirty.value&&!window.confirm('Bỏ thay đổi hồ sơ chưa lưu?'))return;reset(values(profile.value));editing.value=false;await nextTick();editButton.value?.focus();}
function save(){submit(async()=>{profile.value=await request('/account/profile','PATCH',{...draft});reset(values(profile.value));editing.value=false;success.value='Đã lưu hồ sơ.';await restoreSession(true);await nextTick();editButton.value?.focus();});}
onMounted(load);
</script>
