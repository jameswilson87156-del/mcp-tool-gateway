<template>
  <header class="topbar">
    <label class="search-box">
      <span aria-hidden="true">⌕</span>
      <input type="search" placeholder="搜索 Tool、Provider、Trace" />
    </label>
    <div class="topbar-actions">
      <button type="button" class="select-button">生产环境 <span>⌄</span></button>
      <span class="provider-status">Provider 状态 <strong>12/12 正常</strong></span>
      <button type="button" class="icon-button" aria-label="通知">!</button>
      <div class="avatar" aria-label="管理员头像">管</div>
      <button type="button" class="admin-button">ADMIN <span>⌄</span></button>
    </div>
  </header>
</template>
