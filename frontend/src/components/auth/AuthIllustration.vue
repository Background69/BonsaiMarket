<script setup>
defineProps({paused: Boolean});
</script>

<template>
  <div class="auth-illustration" :class="{'is-paused': paused}" aria-hidden="true">
    <svg viewBox="0 0 420 440" fill="none" focusable="false">
      <ellipse class="auth-tree-shadow" cx="210" cy="390" rx="91" ry="9" fill="currentColor"/>
      <g class="auth-tree">
        <path d="M201 309c28-26-10-49 10-79 14-20 17-44 8-71-5-16-3-34 4-48" stroke="currentColor" stroke-width="17" stroke-linecap="round"/>
        <path d="M211 249c-17-19-43-18-65-40m76-8c21-17 42-13 63-34m-62 0c-15-13-31-12-42-29" stroke="currentColor" stroke-width="7" stroke-linecap="round"/>
        <g fill="currentColor">
          <path d="M153 112c0-15 13-26 29-25 7-19 29-25 46-14 23-9 45 7 44 26 13 3 22 12 22 23-21 11-40 7-57 4-24 8-58 8-84-1-8-3-9-9 0-13Z"/>
          <path d="M241 158c8-15 25-19 38-14 8-13 26-15 38-5 15-1 28 11 26 24 12 3 17 13 12 23-28 8-48 1-63 3-19 4-41 6-61-2-10-5-6-18 10-29Z"/>
          <path d="M79 204c8-15 21-20 37-15 9-17 30-23 46-12 20-4 37 9 35 25 15 6 20 18 12 27-24 6-38 0-58 4-29 7-60 4-81-6-7-7-3-17 9-23Z"/>
          <path d="M164 304c13-8 28-7 39-5 13-8 25-5 37 5H164Z"/>
          <path d="M139 313h143l-16 39a13 13 0 0 1-12 8h-88a13 13 0 0 1-12-8l-15-39Z"/>
          <rect x="130" y="306" width="161" height="10" rx="4"/>
          <path d="M160 359h17v8h-17zm84 0h17v8h-17z"/>
        </g>
        <path d="M160 325h99" stroke="var(--bm-dark)" stroke-width="2" opacity=".2"/>
        <path d="M123 183c-7-9-6-18-1-24 9 4 13 13 8 23" fill="currentColor" opacity=".45"/>
        <path d="M293 120c-1-10 3-17 11-21 5 9 4 17-4 23" fill="currentColor" opacity=".45"/>
      </g>
    </svg>
  </div>
</template>
