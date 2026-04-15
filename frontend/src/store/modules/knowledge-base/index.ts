import { REQUEST_ID_KEY } from '~/packages/axios/src';
import { nanoid } from '~/packages/utils/src';

export const useKnowledgeBaseStore = defineStore(SetupStoreId.KnowledgeBase, () => {
  const tasks = ref<Api.KnowledgeBase.UploadTask[]>([]);
  const activeUploads = ref<Set<string>>(new Set());

  async function uploadChunk(task: Api.KnowledgeBase.UploadTask): Promise<boolean> {
    const totalChunks = Math.ceil(task.totalSize / chunkSize);

    const chunkStart = task.chunkIndex * chunkSize;
    const chunkEnd = Math.min(chunkStart + chunkSize, task.totalSize);
    const chunk = task.file.slice(chunkStart, chunkEnd);

    task.chunk = chunk;
    const requestId = nanoid();
    task.requestIds ??= [];
    task.requestIds.push(requestId);

    const { error, data } = await request<Api.KnowledgeBase.Progress>({
      url: '/upload/chunk',
      method: 'POST',
      data: {
        file: task.chunk,
        fileMd5: task.fileMd5,
        chunkIndex: task.chunkIndex,
        totalSize: task.totalSize,
        fileName: task.fileName,
        orgTag: task.orgTag,
        isPublic: task.isPublic ?? false
      },
      headers: {
        'Content-Type': 'multipart/form-data',
        [REQUEST_ID_KEY]: requestId
      },
      timeout: 10 * 60 * 1000
    });

    task.requestIds = task.requestIds.filter(id => id !== requestId);

    if (error) return false;

    const updatedTask = tasks.value.find(t => t.fileMd5 === task.fileMd5)!;
    updatedTask.uploadedChunks = data.uploaded;
    updatedTask.progress = Number.parseFloat(data.progress.toFixed(2));

    if (data.uploaded.length === totalChunks) {
      const success = await mergeFile(task);
      if (!success) return false;
    }

    return true;
  }

  async function mergeFile(task: Api.KnowledgeBase.UploadTask) {
    try {
      const { error } = await request({
        url: '/upload/merge',
        method: 'POST',
        data: { fileMd5: task.fileMd5, fileName: task.fileName }
      });

      if (error) return false;

      const index = tasks.value.findIndex(t => t.fileMd5 === task.fileMd5);
      tasks.value[index].status = UploadStatus.Completed;
      return true;
    } catch {
      return false;
    }
  }

  async function enqueueFile(file: File, form: Pick<Api.KnowledgeBase.Form, 'isPublic' | 'orgTag' | 'orgTagName'>) {
    const md5 = await calculateMD5(file);

    const existingTask = tasks.value.find(t => t.fileMd5 === md5);
    if (existingTask) {
      if (existingTask.status === UploadStatus.Completed) {
        window.$message?.error('文件已存在');
        return;
      }

      if (existingTask.status === UploadStatus.Pending || existingTask.status === UploadStatus.Uploading) {
        window.$message?.error('文件正在上传中');
        return;
      }

      if (existingTask.status === UploadStatus.Break) {
        existingTask.status = UploadStatus.Pending;
        startUpload();
        return;
      }
    }

    const newTask: Api.KnowledgeBase.UploadTask = {
      file,
      chunk: null,
      chunkIndex: 0,
      fileMd5: md5,
      fileName: file.name,
      totalSize: file.size,
      public: form.isPublic,
      isPublic: form.isPublic,
      uploadedChunks: [],
      progress: 0,
      status: UploadStatus.Pending,
      orgTag: form.orgTag
    };

    newTask.orgTagName = form.orgTagName ?? null;
    tasks.value.push(newTask);
    startUpload();
  }

  async function enqueueUpload(form: Api.KnowledgeBase.Form) {
    const file = form.fileList?.[0]?.file;
    if (!file) return;

    await enqueueFile(file, form);
  }

  async function enqueueBatchUpload(form: Api.KnowledgeBase.Form) {
    const files = (form.fileList ?? [])
      .map(item => item.file)
      .filter((file): file is File => Boolean(file))
      .sort((a, b) => {
        const pathA = a.webkitRelativePath || a.name;
        const pathB = b.webkitRelativePath || b.name;
        return pathA.localeCompare(pathB);
      });

    for (const file of files) {
      // eslint-disable-next-line no-await-in-loop
      await enqueueFile(file, form);
    }
  }

  async function startUpload() {
    if (activeUploads.value.size >= 3) return;

    const pendingTasks = tasks.value.filter(
      t => t.status === UploadStatus.Pending && !activeUploads.value.has(t.fileMd5)
    );

    if (pendingTasks.length === 0) return;

    const task = pendingTasks[0];
    task.status = UploadStatus.Uploading;
    activeUploads.value.add(task.fileMd5);

    const totalChunks = Math.ceil(task.totalSize / chunkSize);

    try {
      if (task.uploadedChunks.length === totalChunks) {
        const success = await mergeFile(task);
        if (!success) throw new Error('文件合并失败');
      }

      for (let i = 0; i < totalChunks; i += 1) {
        if (!task.uploadedChunks.includes(i)) {
          task.chunkIndex = i;
          // eslint-disable-next-line no-await-in-loop
          const success = await uploadChunk(task);
          if (!success) throw new Error('分片上传失败');
        }
      }
    } catch (e) {
      console.error('%c [ upload error ]-168', 'font-size:16px; background:#94cc97; color:#d8ffdb;', e);
      const index = tasks.value.findIndex(t => t.fileMd5 === task.fileMd5);
      tasks.value[index].status = UploadStatus.Break;
    } finally {
      activeUploads.value.delete(task.fileMd5);
      startUpload();
    }
  }

  return {
    tasks,
    activeUploads,
    enqueueUpload,
    enqueueBatchUpload,
    startUpload
  };
});
