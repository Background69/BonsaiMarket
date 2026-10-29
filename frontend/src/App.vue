<template>
  <RouterView v-slot="{Component, route}">
    <Transition name="market-route" :duration="{enter: 240, leave: 0}" @after-enter="focusPage">
      <component :is="Component" :key="route.path"/>
    </Transition>
  </RouterView>
  <AIExpertChat/>
</template>
<script setup>
import AIExpertChat from './components/AIExpertChat.vue';
import './css/botanical-motion.css';
function focusPage(){
  if(document.activeElement && document.activeElement!==document.body)return;
  const main=document.querySelector('#market-main, #workspace-main, #auth-main, main');
  if(main){main.setAttribute('tabindex','-1');main.focus({preventScroll:true});}
}
</script>
