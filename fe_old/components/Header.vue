<template>
<el-menu mode="horizontal" :router="true" default-active="/rule-manager">
  <el-menu-item index="/rule-manager">签名规则</el-menu-item>
  <el-menu-item index="/sign-sniff">签名嗅探</el-menu-item>
</el-menu>
</template>

<script setup lang="ts">
// import {useRouter} from "vue-router";
//
// const router = useRouter();
// router.push({
//   path: "/rule-manager"
// })
</script>

<style scoped>

</style>