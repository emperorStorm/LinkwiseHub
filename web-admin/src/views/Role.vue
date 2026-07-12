<template>
  <div>
    <a-button type="primary" style="margin-bottom: 16px;" @click="showModal">
      添加角色
    </a-button>
    <a-table :columns="columns" :data-source="roles" row-key="id">
      <template #action="{ record }">
        <a-space>
          <a-button size="small" @click="editRole(record)">编辑</a-button>
          <a-button size="small" danger @click="deleteRole(record.id)">删除</a-button>
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
        <a-form-item label="角色名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入角色名称" />
        </a-form-item>
        <a-form-item label="角色编码" name="code">
          <a-input v-model:value="form.code" placeholder="请输入角色编码" />
        </a-form-item>
        <a-form-item label="角色描述" name="description">
          <a-input-textarea v-model:value="form.description" placeholder="请输入角色描述" />
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

const roles = ref([])
const modalVisible = ref(false)
const modalTitle = ref('添加角色')
const formRef = ref(null)
const form = reactive({
  id: null,
  name: '',
  code: '',
  description: '',
  status: '1'
})
const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '角色名称', dataIndex: 'name', key: 'name' },
  { title: '角色编码', dataIndex: 'code', key: 'code' },
  { title: '角色描述', dataIndex: 'description', key: 'description' },
  { title: '状态', dataIndex: 'status', key: 'status', render: (status) => (status === 1 ? '启用' : '禁用') },
  { title: '操作', key: 'action', slots: { customRender: 'action' } }
]

// 加载角色数据
const loadRoles = async () => {
  try {
    // 模拟数据，实际项目中应该调用后端接口
    roles.value = [
      { id: 1, name: '超级管理员', code: 'admin', description: '系统超级管理员', status: 1 },
      { id: 2, name: '技术主管', code: 'tech_manager', description: '技术部门主管', status: 1 },
      { id: 3, name: '市场主管', code: 'market_manager', description: '市场部门主管', status: 1 },
      { id: 4, name: '财务主管', code: 'finance_manager', description: '财务部门主管', status: 1 },
      { id: 5, name: '普通员工', code: 'employee', description: '普通员工', status: 1 }
    ]
  } catch (error) {
    console.error('加载角色失败:', error)
  }
}

// 显示添加模态框
const showModal = () => {
  form.id = null
  form.name = ''
  form.code = ''
  form.description = ''
  form.status = '1'
  modalTitle.value = '添加角色'
  modalVisible.value = true
}

// 编辑角色
const editRole = (record) => {
  form.id = record.id
  form.name = record.name
  form.code = record.code
  form.description = record.description
  form.status = record.status.toString()
  modalTitle.value = '编辑角色'
  modalVisible.value = true
}

// 删除角色
const deleteRole = (id) => {
  if (confirm('确定要删除该角色吗？')) {
    // 实际项目中应该调用后端接口
    roles.value = roles.value.filter(role => role.id !== id)
  }
}

// 处理确定按钮
const handleOk = () => {
  // 实际项目中应该调用后端接口
  if (form.id) {
    // 编辑
    const index = roles.value.findIndex(role => role.id === form.id)
    if (index !== -1) {
      roles.value[index] = {
        ...roles.value[index],
        name: form.name,
        code: form.code,
        description: form.description,
        status: parseInt(form.status)
      }
    }
  } else {
    // 添加
    const newRole = {
      id: roles.value.length + 1,
      name: form.name,
      code: form.code,
      description: form.description,
      status: parseInt(form.status)
    }
    roles.value.push(newRole)
  }
  modalVisible.value = false
}

// 处理取消按钮
const handleCancel = () => {
  modalVisible.value = false
}

// 组件挂载时加载数据
onMounted(() => {
  loadRoles()
})
</script>
