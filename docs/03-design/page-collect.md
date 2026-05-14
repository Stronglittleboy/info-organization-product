# 收集页面设计 - 方案 B（智能识别）

## 设计理念

**核心原则：** 一个输入框，智能识别类型，最快速度完成收集。

---

## 页面结构

```
┌─────────────────────────────────────┐
│                                     │
│  收集素材                            │
│                                     │
│  ┌───────────────────────────────┐  │
│  │                               │  │
│  │  粘贴文本、图片或链接...       │  │
│  │                               │  │
│  │  或点击上传图片 📷             │  │
│  │                               │  │
│  └───────────────────────────────┘  │
│                                     │
└─────────────────────────────────────┘
```

---

## 交互流程

### 1. 初始状态

- 显示一个大的输入区域
- 提示文字："粘贴文本、图片或链接..."
- 底部有"或点击上传图片 📷"

### 2. 粘贴文本

```
用户粘贴：聚合根负责维护业务不变性...
↓
系统识别：文本类型
↓
展开表单：
┌───────────────────────────────┐
│ 📝 文本                        │
├───────────────────────────────┤
│ 聚合根负责维护业务不变性...    │
├───────────────────────────────┤
│ 你的思考（可选）               │
│ ┌─────────────────────────┐   │
│ │                         │   │
│ └─────────────────────────┘   │
│                               │
│ ⚙️ 更多选项（可选）            │
│                               │
│ ┌─────────────────────────┐   │
│ │        保存             │   │
│ └─────────────────────────┘   │
└───────────────────────────────┘
```

### 3. 粘贴 URL

```
用户粘贴：https://example.com/article
↓
系统识别：URL 类型
↓
自动提取元数据（显示加载动画）
↓
展开表单：
┌───────────────────────────────┐
│ 🔗 链接                        │
├───────────────────────────────┤
│ DDD 领域驱动设计实践           │
│ 本文介绍了 DDD 的核心概念...   │
├───────────────────────────────┤
│ 你的思考（必填）*              │
│ ┌─────────────────────────┐   │
│ │ 这篇文章讲了...          │   │
│ └─────────────────────────┘   │
│                               │
│ ⚙️ 更多选项（可选）            │
│                               │
│ ┌─────────────────────────┐   │
│ │        保存             │   │
│ └─────────────────────────┘   │
└───────────────────────────────┘
```

### 4. 上传图片

```
用户点击"上传图片"或拖拽图片
↓
显示图片预览
↓
自动 OCR 识别（显示加载动画）
↓
展开表单：
┌───────────────────────────────┐
│ 📷 图片                        │
├───────────────────────────────┤
│ [图片预览]                     │
├───────────────────────────────┤
│ 识别到的文字：                 │
│ 用户登录、密码输入、提交按钮    │
├───────────────────────────────┤
│ 这张图说明了什么？（必填）*     │
│ ┌─────────────────────────┐   │
│ │ 这是竞品的登录页面...     │   │
│ └─────────────────────────┘   │
│                               │
│ ⚙️ 更多选项（可选）            │
│                               │
│ ┌─────────────────────────┐   │
│ │        保存             │   │
│ └─────────────────────────┘   │
└───────────────────────────────┘
```

---

## 智能识别逻辑

### 前端 JavaScript

```javascript
// 监听粘贴事件
inputArea.addEventListener('paste', async (e) => {
  const items = e.clipboardData.items;
  
  for (let item of items) {
    // 图片识别
    if (item.type.startsWith('image/')) {
      e.preventDefault();
      const file = item.getAsFile();
      await handleImageUpload(file);
      return;
    }
  }
  
  // 文本识别
  const text = e.clipboardData.getData('text');
  
  // URL 识别
  if (/^https?:\/\/.+/.test(text.trim())) {
    e.preventDefault();
    await handleUrlInput(text.trim());
    return;
  }
  
  // 默认文本
  handleTextInput(text);
});

// 监听拖拽事件
inputArea.addEventListener('drop', async (e) => {
  e.preventDefault();
  const files = e.dataTransfer.files;
  
  if (files.length > 0 && files[0].type.startsWith('image/')) {
    await handleImageUpload(files[0]);
  }
});
```

---

## 组件设计

### CollectInput.vue

```vue
<template>
  <div class="collect-container">
    <h1 class="page-title">收集素材</h1>
    
    <!-- 初始输入区域 -->
    <div v-if="!contentDetected" class="input-card">
      <textarea
        ref="inputArea"
        class="smart-input"
        placeholder="粘贴文本、图片或链接..."
        @paste="handlePaste"
        @drop="handleDrop"
        @dragover.prevent
      ></textarea>
      
      <div class="upload-hint">
        或点击上传图片 
        <label class="upload-trigger">
          📷
          <input type="file" accept="image/*" @change="handleFileSelect" hidden>
        </label>
      </div>
    </div>
    
    <!-- 文本表单 -->
    <TextForm v-if="contentType === 'text'" :content="content" @save="handleSave" @cancel="reset" />
    
    <!-- 图片表单 -->
    <ImageForm v-if="contentType === 'image'" :file="imageFile" @save="handleSave" @cancel="reset" />
    
    <!-- URL 表单 -->
    <UrlForm v-if="contentType === 'url'" :url="url" @save="handleSave" @cancel="reset" />
  </div>
</template>

<script setup>
import { ref } from 'vue';

const contentDetected = ref(false);
const contentType = ref(null);
const content = ref('');
const imageFile = ref(null);
const url = ref('');

const handlePaste = async (e) => {
  const items = e.clipboardData.items;
  
  // 检查是否是图片
  for (let item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault();
      imageFile.value = item.getAsFile();
      contentType.value = 'image';
      contentDetected.value = true;
      return;
    }
  }
  
  // 检查是否是 URL
  const text = e.clipboardData.getData('text');
  if (/^https?:\/\/.+/.test(text.trim())) {
    e.preventDefault();
    url.value = text.trim();
    contentType.value = 'url';
    contentDetected.value = true;
    return;
  }
  
  // 默认文本
  content.value = text;
  contentType.value = 'text';
  contentDetected.value = true;
};

const reset = () => {
  contentDetected.value = false;
  contentType.value = null;
  content.value = '';
  imageFile.value = null;
  url.value = '';
};
</script>
```

---

## 样式设计

```css
.collect-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--gray-900);
  margin-bottom: 24px;
}

.input-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.smart-input {
  width: 100%;
  min-height: 200px;
  padding: 16px;
  border: 2px dashed var(--gray-200);
  border-radius: 8px;
  font-size: 16px;
  resize: vertical;
  transition: all 0.2s;
}

.smart-input:focus {
  outline: none;
  border-color: var(--primary);
  border-style: solid;
}

.upload-hint {
  margin-top: 16px;
  text-align: center;
  color: var(--gray-400);
  font-size: 14px;
}

.upload-trigger {
  cursor: pointer;
  font-size: 20px;
  transition: transform 0.2s;
}

.upload-trigger:hover {
  transform: scale(1.2);
}
```

---

## 加载状态

### OCR 识别中

```
┌───────────────────────────────┐
│ 📷 图片                        │
├───────────────────────────────┤
│ [图片预览]                     │
├───────────────────────────────┤
│ 正在识别文字...                │
│ [加载动画]                     │
└───────────────────────────────┘
```

### URL 元数据提取中

```
┌───────────────────────────────┐
│ 🔗 链接                        │
├───────────────────────────────┤
│ 正在获取网页信息...            │
│ [加载动画]                     │
└───────────────────────────────┘
```

---

## 错误处理

### OCR 失败

```
┌───────────────────────────────┐
│ 📷 图片                        │
├───────────────────────────────┤
│ [图片预览]                     │
├───────────────────────────────┤
│ ⚠️ 文字识别失败                │
│ 请手动输入图片说明             │
├───────────────────────────────┤
│ 这张图说明了什么？（必填）*     │
│ ┌─────────────────────────┐   │
│ │                         │   │
│ └─────────────────────────┘   │
└───────────────────────────────┘
```

### URL 提取失败

```
┌───────────────────────────────┐
│ 🔗 链接                        │
├───────────────────────────────┤
│ ⚠️ 无法获取网页信息            │
│ 请手动输入                     │
├───────────────────────────────┤
│ 你的思考（必填）*              │
│ ┌─────────────────────────┐   │
│ │                         │   │
│ └─────────────────────────┘   │
└───────────────────────────────┘
```

---

## 更多选项（折叠）

点击"⚙️ 更多选项"展开：

```
┌───────────────────────────────┐
│ ⚙️ 更多选项 ▼                  │
├───────────────────────────────┤
│ 来源类型                       │
│ ┌─────────────────────────┐   │
│ │ 网页 ▾                   │   │
│ └─────────────────────────┘   │
│                               │
│ 来源标题                       │
│ ┌─────────────────────────┐   │
│ │ 竞品分析                 │   │
│ └─────────────────────────┘   │
│                               │
│ 专题                          │
│ ┌─────────────────────────┐   │
│ │ 产品设计 ▾                │   │
│ └─────────────────────────┘   │
└───────────────────────────────┘
```

---

## 快捷键

- `Ctrl/Cmd + V` - 粘贴
- `Ctrl/Cmd + Enter` - 保存
- `Esc` - 取消/重置
