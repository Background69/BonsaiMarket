<template>
  <div v-if="socials.length" aria-label="Mạng xã hội BonsaiMarket" class="hfs-social-icons" role="group">
    <a
        v-for="item in socials"
        :key="item.name"
        :aria-label="`${item.label} BonsaiMarket (mở tab mới)`"
        :href="item.href"
        rel="noopener noreferrer"
        target="_blank"
        :title="item.label"
        class="hfs-social-icon"
    >
      <FontAwesomeIcon v-if="item.icon" :icon="item.icon" aria-hidden="true"/>
      <!-- The supplied SVG is a sheet of variants. Use its circular transparent mark. -->
      <img v-else alt="" aria-hidden="true" height="24" src="/images/home/zalo-icon.svg#svgView(viewBox(337,114,66,66))"
           width="24"/>
    </a>
  </div>
</template>

<script setup>
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faFacebook, faYoutube, faTiktok} from '@fortawesome/free-brands-svg-icons';
import {socialLinks} from '../../config/socialLinks.js';

const socials = [
  {name: 'facebook', label: 'Facebook', icon: faFacebook},
  {name: 'youtube', label: 'YouTube', icon: faYoutube},
  {name: 'tiktok', label: 'TikTok', icon: faTiktok},
  {name: 'zalo', label: 'Zalo'},
].map(item => ({...item, href: socialLinks[item.name]})).filter(item => /^https:\/\//.test(item.href || ''));
</script>
