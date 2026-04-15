<script setup lang="ts">
import type { UploadFileInfo } from 'naive-ui';

defineOptions({
  name: 'BatchUploadDialog'
});

const loading = ref(false);
const visible = defineModel<boolean>('visible', { default: false });

const authStore = useAuthStore();
const store = useKnowledgeBaseStore();

const { formRef, validate, restoreValidation } = useNaiveForm();
const { defaultRequiredRule } = useFormRules();

const fileInputRef = ref<HTMLInputElement | null>(null);
const folderInputRef = ref<HTMLInputElement | null>(null);

const model = ref<Api.KnowledgeBase.Form>(createDefaultModel());

function createDefaultModel(): Api.KnowledgeBase.Form {
  return {
    orgTag: null,
    orgTagName: '',
    isPublic: false,
    fileList: []
  };
}

const rules = ref<FormRules>({
  orgTag: defaultRequiredRule,
  isPublic: defaultRequiredRule,
  fileList: defaultRequiredRule
});

function close() {
  visible.value = false;
}

function onUpdate(option: unknown) {
  if (option) model.value.orgTagName = (option as Api.OrgTag.Item).name;
}

function sortFiles(files: File[]) {
  return [...files].sort((a, b) => {
    const pathA = a.webkitRelativePath || a.name;
    const pathB = b.webkitRelativePath || b.name;
    return pathA.localeCompare(pathB);
  });
}

function setSelectedFiles(files: File[]) {
  model.value.fileList = sortFiles(files).map((file, index) => ({
    id: `${file.name}-${file.lastModified}-${index}`,
    name: file.webkitRelativePath || file.name,
    status: 'finished',
    percentage: 100,
    file
  })) as UploadFileInfo[];
}

function openFilePicker() {
  fileInputRef.value?.click();
}

function openFolderPicker() {
  folderInputRef.value?.click();
}

function handleFileInput(event: Event) {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []);
  if (files.length > 0) setSelectedFiles(files);
  input.value = '';
}

async function handleSubmit() {
  await validate();
  loading.value = true;
  await store.enqueueBatchUpload(model.value);
  loading.value = false;
  close();
}

watch(visible, () => {
  if (visible.value) {
    model.value = createDefaultModel();
    restoreValidation();
  }
});
</script>

<template>
  <NModal
    v-model:show="visible"
    preset="dialog"
    title="批量上传"
    :show-icon="false"
    :mask-closable="false"
    class="w-560px!"
    @positive-click="handleSubmit"
  >
    <NForm ref="formRef" :model="model" :rules="rules" label-placement="left" :label-width="100" mt-10>
      <NFormItem v-if="authStore.isAdmin" label="组织标签" path="orgTag">
        <OrgTagCascader v-model:value="model.orgTag" @change="onUpdate" />
      </NFormItem>
      <NFormItem v-else label="组织标签" path="orgTag">
        <TheSelect
          v-model:value="model.orgTag"
          url="/users/org-tags"
          key-field="orgTagDetails"
          label-field="name"
          value-field="tagId"
          @change="onUpdate"
        />
      </NFormItem>

      <NFormItem label="是否公开" path="isPublic">
        <NRadioGroup v-model:value="model.isPublic" name="radiogroup">
          <NSpace :size="16">
            <NRadio :value="true">公开</NRadio>
            <NRadio :value="false">私有</NRadio>
          </NSpace>
        </NRadioGroup>
      </NFormItem>

      <NFormItem label="选择内容" path="fileList">
        <NSpace vertical :size="12">
          <NSpace :size="12">
            <NButton @click="openFilePicker">选择多个文件</NButton>
            <NButton @click="openFolderPicker">选择文件夹</NButton>
          </NSpace>

          <input
            ref="fileInputRef"
            type="file"
            multiple
            class="hidden"
            @change="handleFileInput"
          >

          <input
            ref="folderInputRef"
            type="file"
            webkitdirectory
            directory
            multiple
            class="hidden"
            @change="handleFileInput"
          >

          <div v-if="model.fileList.length" class="rounded-8px bg-#f8fafc px-12px py-10px text-13px text-#475569">
            已选择 {{ model.fileList.length }} 个文件，提交后会按路径顺序进入上传队列。
          </div>
        </NSpace>
      </NFormItem>
    </NForm>

    <template #action>
      <NSpace :size="16">
        <NButton @click="close">取消</NButton>
        <NButton type="primary" @click="handleSubmit">开始批量上传</NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<style scoped></style>
