<script setup>
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faEnvelope} from '@fortawesome/free-solid-svg-icons';
defineProps({email: String});
</script>
<template>
  <span class="auth-masked-email"><FontAwesomeIcon :icon="faEnvelope" aria-hidden="true"/><span>{{ email }}</span></span>
</template>
