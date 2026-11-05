<template>
  <dialog ref="dialog" class="admin-confirm-dialog" :aria-labelledby="id+'-title'" :aria-describedby="id+'-description'"
          @cancel="cancel" @close="closed" @click="backdrop">
    <form @submit.prevent="$emit('confirm',reason)"><h2 :id="id+'-title'">{{ title }}</h2>
      <p :id="id+'-description'">{{ description }}</p><label :for="id+'-reason'">Lý do xử lý</label><textarea
          :id="id+'-reason'" v-model="reason" :disabled="busy" required maxlength="1000" rows="4"/>
      <p v-if="error" class="management-error" role="alert">{{ error }}</p>
      <div class="admin-header-actions">
        <button ref="cancelButton" type="button" class="button button-outline" :disabled="busy" @click="dialog.close()">
          Quay lại
        </button>
        <button class="button button-primary" :disabled="busy||!reason.trim()">{{
            busy ? 'Đang ghi nhận…' : 'Xác nhận'
          }}
        </button>
      </div>
    </form>
  </dialog>
</template>
<script setup>
import {nextTick, ref, useId} from 'vue';

const props = defineProps({title: String, description: String, busy: Boolean, error: String});
const emit = defineEmits(['confirm', 'close']), dialog = ref(null), cancelButton = ref(null), reason = ref(''),
    id = useId();
let opener;

async function open(element) {
  opener = element || document.activeElement;
  reason.value = '';
  dialog.value.showModal();
  await nextTick();
  cancelButton.value.focus();
}

function close() {
  dialog.value.close();
}

function cancel(e) {
  if (props.busy) e.preventDefault();
}

function closed() {
  opener?.isConnected && opener.focus();
  emit('close');
}

function backdrop(e) {
  if (e.target !== dialog.value || props.busy) return;
  const r = dialog.value.getBoundingClientRect();
  if (e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom) close();
}

defineExpose({open, close});
</script>
