// 通用类型定义

// 主题相关
export type Theme = 'light' | 'dark' | 'auto'
export type Language = 'zh-CN' | 'en-US'

// 通知相关
export type NotificationType = 'success' | 'warning' | 'error' | 'info'
export type NotificationPosition = 'top-right' | 'top-left' | 'bottom-right' | 'bottom-left'

export interface Notification {
  id: string
  type: NotificationType
  title: string
  message?: string
  duration?: number
  position?: NotificationPosition
  showClose?: boolean
  onClick?: () => void
}

// 菜单/导航相关
export interface MenuItem {
  id: string
  title: string
  icon?: string
  path?: string
  children?: MenuItem[]
  badge?: string | number
  disabled?: boolean
  hidden?: boolean
  external?: boolean
}

// 面包屑导航
export interface BreadcrumbItem {
  title: string
  path?: string
  disabled?: boolean
}

// 表单相关
export interface FormRule {
  required?: boolean
  min?: number
  max?: number
  pattern?: RegExp
  validator?: (value: any) => boolean | string
  message?: string
  trigger?: 'blur' | 'change' | 'submit'
}

export interface FormField {
  name: string
  label: string
  type: 'text' | 'email' | 'password' | 'number' | 'tel' | 'url' | 'textarea' | 'select' | 'radio' | 'checkbox' | 'date' | 'file'
  placeholder?: string
  rules?: FormRule[]
  options?: Array<{ label: string; value: any }>
  disabled?: boolean
  readonly?: boolean
  hidden?: boolean
}

// 表格相关
export interface TableColumn {
  key: string
  title: string
  dataIndex: string
  width?: string | number
  align?: 'left' | 'center' | 'right'
  sortable?: boolean
  filterable?: boolean
  render?: (value: any, record: any, index: number) => any
  fixed?: 'left' | 'right'
}

export interface TableConfig {
  columns: TableColumn[]
  data: any[]
  loading?: boolean
  pagination?: {
    current: number
    pageSize: number
    total: number
    showSizeChanger?: boolean
    pageSizeOptions?: string[]
  }
  selection?: {
    type: 'checkbox' | 'radio'
    selectedRowKeys?: any[]
    onChange?: (selectedRowKeys: any[], selectedRows: any[]) => void
  }
  scroll?: {
    x?: string | number
    y?: string | number
  }
}

// 图表相关
export interface ChartData {
  labels: string[]
  datasets: Array<{
    label: string
    data: number[]
    backgroundColor?: string | string[]
    borderColor?: string | string[]
    borderWidth?: number
  }>
}

export interface ChartOptions {
  responsive?: boolean
  maintainAspectRatio?: boolean
  plugins?: {
    legend?: {
      display?: boolean
      position?: 'top' | 'bottom' | 'left' | 'right'
    }
    title?: {
      display?: boolean
      text?: string
    }
  }
  scales?: {
    x?: any
    y?: any
  }
}

// 文件相关
export interface FileInfo {
  name: string
  size: number
  type: string
  url?: string
  thumbnailUrl?: string
  status: 'uploading' | 'success' | 'error'
  percent?: number
}

// 地址相关
export interface Address {
  country?: string
  state?: string
  city?: string
  street?: string
  postal_code?: string
  full_address?: string
}

// 分页相关
export interface Pagination {
  current: number
  pageSize: number
  total: number
  showSizeChanger?: boolean
  showQuickJumper?: boolean
  showTotal?: boolean
  pageSizeOptions?: string[]
}

// 搜索过滤相关
export interface FilterOption {
  label: string
  value: any
  disabled?: boolean
}

export interface SearchFilters {
  keyword?: string
  status?: string
  category?: string
  dateRange?: [string, string]
  [key: string]: any
}

// 操作按钮相关
export interface ActionButton {
  label: string
  type?: 'primary' | 'secondary' | 'success' | 'warning' | 'danger'
  icon?: string
  onClick: () => void
  disabled?: boolean
  loading?: boolean
}

// 状态相关
export interface LoadingState {
  loading: boolean
  error?: string | null
  data?: any
}

// 配置相关
export interface AppConfig {
  name: string
  version: string
  api_base_url: string
  websocket_url: string
  support_email: string
  support_phone: string
  terms_url: string
  privacy_url: string
}