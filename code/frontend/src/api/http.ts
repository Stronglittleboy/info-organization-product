import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络后重试')
    } else if (!error.response) {
      ElMessage.error('网络异常，无法连接服务器')
    } else {
      const status = error.response.status
      const serverMsg = error.response.data?.error?.message
      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          break
        case 403:
          ElMessage.error('没有操作权限')
          break
        case 404:
          ElMessage.error(serverMsg || '请求的资源不存在')
          break
        case 409:
          ElMessage.error(serverMsg || '数据已被其他操作修改，请刷新后重试')
          break
        case 422:
          ElMessage.error(serverMsg || '输入数据有误')
          break
        case 500:
          ElMessage.error(serverMsg || '服务器内部错误，请稍后重试')
          break
        default:
          ElMessage.error(serverMsg || `请求失败 (${status})`)
      }
    }
    return Promise.reject(error)
  }
)

export default http
