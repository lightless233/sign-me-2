<script setup lang="tsx">
import {RouterLink, RouterView} from 'vue-router'
import {NLayout, NMessageProvider, NLayoutHeader, NLayoutSider, NMenu} from "naive-ui"
import {type MenuOption} from "naive-ui"
import {List, ColorWand} from '@vicons/ionicons5'

// 点击菜单时触发的操作，TODO 需要高亮对应的菜单项
const handleUpdateValue = (v: string) => {
  console.log("handleUpdateValue: v")
}

// 菜单定义
const menuOpts: MenuOption[] = [
  {
    label: () => (
      <RouterLink to={{name: 'rule-manager'}}>
        规则管理
      </RouterLink>
    ),
    key: 'rule-manager',
    icon: () => (<List/>)
  },
  {
    label: () => (
      <RouterLink to={{name: 'rule-sniff'}}>
        规则嗅探
      </RouterLink>
    ),
    key: 'rule-sniff',
    icon: () => (<ColorWand/>)
  }
];
</script>

<template>
  <n-message-provider>
    <n-layout class="h-screen">
      <!-- header -->
      <n-layout-header class="h-16 p-5 bg-green-600 text-white" bordered>
        <span class="font-bold text-xl">SIGN ME</span>
      </n-layout-header>

      <n-layout position="absolute" has-sider style="top: 64px">
        <!-- sidebar -->
        <n-layout-sider
          width="8%"
          show-trigger
          show-collapsed-content
          :collapsed-width="64"
          content-style="padding: 8px; text-align:center;"
          :native-scrollbar="false"
          bordered
          collapse-mode="width"
        >
          <n-menu
            :indent="24"
            :options="menuOpts"
            @update:value="handleUpdateValue"
          />
        </n-layout-sider>

        <!-- content -->
        <n-layout content-style="padding: 16px;" :native-scrollbar="false">
          <router-view/>
        </n-layout>
      </n-layout>

    </n-layout>
  </n-message-provider>
</template>

