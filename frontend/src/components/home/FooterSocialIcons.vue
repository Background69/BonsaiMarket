<template>
  <div aria-label="Mạng xã hội BonsaiMarket" class="hfs-social-icons" role="group">
    <component
        :is="item.href ? 'a' : 'span'"
        v-for="item in socials"
        :key="item.name"
        :aria-label="item.href ? `${item.label} BonsaiMarket (mở tab mới)` : `${item.label} — chưa cấu hình liên kết`"
        :href="item.href || undefined"
        :rel="item.href ? 'noopener noreferrer' : undefined"
        :role="item.href ? undefined : 'img'"
        :target="item.href ? '_blank' : undefined"
        :title="item.href ? item.label : `${item.label} · Chưa cấu hình liên kết`"
        class="hfs-social-icon"
    >
      <FontAwesomeIcon v-if="item.icon" :icon="item.icon" aria-hidden="true"/>
      <!-- The supplied SVG is a sheet of variants. Use its circular transparent mark. -->
      <img v-else alt="" aria-hidden="true" height="24" src="/images/home/zalo-icon.svg#svgView(viewBox(337,114,66,66))"
           width="24"/>
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
