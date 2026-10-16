<script setup>
import {onMounted, onBeforeUnmount} from 'vue';
import {useRouter} from 'vue-router';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faCircleCheck, faArrowRight} from '@fortawesome/free-solid-svg-icons';
const props = defineProps({title: String, description: String, action: String, destination: String, delay: {type: Number, default: 0}});
const router = useRouter();
let timer;
onMounted(() => {if (props.delay > 0) timer = setTimeout(() => router.replace(props.destination), props.delay * 1000);});
onBeforeUnmount(() => clearTimeout(timer));
</script>
<template>
  <div class="auth-success" role="status">
    <FontAwesomeIcon class="auth-success-icon" :icon="faCircleCheck"/>
    <h2 id="auth-panel-title" tabindex="-1">{{ title }}</h2>
    <p class="auth-intro">{{ description }}</p>
    <RouterLink :to="destination" class="auth-primary">{{ action }} <FontAwesomeIcon :icon="faArrowRight"/></RouterLink>
    <p v-if="delay" class="auth-help auth-redirect-note">Bạn sẽ được chuyển về trang chủ sau vài giây.</p>
  </div>
</template>
