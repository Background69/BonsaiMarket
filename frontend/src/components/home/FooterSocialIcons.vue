<template>
  <div class="hfs-social-icons" role="group" aria-label="Mạng xã hội BonsaiMarket">
    <component
      :is="item.href ? 'a' : 'span'"
      v-for="item in socials"
      :key="item.name"
      class="hfs-social-icon"
      :href="item.href || undefined"
      :target="item.href ? '_blank' : undefined"
      :rel="item.href ? 'noopener noreferrer' : undefined"
      :role="item.href ? undefined : 'img'"
      :aria-label="item.href ? `${item.label} BonsaiMarket (mở tab mới)` : `${item.label} — chưa cấu hình liên kết`"
      :title="item.href ? item.label : `${item.label} · Chưa cấu hình liên kết`"
    >
      <FontAwesomeIcon v-if="item.icon" :icon="item.icon" aria-hidden="true" />
      <!-- The supplied SVG is a sheet of variants. Use its circular transparent mark. -->
      <img v-else src="/images/home/zalo-icon.svg#svgView(viewBox(337,114,66,66))" width="24" height="24" alt="" aria-hidden="true" />
    </component>
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
].map(item => ({...item, href: socialLinks[item.name]}));
</script>
