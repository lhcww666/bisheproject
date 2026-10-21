<template>
  <div class="app-container">
    <el-card v-if="!exam" v-loading="statusLoading" class="exam-status-card">
      <template #header>
        <div class="page-header">
          <span>安全考试</span>
          <el-button icon="Refresh" @click="loadStatus">刷新状态</el-button>
        </div>
      </template>

      <el-alert
        v-if="status.hasValidPass"
        class="exam-alert"
        type="success"
        :closable="false"
        show-icon
        :title="'你已通过安全考试，有效期至 ' + formatDate(status.latestValidUntil)"
      />
      <el-alert
        v-else-if="status.inProgressRecordId"
        class="exam-alert"
        type="warning"
        :closable="false"
        show-icon
        :title="'你有一场进行中的考试，截止时间 ' + formatDate(status.inProgressDeadline)"
      />
      <el-alert
        v-else
        class="exam-alert"
        type="info"
        :closable="false"
        show-icon
        title="参加安全考试并达到通过分数后，才能预约高风险实验室。"
      />

      <el-descriptions :column="2" border>
        <el-descriptions-item label="最近成绩">{{ status.latestScore ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="最近结果">
          <el-tag v-if="status.latestPassed === true" type="success">通过</el-tag>
          <el-tag v-else-if="status.latestPassed === false" type="danger">未通过</el-tag>
          <span v-else>—</span>
        </el-descriptions-item>
        <el-descriptions-item label="最近考试时间">{{ formatDate(status.latestExamTime) }}</el-descriptions-item>
        <el-descriptions-item label="通过有效期">{{ formatDate(status.latestValidUntil) }}</el-descriptions-item>
        <el-descriptions-item label="通过分数">{{ status.passScore ?? 80 }} 分</el-descriptions-item>
        <el-descriptions-item label="考试时长">{{ status.durationMinutes ?? 30 }} 分钟</el-descriptions-item>
      </el-descriptions>

      <div class="exam-actions">
        <el-button type="primary" :loading="starting || statusLoading" :disabled="statusLoading || !status.canStart" @click="startExam">
          {{ startButtonText }}
        </el-button>
        <span class="exam-hint">考试从题库随机抽题，提交后自动判分。</span>
      </div>
    </el-card>

    <el-card v-else class="exam-paper-card">
      <template #header>
        <div class="page-header">
          <span>安全考试 · {{ exam.paperNo }}</span>
          <div class="exam-timer" :class="{ danger: remainingSeconds <= 300 }">
            剩余 {{ formatDuration(remainingSeconds) }}
          </div>
        </div>
      </template>

      <el-alert
        class="exam-alert"
        type="info"
        :closable="false"
        show-icon
        :title="'共 ' + exam.questions.length + ' 题，' + exam.passScore + ' 分通过，考试时间 ' + exam.durationMinutes + ' 分钟'"
      />

      <div v-for="(question, index) in exam.questions" :key="question.id" class="question-block">
        <div class="question-title">
          <span>{{ index + 1 }}. {{ question.content }}</span>
          <div>
            <el-tag size="small" type="info">{{ typeLabel(question.questionType) }}</el-tag>
            <el-tag size="small" type="warning">{{ question.score }} 分</el-tag>
          </div>
        </div>

        <el-radio-group
          v-if="question.questionType !== 'MULTIPLE'"
          :model-value="answers[question.id]?.[0]"
          class="option-group"
          @update:model-value="value => setSingleAnswer(question.id, value)"
        >
          <el-radio
            v-for="(option, optionIndex) in question.options"
            :key="optionIndex"
            :label="optionKey(optionIndex)"
            class="option-item"
          >
            {{ optionKey(optionIndex) }}. {{ optionText(option) }}
          </el-radio>
        </el-radio-group>

        <el-checkbox-group v-else v-model="answers[question.id]" class="option-group">
          <el-checkbox
            v-for="(option, optionIndex) in question.options"
            :key="optionIndex"
            :label="optionKey(optionIndex)"
            class="option-item"
          >
            {{ optionKey(optionIndex) }}. {{ optionText(option) }}
          </el-checkbox>
        </el-checkbox-group>
      </div>

      <div class="exam-footer">
        <el-button type="primary" :loading="submitting" @click="confirmSubmit">提交试卷</el-button>
        <el-button @click="cancelExam">暂时离开</el-button>
        <span class="exam-hint">离开后可在截止时间前继续作答。</span>
      </div>
    </el-card>

    <el-dialog title="考试结果" v-model="resultOpen" width="460px" append-to-body destroy-on-close>
      <div v-if="result" class="result-box">
        <el-result
          :icon="result.passed ? 'success' : 'error'"
          :title="result.passed ? '考试通过' : '未通过'"
          :sub-title="'得分 ' + result.score + ' / 100，答对 ' + result.correctCount + ' / ' + result.totalQuestions + ' 题'"
        >
          <template #extra>
            <p v-if="result.passed">有效期至 {{ formatDate(result.validUntil) }}</p>
            <p v-else>达到 {{ result.passScore }} 分即可通过，可以重新考试。</p>
          </template>
        </el-result>
      </div>
      <template #footer>
        <el-button type="primary" @click="closeResult">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="SafetyExam">
import { getSafetyExamStatus, startSafetyExam, submitSafetyExam } from "@/api/module/safetyExam";

const { proxy } = getCurrentInstance();
const starting = ref(false);
const submitting = ref(false);
const exam = ref(null);
const status = ref({ canStart: false });
const statusLoading = ref(true);
const answers = reactive({});
const remainingSeconds = ref(0);
const result = ref(null);
const resultOpen = ref(false);
let timer = null;

const startButtonText = computed(() => {
  if (statusLoading.value) return "加载中...";
  if (status.value.hasValidPass) return "重新考试";
  if (status.value.inProgressRecordId) return "继续考试";
  return "开始考试";
});

onMounted(loadStatus);
onBeforeUnmount(clearTimer);

function loadStatus() {
  statusLoading.value = true;
  getSafetyExamStatus().then(response => {
    status.value = response.data || { canStart: false };
  }).finally(() => {
    statusLoading.value = false;
  });
}

function startExam() {
  starting.value = true;
  startSafetyExam().then(response => {
    exam.value = response.data;
    Object.keys(answers).forEach(key => delete answers[key]);
    (exam.value.questions || []).forEach(question => {
      answers[question.id] = [];
    });
    startTimer();
  }).finally(() => {
    starting.value = false;
  });
}

function startTimer() {
  clearTimer();
  updateRemaining();
  timer = setInterval(() => {
    updateRemaining();
    if (remainingSeconds.value <= 0) {
      clearTimer();
      submitExam(true);
    }
  }, 1000);
}

function updateRemaining() {
  if (!exam.value || !exam.value.deadline) {
    remainingSeconds.value = 0;
    return;
  }
  const deadline = new Date(exam.value.deadline.replace(/-/g, '/')).getTime();
  remainingSeconds.value = Math.max(0, Math.floor((deadline - Date.now()) / 1000));
}

function clearTimer() {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
}

function setSingleAnswer(questionId, value) {
  answers[questionId] = value ? [value] : [];
}

function optionKey(index) {
  return String.fromCharCode(65 + index);
}

function optionText(option) {
  return String(option || "").replace(/^\s*[A-Z][.、:：]\s*/, "");
}

function typeLabel(type) {
  if (type === "MULTIPLE") return "多选题";
  if (type === "JUDGE") return "判断题";
  return "单选题";
}

function confirmSubmit() {
  const unanswered = (exam.value.questions || []).filter(question => !answers[question.id] || answers[question.id].length === 0).length;
  const message = unanswered > 0 ? `还有 ${unanswered} 题未作答，确认提交吗？` : "确认提交试卷吗？提交后不能修改。";
  proxy.$modal.confirm(message).then(() => submitExam(false)).catch(() => {});
}

function submitExam(auto) {
  if (!exam.value || submitting.value) return;
  submitting.value = true;
  clearTimer();
  const payload = {
    recordId: exam.value.recordId,
    answers: (exam.value.questions || []).map(question => ({
      questionId: question.id,
      answer: answers[question.id] || []
    }))
  };
  submitSafetyExam(payload).then(response => {
    result.value = response.data;
    resultOpen.value = true;
    exam.value = null;
    loadStatus();
    if (auto) {
      proxy.$modal.msgWarning("考试时间到，已自动交卷");
    }
  }).catch(() => {
    if (auto) {
      loadStatus();
    }
  }).finally(() => {
    submitting.value = false;
  });
}

function cancelExam() {
  proxy.$modal.confirm("暂时离开后，考试仍在计时，确认离开吗？").then(() => {
    clearTimer();
    exam.value = null;
    loadStatus();
  }).catch(() => {});
}

function closeResult() {
  resultOpen.value = false;
  result.value = null;
}

function formatDuration(seconds) {
  const safe = Math.max(0, seconds || 0);
  const minutes = Math.floor(safe / 60);
  const rest = safe % 60;
  return `${String(minutes).padStart(2, "0")}:${String(rest).padStart(2, "0")}`;
}

function formatDate(value) {
  return value ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") : "—";
}
</script>

<style lang="scss" scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; }
.exam-alert { margin-bottom: 14px; }
.exam-actions { margin-top: 18px; display: flex; align-items: center; gap: 12px; }
.exam-hint { color: #909399; font-size: 13px; }
.exam-timer { font-size: 18px; font-weight: 600; color: #409eff; }
.exam-timer.danger { color: #f56c6c; }
.question-block { padding: 16px 0; border-bottom: 1px solid #ebeef5; }
.question-title { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 12px; font-weight: 600; line-height: 1.6; }
.question-block > .option-group {
  width: 100%;
  display: flex !important;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  padding-left: 0;
}
.option-group :deep(.option-item) {
  display: flex;
  align-items: flex-start;
  width: 100%;
  min-height: 40px;
  height: auto;
  margin-right: 0;
  padding: 9px 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  line-height: 1.6;
  white-space: normal;
}
.option-group :deep(.option-item:hover) { border-color: #b3d8ff; background: #f5f9ff; }
.option-group :deep(.el-radio__label),
.option-group :deep(.el-checkbox__label) {
  white-space: normal;
  word-break: break-word;
  line-height: 1.6;
}
.exam-footer { margin-top: 20px; display: flex; align-items: center; gap: 12px; }
.result-box { padding: 4px 0; text-align: center; }
</style>
