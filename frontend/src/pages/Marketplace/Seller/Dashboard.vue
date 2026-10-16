<template>
  <SellerLayout>
    <header class="workspace-page-heading"><h1>Tổng quan gian hàng</h1><p>Cập nhật sản phẩm, hình ảnh và thông tin gian hàng.</p></header>
    <WorkspaceTask :title="tasks[0].label" :description="tasks[0].description" :to="tasks[0].to" :action="'Quản lý tồn kho'"/>
    <nav aria-label="Các mục khác của gian hàng" class="workspace-quick-links">
      <RouterLink v-for="task in tasks.slice(1)" :key="task.to" :to="task.to"><MarketIcon :name="task.icon"/><span><strong>{{ task.label }}</strong><small>{{ task.description }}</small></span><MarketIcon name="arrow"/></RouterLink>
    </nav>
  </SellerLayout>
</template>
<script setup>
import SellerLayout from './SellerLayout.vue';
import WorkspaceTask from '../../../components/marketplace/WorkspaceTask.vue';
import MarketIcon from '../../../components/marketplace/MarketIcon.vue';
import {workspaceNavigation} from '../../../navigation/marketplace.js';
const tasks = workspaceNavigation.seller.slice(1);
</script>
