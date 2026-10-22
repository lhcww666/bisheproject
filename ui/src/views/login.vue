<template>
  <div class="login-shell">
    <section class="brand-panel">
      <div class="brand-glow"></div>
      <div class="brand-header">
        <img :src="brandLogo" alt="实验室智能管理平台" class="brand-logo" />
        <div>
          <strong>实验室智能管理平台</strong>
          <span>LABORATORY INTELLIGENCE</span>
        </div>
      </div>

      <div class="brand-copy">
        <span class="eyebrow">AI AGENT · LAB SAFETY</span>
        <h1>校园实验室<br /><em>智能预约与安全管理平台</em></h1>
        <p>让实验室预约、安全准入、审核管理和智能问答形成一个可信闭环。</p>
        <div class="feature-list">
          <span><i></i>实验室预约</span>
          <span><i></i>安全考试</span>
          <span><i></i>智能助手</span>
        </div>
      </div>

      <div class="brand-footer">
        <span>安全准入</span>
        <span>数据可追溯</span>
        <span>AI 可解释</span>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-card">
        <div class="mobile-brand">
          <img :src="brandLogo" alt="实验室智能管理平台" />
          <span>实验室智能管理平台</span>
        </div>
        <div class="login-heading">
          <span class="login-kicker">WELCOME BACK</span>
          <h2>欢迎登录</h2>
          <p>学生、教师与管理员统一入口</p>
        </div>

        <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              type="text"
              size="large"
              auto-complete="username"
              placeholder="请输入账号"
            >
              <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              size="large"
              auto-complete="current-password"
              placeholder="请输入密码"
              @keyup.enter="handleLogin"
            >
              <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="code" v-if="captchaEnabled">
            <div class="captcha-row">
              <el-input
                v-model="loginForm.code"
                size="large"
                auto-complete="off"
                placeholder="请输入验证码"
                @keyup.enter="handleLogin"
              >
                <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
              </el-input>
              <button type="button" class="login-code" aria-label="刷新验证码" @click="getCode">
                <img :src="codeUrl" alt="验证码" class="login-code-img" />
              </button>
            </div>
          </el-form-item>
          <div class="login-options">
            <el-checkbox v-model="loginForm.rememberMe">记住账号</el-checkbox>
            <span>安全加密登录</span>
          </div>
          <el-button
            :loading="loading"
            size="large"
            type="primary"
            class="login-button"
            @click.prevent="handleLogin"
          >
            <span v-if="!loading">进入实验室平台</span>
            <span v-else>正在登录...</span>
          </el-button>
          <div class="register-link" v-if="register">
            <router-link class="link-type" :to="'/register'">立即注册</router-link>
          </div>
        </el-form>

        <p class="login-tip">账号由实验室管理员统一分配，请勿与他人共享密码。</p>
      </div>
      <footer class="el-login-footer">© 2026 校园实验室智能预约与安全管理平台</footer>
    </section>

    <el-dialog
      v-model="smsDialogVisible"
      title="请输入手机验证码"
      width="350px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      center
    >
      <el-form
        :model="loginForm"
        :rules="smsRules"
        ref="loginFormRef"
        label-width="80px"
      >
        <el-form-item label="验证码" prop="smsCode">
          <el-input
            v-model="loginForm.smsCode"
            placeholder="请输入收到的验证码"
          ></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelSms">取消</el-button>
        <el-button type="primary" @click="handleLoginBySms">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { getCodeImg } from "@/api/login";
import Cookies from "js-cookie";
import useUserStore from '@/store/modules/user'
import brandLogo from "@/assets/logo/lab-logo.svg";

const userStore = useUserStore()
const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();

const loginForm = ref({
  username: "",
  password: "",
  rememberMe: false,
  code: "",
  uuid: "",
  smsCode: ""
});

const smsRules = {
  smsCode: [{ required: true, trigger: "blur", message: "请输入验证码" }]
};

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
};

const codeUrl = ref("");
const loading = ref(false);
// 验证码开关
const captchaEnabled = ref(true);
// 注册开关
const register = ref(false);
const redirect = ref(undefined);
const smsDialogVisible = ref(false);
const secondaryVerificationEnabled = import.meta.env.VITE_APP_SECONDARY_VERIFICATION_ENABLED === 'true';

watch(route, (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect;
}, { immediate: true });

function cancelSms() {
  smsDialogVisible.value = false;
  loginForm.value.smsCode = "";
}

function handleLoginBySms() {
  // 判断 loginForm.smsCode 是否为6位数字
  const smsCode = loginForm.value.smsCode;
  if (!/^\d{6}$/.test(smsCode)) {
    // 尝试使用 proxy.$modal，如果不存在则 fallback
    if (proxy.$modal) {
       proxy.$modal.msgError("请输入6位数字的短信验证码");
    } else {
       // Simple fallback or assume Element Plus auto-import
       console.error("请输入6位数字的短信验证码");
       // Consider importing ElMessage if needed, but for now relying on existing setup
    }
    return;
  }
  handleLogin();
}

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true;
      // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 });
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 });
      } else {
        // 否则移除
        Cookies.remove("username");
        Cookies.remove("password");
        Cookies.remove("rememberMe");
      }
      // 调用action的登录方法
      userStore.login(loginForm.value).then(() => {
        const query = route.query;
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur];
          }
          return acc;
        }, {});
        router.push({ path: redirect.value || "/", query: otherQueryParams });
      }).catch((e) => {
        loading.value = false;
        if (e === "sms" && secondaryVerificationEnabled) {
          smsDialogVisible.value = true;
        } else {
          // 重新获取验证码
          if (captchaEnabled.value) {
            getCode();
          }
        }
      });
    }
  });
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled;
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img;
      loginForm.value.uuid = res.uuid;
    }
  });
}

function getCookie() {
  const username = Cookies.get("username");
  const rememberMe = Cookies.get("rememberMe");
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: "",
    rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
  };
}

getCode();
getCookie();
</script>

<style lang="scss" scoped>
.login-shell {
  min-height: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(430px, 0.8fr);
  background: #f3f7fb;
}

.brand-panel {
  position: relative;
  overflow: hidden;
  min-height: 100vh;
  padding: 38px 54px 34px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #fff;
  background:
    linear-gradient(105deg, rgba(4, 17, 30, 0.94) 0%, rgba(7, 40, 59, 0.72) 53%, rgba(8, 75, 85, 0.2) 100%),
    url("../assets/images/lab-login-hero.svg") center / cover no-repeat;
}

.brand-panel::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 15% 80%, rgba(34, 211, 238, 0.18), transparent 28%),
    linear-gradient(180deg, transparent 70%, rgba(2, 12, 23, 0.44));
}

.brand-glow {
  position: absolute;
  width: 360px;
  height: 360px;
  right: -160px;
  top: -140px;
  border-radius: 50%;
  background: rgba(45, 212, 191, 0.22);
  filter: blur(20px);
}

.brand-header,
.brand-copy,
.brand-footer {
  position: relative;
  z-index: 1;
}

.brand-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-logo {
  width: 46px;
  height: 46px;
  filter: drop-shadow(0 8px 18px rgba(14, 165, 233, 0.35));
}

.brand-header strong,
.brand-header span {
  display: block;
}

.brand-header strong {
  font-size: 19px;
  letter-spacing: 0.06em;
}

.brand-header span {
  margin-top: 4px;
  color: rgba(186, 230, 253, 0.68);
  font-size: 10px;
  letter-spacing: 0.18em;
}

.brand-copy {
  max-width: 720px;
  margin: auto 0;
  padding: 6vh 0;
}

.eyebrow {
  display: inline-flex;
  padding: 7px 12px;
  border: 1px solid rgba(125, 211, 252, 0.34);
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.1);
  color: #7dd3fc;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.16em;
}

.brand-copy h1 {
  margin: 24px 0 18px;
  color: #fff;
  font-size: clamp(34px, 4vw, 54px);
  font-weight: 700;
  line-height: 1.22;
  letter-spacing: -0.025em;
}

.brand-copy h1 em {
  color: #7dd3fc;
  font-style: normal;
}

.brand-copy > p {
  max-width: 580px;
  margin: 0;
  color: rgba(226, 240, 248, 0.72);
  font-size: 15px;
  line-height: 1.9;
}

.feature-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 28px;
}

.feature-list span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 13px;
  border: 1px solid rgba(148, 215, 235, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.055);
  color: rgba(240, 249, 255, 0.86);
  font-size: 13px;
}

.feature-list i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #5eead4;
  box-shadow: 0 0 12px rgba(94, 234, 212, 0.9);
}

.brand-footer {
  display: flex;
  gap: 24px;
  color: rgba(186, 230, 253, 0.55);
  font-size: 11px;
  letter-spacing: 0.12em;
}

.login-panel {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 44px 34px 60px;
  background:
    radial-gradient(circle at 80% 15%, rgba(56, 189, 248, 0.12), transparent 25%),
    linear-gradient(145deg, #f8fbff, #edf4f8);
}

.login-card {
  width: min(430px, 100%);
  padding: 38px 38px 28px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 28px 70px rgba(15, 49, 73, 0.14);
}

.mobile-brand {
  display: none;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
  color: #0f3f57;
  font-weight: 700;
}

.mobile-brand img {
  width: 38px;
  height: 38px;
}

.login-heading {
  margin-bottom: 26px;
}

.login-kicker {
  color: #0f9fb3;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.login-heading h2 {
  margin: 9px 0 7px;
  color: #102d3e;
  font-size: 28px;
  letter-spacing: -0.02em;
}

.login-heading p {
  margin: 0;
  color: #8093a1;
  font-size: 13px;
}

.login-form {
  width: 100%;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 19px;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 10px;
  box-shadow: 0 0 0 1px #dce6ed inset;
  background: #f9fbfd;
  transition: box-shadow 0.2s ease, background 0.2s ease;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px #14b8a6 inset, 0 8px 22px rgba(20, 184, 166, 0.1);
}

.input-icon {
  width: 15px;
  height: 15px;
  color: #8aa0ae;
}

.captcha-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 128px;
  gap: 10px;
}

.login-code {
  height: 48px;
  padding: 0;
  border: 1px solid #dce6ed;
  border-radius: 10px;
  background: #f9fbfd;
  overflow: hidden;
  cursor: pointer;
}

.login-code-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -2px 0 22px;
  color: #9aa9b4;
  font-size: 12px;
}

.login-options :deep(.el-checkbox__label) {
  color: #667b89;
  font-size: 13px;
}

.login-button {
  width: 100%;
  height: 49px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(100deg, #0ea5a8, #0284c7);
  box-shadow: 0 14px 28px rgba(2, 132, 199, 0.22);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.login-button:hover {
  background: linear-gradient(100deg, #0d989b, #0274ad);
}

.register-link {
  margin-top: 14px;
  text-align: right;
}

.login-tip {
  margin: 22px 0 0;
  padding-top: 18px;
  border-top: 1px solid #edf2f6;
  color: #9aa9b4;
  font-size: 12px;
  line-height: 1.65;
  text-align: center;
}

.el-login-footer {
  position: absolute;
  bottom: 22px;
  color: #91a2ad;
  font-size: 11px;
  letter-spacing: 0.03em;
}

@media (max-width: 960px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    display: none;
  }

  .login-panel {
    padding: 34px 22px 60px;
    background:
      radial-gradient(circle at 10% 10%, rgba(34, 211, 238, 0.2), transparent 30%),
      linear-gradient(150deg, #0b3047, #0b5860);
  }

  .login-card {
    padding: 32px 26px 25px;
    border-radius: 18px;
  }

  .mobile-brand {
    display: flex;
  }

  .el-login-footer {
    color: rgba(226, 240, 248, 0.7);
  }
}

@media (max-width: 520px) {
  .login-panel {
    justify-content: flex-start;
    padding-top: max(48px, 9vh);
  }

  .login-card {
    padding: 28px 20px 22px;
  }

  .login-heading h2 {
    font-size: 25px;
  }

  .captcha-row {
    grid-template-columns: minmax(0, 1fr) 112px;
  }
}
</style>
