<template>
  <div>
    <h2>搜索参数测试</h2>
    <input v-model="keyword" placeholder="输入编号或名称" />
    <button @click="search">搜索</button>
    <div v-if="nameResult.length > 0">
      <div>按 name 搜索:</div>
      <div v-for="item in nameResult" :key="item.code">{{ item.code }} - {{ item.name }}</div>
    </div>
    <div v-if="codeResult.length > 0">
      <div>按 code 搜索:</div>
      <div v-for="item in codeResult" :key="item.code">{{ item.code }} - {{ item.name }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const keyword = ref('')
const nameResult = ref([])
const codeResult = ref([])

const search = async () => {
  const token = localStorage.getItem('token')
  
  // 测试 name 参数
  const resName = await axios.get('/api/v1/hazard-points/page', {
    params: { pageNum: 1, pageSize: 10, name: keyword.value },
    headers: { Authorization: `Bearer ${token}` }
  })
  nameResult.value = resName.data.data?.rows.map(item => ({ code: item.code, name: item.name })) || []
  
  // 测试 code 参数
  const resCode = await axios.get('/api/v1/hazard-points/page', {
    params: { pageNum: 1, pageSize: 10, code: keyword.value },
    headers: { Authorization: `Bearer ${token}` }
  })
  codeResult.value = resCode.data.data?.rows.map(item => ({ code: item.code, name: item.name })) || []
}
</script>