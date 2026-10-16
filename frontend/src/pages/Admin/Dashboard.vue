<template>
  <AdminLayout>
    <header class="workspace-page-heading"><h1>Quản trị sàn BonsaiMarket</h1><p>Quản lý danh mục, xét duyệt gian hàng và xử lý báo cáo.</p></header>
    <WorkspaceTask :title="tasks[0].label" :description="tasks[0].description" :to="tasks[0].to" :action="'Quản lý danh mục'"/>
    <nav aria-label="Công việc quản trị" class="workspace-quick-links">
      <RouterLink v-for="task in tasks.slice(1)" :key="task.to" :to="task.to"><MarketIcon :name="task.icon"/><span><strong>{{ task.label }}</strong><small>{{ task.description }}</small></span><MarketIcon name="arrow"/></RouterLink>
    </nav>
  </AdminLayout>
</template>
<script setup>
import AdminLayout from '../../layouts/AdminLayout.vue';
import WorkspaceTask from '../../components/marketplace/WorkspaceTask.vue';
import MarketIcon from '../../components/marketplace/MarketIcon.vue';
import {workspaceNavigation} from '../../navigation/marketplace.js';
const tasks = workspaceNavigation.admin.slice(1);
</script>
