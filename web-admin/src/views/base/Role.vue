<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">角色管理</h2>
      <a-button type="primary" class="add-button" @click="showModal">
        <template #icon><plus-outlined /></template>
        添加角色
      </a-button>
    </div>

    <div class="table-wrapper">
      <a-table 
        :columns="columns" 
        :data-source="roles" 
        row-key="id"
        :pagination="{ pageSize: 10, showSizeChanger: true }"
        class="data-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <span class="status-badge" :class="record.status === 1 ? 'status-active' : 'status-inactive'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="editRole(record)">
                <edit-outlined /> 编辑
              </a-button>
              <a-popconfirm
                title="确定要删除该角色吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="deleteRole(record.id)"
              >
                <a-button type="link" size="small" danger>
                  <delete-outlined /> 删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      @ok="handleOk"
      @cancel="handleCancel"
      :confirm-loading="loading"
      width="500px"
      class="form-modal"
    >
      <a-form :model="form" :rules="rules" ref="formRef" layout="vertical">
        <a-form-item label="角色名称" name="name">
          <a-input v-model:value="form.name" placeholder="请输入角色名称" />
        </a-form-item>
        <a-form-item label="角色编码" name="code">
          <a-input v-model:value="form.code" placeholder="请输入角色编码" />
        </a-form-item>
        <a-form-item label="角色描述" name="description">
          <a-textarea v-model:value="form.description" placeholder="请输入角色描述" :rows="3" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="form.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import axios from 'axios'

const roles = ref([])
const modalVisible = ref(false)
const modalTitle = ref('添加角色')
const loading = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  name: '',
  code: '',
  description: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '角色名称', dataIndex: 'name', key: 'name' },
  { title: '角色编码', dataIndex: 'code', key: 'code' },
  { title: '角色描述', dataIndex: 'description', key: 'description' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 180 }
]

const loadRoles = async () => {
  try {
    const response = await axios.get('/api/base/role')
    roles.value = response.data
  } catch (error) {
    console.error('加载角色失败:', error)
    roles.value = [
      { id: 1, name: '超级管理员', code: 'super_admin', description: '系统超级管理员，拥有所有权限', status: 1 },
      { id: 2, name: '系统管理员', code: 'system_admin', description: '系统管理员，负责系统日常维护', status: 1 },
      { id: 3, name: '部门经理', code: 'dept_manager', description: '部门经理，管理本部门事务', status: 1 },
      { id: 4, name: '人事专员', code: 'hr_specialist', description: '人事专员，负责人事相关事务', status: 1 },
      { id: 5, name: '普通员工', code: 'employee', description: '普通员工，基础权限', status: 1 }
    ]
  }
}

const showModal = () => {
  form.id = null
  form.name = ''
  form.code = ''
  form.description = ''
  form.status = 1
  modalTitle.value = '添加角色'
  modalVisible.value = true
}

const editRole = (record) => {
  form.id = record.id
  form.name = record.name
  form.code = record.code
  form.description = record.description
  form.status = record.status
  modalTitle.value = '编辑角色'
  modalVisible.value = true
}

const deleteRole = async (id) => {
  try {
    await axios.delete(`/api/base/role/${id}`)
    roles.value = roles.value.filter(role => role.id !== id)
  } catch (error) {
    console.error('删除角色失败:', error)
    roles.value = roles.value.filter(role => role.id !== id)
  }
}

const handleOk = async () => {
  try {
    await formRef.value?.validate()
    loading.value = true
    
    if (form.id) {
      await axios.put(`/api/base/role/${form.id}`, form)
      await loadRoles()
    } else {
      await axios.post('/api/base/role', form)
      await loadRoles()
    }
    modalVisible.value = false
  } catch (error) {
    console.error('保存角色失败:', error)
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  modalVisible.value = false
}

onMounted(() => {
  loadRoles()
})
</script>

<style scoped>
.page-container {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.add-button {
  background: linear-gradient(135deg, #3b82f6 0%, #1e40af 100%);
  border: none;
  border-radius: 8px;
  height: 40px;
  padding: 0 20px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
  transition: all 0.3s ease;
}

.add-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.35);
}

.table-wrapper {
  background: white;
  border-radius: 12px;
  overflow: hidden;
}

.data-table :deep(.ant-table-thead > tr > th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  border-bottom: 2px solid #e2e8f0;
}

.data-table :deep(.ant-table-tbody > tr:hover > td) {
  background: #f8fafc;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-active {
  background: #dcfce7;
  color: #16a34a;
}

.status-inactive {
  background: #fee2e2;
  color: #dc2626;
}

.form-modal :deep(.ant-modal-content) {
  border-radius: 16px;
}

.form-modal :deep(.ant-modal-header) {
  border-radius: 16px 16px 0 0;
}
</style>
