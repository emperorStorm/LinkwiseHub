<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">用户管理</h2>
      <a-button type="primary" class="add-button" @click="showModal">
        <template #icon><plus-outlined /></template>
        添加用户
      </a-button>
    </div>

    <div class="table-wrapper">
      <a-table 
        :columns="columns" 
        :data-source="users" 
        row-key="id"
        :pagination="{ pageSize: 10, showSizeChanger: true }"
        class="data-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'realName'">
            {{ record.realName || record.real_name || '-' }}
          </template>
          <template v-else-if="column.key === 'organizationName'">
            {{ record.organizationName || record.organization_name || '-' }}
          </template>
          <template v-else-if="column.key === 'status'">
            <span class="status-badge" :class="record.status === 1 ? 'status-active' : 'status-inactive'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="editUser(record)">
                <edit-outlined /> 编辑
              </a-button>
              <a-popconfirm
                :title="`确定要外呼 ${getDisplayName(record)}（${getPhone(record)}）吗？`"
                ok-text="确定外呼"
                cancel-text="取消"
                :disabled="!getPhone(record) || isOutboundLoading(record.id)"
                @confirm="outboundCall(record)"
              >
                <a-button
                  type="link"
                  size="small"
                  :disabled="!getPhone(record)"
                  :loading="isOutboundLoading(record.id)"
                >
                  <phone-outlined /> 外呼
                </a-button>
              </a-popconfirm>
              <a-popconfirm
                title="确定要删除该用户吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="deleteUser(record.id)"
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
          <a-tree-select
            v-model:value="form.organizationId"
            :tree-data="treeData"
            placeholder="请选择组织机构"
            allow-clear
            tree-default-expand-all
          />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="form.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="电话" name="phone">
          <a-input v-model:value="form.phone" placeholder="请输入电话" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { PlusOutlined, EditOutlined, DeleteOutlined, PhoneOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import axios from 'axios'

const users = ref([])
const organizations = ref([])
const modalVisible = ref(false)
const modalTitle = ref('添加用户')
const loading = ref(false)
const outboundLoadingIds = ref(new Set())
const formRef = ref(null)

const form = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  organizationId: null,
  email: '',
  phone: '',
  status: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }]
}

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '真实姓名', dataIndex: 'realName', key: 'realName' },
  { title: '所属组织机构', dataIndex: 'organizationName', key: 'organizationName' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '电话', dataIndex: 'phone', key: 'phone' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 260 }
]

const treeData = computed(() => {
  const buildTree = (list, parentId = null) => {
    return list
      .filter(item => item.parentId === parentId)
      .map(item => ({
        value: item.id,
        title: item.name,
        children: buildTree(list, item.id)
      }))
  }
  return buildTree(organizations.value)
})

const loadUsers = async () => {
  try {
    const response = await axios.get('/api/base/user')
    users.value = response.data
  } catch (error) {
    console.error('加载用户失败:', error)
    users.value = [
      { id: 1, username: 'admin', realName: '管理员', organizationId: 1, organizationName: '集团总部', email: 'admin@oa.com', phone: '13800000001', status: 1 },
      { id: 2, username: 'zhangsan', realName: '张三', organizationId: 2, organizationName: '技术研发中心', email: 'zhangsan@oa.com', phone: '13800000002', status: 1 }
    ]
  }
}

const loadOrganizations = async () => {
  try {
    const response = await axios.get('/api/base/organization')
    organizations.value = response.data
  } catch (error) {
    console.error('加载组织机构失败:', error)
    organizations.value = [
      { id: 1, name: '集团总部', parentId: null },
      { id: 2, name: '技术研发中心', parentId: 1 }
    ]
  }
}

const showModal = () => {
  form.id = null
  form.username = ''
  form.password = ''
  form.realName = ''
  form.organizationId = null
  form.email = ''
  form.phone = ''
  form.status = 1
  modalTitle.value = '添加用户'
  modalVisible.value = true
}

const editUser = (record) => {
  form.id = record.id
  form.username = record.username
  form.password = ''
  form.realName = record.realName || record.real_name || ''
  form.organizationId = record.organizationId || record.organization_id
  form.email = record.email
  form.phone = record.phone
  form.status = record.status
  modalTitle.value = '编辑用户'
  modalVisible.value = true
}

const deleteUser = async (id) => {
  try {
    await axios.delete(`/api/base/user/${id}`)
    users.value = users.value.filter(user => user.id !== id)
  } catch (error) {
    console.error('删除用户失败:', error)
    users.value = users.value.filter(user => user.id !== id)
  }
}

// 获取用户显示名，外呼确认框优先展示真实姓名。
const getDisplayName = (record) => {
  return record.realName || record.real_name || record.username || '-'
}

// 获取用户手机号，便于按钮禁用和后端调用前确认。
const getPhone = (record) => {
  return record.phone || ''
}

// 判断指定用户行是否正在提交外呼请求。
const isOutboundLoading = (id) => {
  return outboundLoadingIds.value.has(id)
}

// 设置单行外呼 loading 状态，替换 Set 引用以触发 Vue 响应式更新。
const setOutboundLoading = (id, value) => {
  const nextIds = new Set(outboundLoadingIds.value)
  if (value) {
    nextIds.add(id)
  } else {
    nextIds.delete(id)
  }
  outboundLoadingIds.value = nextIds
}

// 调用后端用户外呼接口，成功或失败都只提示结果，不影响当前列表数据。
const outboundCall = async (record) => {
  const phone = getPhone(record)
  if (!phone) {
    message.warning('该用户没有手机号，无法外呼')
    return
  }
  try {
    setOutboundLoading(record.id, true)
    const response = await axios.post(`/api/base/user/${record.id}/outbound-call`)
    const result = response.data || {}
    if (result.code && result.code !== 200) {
      throw new Error(result.message || '外呼提交失败')
    }
    const requestId = result.data?.requestId
    message.success(requestId ? `外呼任务已提交，请求ID：${requestId}` : '外呼任务已提交')
  } catch (error) {
    console.error('外呼提交失败:', error)
    message.error(error.response?.data?.message || error.message || '外呼提交失败')
  } finally {
    setOutboundLoading(record.id, false)
  }
}

const handleOk = async () => {
  try {
    await formRef.value?.validate()
    loading.value = true
    
    const submitData = {
      username: form.username,
      realName: form.realName,
      organizationId: form.organizationId,
      email: form.email,
      phone: form.phone,
      status: form.status
    }
    
    if (form.id) {
      await axios.put(`/api/base/user/${form.id}`, submitData)
      await loadUsers()
    } else {
      submitData.password = form.password
      await axios.post('/api/base/user', submitData)
      await loadUsers()
    }
    modalVisible.value = false
  } catch (error) {
    console.error('保存用户失败:', error)
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  modalVisible.value = false
}

onMounted(() => {
  loadUsers()
  loadOrganizations()
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
