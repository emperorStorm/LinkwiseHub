<template>
  <div>
    <a-button type="primary" style="margin-bottom: 16px;" @click="showModal">
      添加组织机构
    </a-button>
    <a-table :columns="columns" :data-source="organizations" row-key="id">
      <template #action="{ record }">
        <a-space>
          <a-button size="small" @click="editOrganization(record)">编辑</a-button>
          <a-button size="small" danger @click="deleteOrganization(record.id)">删除</a-button>
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
        <a-form-item label="组织机构名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入组织机构名称" />
        </a-form-item>
        <a-form-item label="父组织机构" name="parentId">
          <a-select v-model:value="form.parentId" placeholder="请选择父组织机构">
            <a-select-option value="">无</a-select-option>
            <a-select-option v-for="org in organizations" :key="org.id" :value="org.id">
              {{ org.name }}
            </a-select-option>
          </a-select>
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

const organizations = ref([])
const modalVisible = ref(false)
const modalTitle = ref('添加组织机构')
const formRef = ref(null)
const form = reactive({
  id: null,
  name: '',
  parentId: '',
  status: '1'
})
const rules = {
  name: [{ required: true, message: '请输入组织机构名称', trigger: 'blur' }]
}

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '父组织机构', dataIndex: 'parentId', key: 'parentId' },
  { title: '状态', dataIndex: 'status', key: 'status', render: (status) => (status === 1 ? '启用' : '禁用') },
  { title: '操作', key: 'action', slots: { customRender: 'action' } }
]

// 加载组织机构数据
const loadOrganizations = async () => {
  try {
    // 模拟数据，实际项目中应该调用后端接口
    organizations.value = [
      { id: 1, name: '总公司', parentId: null, level: 1, status: 1 },
      { id: 2, name: '技术部', parentId: 1, level: 2, status: 1 },
      { id: 3, name: '市场部', parentId: 1, level: 2, status: 1 },
      { id: 4, name: '财务部', parentId: 1, level: 2, status: 1 }
    ]
  } catch (error) {
    console.error('加载组织机构失败:', error)
  }
}

// 显示添加模态框
const showModal = () => {
  form.id = null
  form.name = ''
  form.parentId = ''
  form.status = '1'
  modalTitle.value = '添加组织机构'
  modalVisible.value = true
}

// 编辑组织机构
const editOrganization = (record) => {
  form.id = record.id
  form.name = record.name
  form.parentId = record.parentId
  form.status = record.status.toString()
  modalTitle.value = '编辑组织机构'
  modalVisible.value = true
}

// 删除组织机构
const deleteOrganization = (id) => {
  if (confirm('确定要删除该组织机构吗？')) {
    // 实际项目中应该调用后端接口
    organizations.value = organizations.value.filter(org => org.id !== id)
  }
}

// 处理确定按钮
const handleOk = () => {
  // 实际项目中应该调用后端接口
  if (form.id) {
    // 编辑
    const index = organizations.value.findIndex(org => org.id === form.id)
    if (index !== -1) {
      organizations.value[index] = {
        ...organizations.value[index],
        name: form.name,
        parentId: form.parentId,
        status: parseInt(form.status)
      }
    }
  } else {
    // 添加
    const newOrg = {
      id: organizations.value.length + 1,
      name: form.name,
      parentId: form.parentId,
      level: 2,
      status: parseInt(form.status)
    }
    organizations.value.push(newOrg)
  }
  modalVisible.value = false
}

// 处理取消按钮
const handleCancel = () => {
  modalVisible.value = false
}

// 组件挂载时加载数据
onMounted(() => {
  loadOrganizations()
})
</script>
