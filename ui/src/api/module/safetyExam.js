import request from '@/utils/request'

// 当前学生安全考试状态
export function getSafetyExamStatus() {
  return request({
    url: '/module/safetyExam/status',
    method: 'get'
  })
}

// 开始或继续考试
export function startSafetyExam() {
  return request({
    url: '/module/safetyExam/start',
    method: 'post'
  })
}

// 交卷
export function submitSafetyExam(data) {
  return request({
    url: '/module/safetyExam/submit',
    method: 'post',
    data: data
  })
}
