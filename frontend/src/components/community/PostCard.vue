<template>
  <article class="community-card">
    <div class="community-card-meta"><span class="community-avatar" aria-hidden="true">{{ post.authorName?.charAt(0) }}</span><div><strong>{{ post.authorName }}</strong><p>{{ roleLabels[post.authorRole] }} · <time :datetime="post.createdAt">{{ date(post.createdAt) }}</time></p></div><span v-if="post.pinned" class="community-label">Đã ghim</span></div>
    <div class="community-labels"><span class="community-label">{{ postTypes[post.postType] }}</span><span>{{ topics[post.topic] }}</span><span v-if="post.locked">Thảo luận đã đóng</span></div>
    <h2><RouterLink :to="'/community/posts/'+post.id">{{ post.title }}</RouterLink></h2><p class="community-preview">{{ preview(post.body) }}</p>
    <div v-if="post.images?.length" class="community-card-images"><CatalogImage v-for="(image,i) in post.images.slice(0,2)" :key="image.id" :src="image.url" :alt="'Ảnh '+(i+1)+' trong bài '+post.title" ratio="16/9" fit="cover"/></div>
    <p v-if="post.postType==='PLANT_HELP'" class="community-help-state"><MarketIcon name="sprout"/>{{ helpStates[post.helpStatus] }}</p>
    <p class="community-results">{{ post.likeCount }} lượt thích · {{ post.commentCount }} bình luận</p><slot/><RouterLink class="community-read-link" :to="'/community/posts/'+post.id">Đọc và thảo luận <MarketIcon name="arrow"/></RouterLink>
  </article>
</template>
<script setup>
import CatalogImage from '../marketplace/CatalogImage.vue';import MarketIcon from '../marketplace/MarketIcon.vue';import {postTypes,helpStates,roleLabels} from '../../services/community.js';import {topics,date,preview} from '../../services/discussions.js';defineProps({post:{type:Object,required:true}});
</script>
