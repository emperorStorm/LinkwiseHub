<template>
  <div>
    <a-button type="primary" style="margin-bottom: 16px;" @click="showModal">
      添加用户
    </a-button>
    <a-table :columns="columns" :data-source="users" row-key="id">
      <template #action="{ record }">
        <a-space>
          <a-button size="small" @click="editUser(record)">编辑</a-button>
          <a-button size="small" danger @click="deleteUser(record.id)">删除</a-button>
        </a-space>
      </template>
    </a-table>

    <!-- 模态框 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="modalTitle"
      @ok="handleOk"
      @cancel="handleCancel"
    >
      <a-form :model="form" :rules="rules" ref="formRef">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="form.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="密码" name="password" v-if="!form.id">
          <a-input-password v-model:value="form.password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item label="真实姓名" name="realName">
          <a-input v-model:value="form.realName" placeholder="请输入真实姓名" />
        </a-form-item>
        <a-form-item label="所属组织机构" name="organizationId">
          <a-select v-model:value="form.organizationId" placeholder="请选择组织机构">
            <a-select-option v-for="org in organizations" :key="org.id" :value="org.id">
              {{ org.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="form.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="电话" name="phone">
          <a-input v-model:value="form.phone" placeholder="请输入电话" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="form.status">
            <a-radio-button value="1">启用</a-radio-button>
            <a-radio-button value="0">禁用</a-radio-button>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'

const users = ref([])
const organizations = ref([])
const modalVisible = ref(false)
const modalTitle = ref('添加用户')
const formRef = ref(null)
const form = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  organizationId: '',
  email: '',
  phone: '',
  status: '1'
})
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  organizationId: [{ required: true, message: '请选择组织机构', trigger: 'blur' }]
}

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '真实姓名', dataIndex: 'realName', key: 'realName' },
  { title: '所属组织机构', dataIndex: 'organizationId', key: 'organizationId' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '电话', dataIndex: 'phone', key: 'phone' },
  { title: '状态', dataIndex: 'status', key: 'status', render: (status) => (status === 1 ? '启用' : '禁用') },
  { title: '操作', key: 'action', slots: { customRender: 'action' } }
]

// 加载用户数据
const loadUsers = async () => {
  try {
    // 模拟数据，实际项目中应该调用后端接口
    users.value = [
      { id: 1, username: 'admin', realName: '管理员', organizationId: 1, email: 'admin@example.com', phone: '13800138000', status: 1 },
      { id: 2, username: 'tech_lead', realName: '技术主管', organizationId: 2, email: 'tech_lead@example.com', phone: '13800138001', status: 1 },
      { id: 3, username: 'market_lead', realName: '市场主管', organizationId: 3, email: 'market_lead@example.com', phone: '13800138002', status: 1 },
      { id: 4, username: 'finance_lead', realName: '财务主管', organizationId: 4, email: 'finance_lead@example.com', phone: '13800138003', status: 1 },
      { id: 5, username: 'employee', realName: '普通员工', organizationId: 2, email: 'employee@example.com', phone: '13800138004', status: 1 }
    ]
  } catch (error) {
    console.error('加载用户失败:', error)
  }
}

// 加载组织机构数据
const loadOrganizations = async () => {
  try {
    // 模拟数据，实际项目中应该调用后端接口
    organizations.value = [
      { id: 1, name: '总公司' },
      { id: 2, name: '技术部' },
      { id: 3, name: '市场部' },
      { id: 4, name: '财务部' }
    ]
  } catch (error) {
    console.error('加载组织机构失败:', error)
  }
}

// 显示添加模态框
const showModal = () => {
  form.id = null
  form.username = ''
  form.password = ''
  form.realName = ''
  form.organizationId = ''
  form.email = ''
  form.phone = ''
  form.status = '1'
  modalTitle.value = '添加用户'
  modalVisible.value = true
}

// 编辑用户
const editUser = (record) => {
  form.id = record.id
  form.username = record.username
  form.password = ''
  form.realName = record.realName
  form.organizationId = record.organizationId
  form.email = record.email
  form.phone = record.phone
  form.status = record.status.toString()
  modalTitle.value = '编辑用户'
  modalVisible.value = true
}

// 删除用户
const deleteUser = (id) => {
  if (confirm('确定要删除该用户吗？')) {
    // 实际项目中应该调用后端接口
    users.value = users.value.filter(user => user.id !== id)
  }
}

// 处理确定按钮
const handleOk = () => {
  // 实际项目中应该调用后端接口
  if (form.id) {
    // 编辑
    const index = users.value.findIndex(user => user.id === form.id)
    if (index !== -1) {
      users.value[index] = {
        ...users.value[index],
        username: form.username,
        realName: form.realName,
        organizationId: form.organizationId,
        email: form.email,
        phone: form.phone,
        status: parseInt(form.status)
      }
    }
  } else {
    // 添加
    const newUser = {
      id: users.value.length + 1,
      username: form.username,
      realName: form.realName,
      organizationId: form.organizationId,
      email: form.email,
      phone: form.phone,
      status: parseInt(form.status)
    }
    users.value.push(newUser)
  }
  modalVisible.value = false
}

// 处理取消按钮
const handleCancel = () => {
  modalVisible.value = false
}

// 组件挂载时加载数据
onMounted(() => {
  loadUsers()
  loadOrganizations()
})
</script>
