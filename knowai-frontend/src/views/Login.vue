<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="title">KnowAI 智能知识库</h2>
      <p class="subtitle">{{ isLogin ? '登录' : '注册' }}</p>

      <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item v-if="!isLogin" prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称" :prefix-icon="Avatar" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
          {{ isLogin ? '登录' : '注册' }}
        </el-button>
      </el-form>

      <div class="toggle">
        <span @click="toggleMode">{{ isLogin ? '没有账号？去注册' : '已有账号？去登录' }}</span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Avatar } from '@element-plus/icons-vue'
import { login, register } from '../api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const isLogin = ref(true)

const form = reactive({ username: '', password: '', nickname: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

const toggleMode = () => {
  isLogin.value = !isLogin.value
  formRef.value && formRef.value.clearValidate()
}

const submit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    if (isLogin.value) {
      const res = await login({ username: form.username, password: form.password })
      localStorage.setItem('token', res.data.token)
      ElMessage.success('登录成功')
      router.push('/home')
    } else {
      await register({ username: form.username, password: form.password, nickname: form.nickname })
      ElMessage.success('注册成功，请登录')
      isLogin.value = true
      form.password = ''
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
.login-card {
  width: 400px;
  padding: 20px;
}
.title {
  text-align: center;
  margin: 0 0 8px;
}
.subtitle {
  text-align: center;
  color: #888;
  margin: 0 0 24px;
}
.submit-btn {
  width: 100%;
}
.toggle {
  text-align: center;
  margin-top: 16px;
  color: #409eff;
  cursor: pointer;
}
</style>
