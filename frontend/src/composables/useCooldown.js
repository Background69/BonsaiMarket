import {onBeforeUnmount, ref} from 'vue';

export function useCooldown(seconds = 60) {
    const remaining = ref(0);
    let timer = null;

    function stop() {
        if (timer) clearInterval(timer);
        timer = null;
        remaining.value = Math.max(0, remaining.value);
    }

    function start() {
        stop();
        remaining.value = seconds;
        timer = setInterval(() => {
            remaining.value -= 1;
            if (remaining.value <= 0) stop();
        }, 1000);
    }

    onBeforeUnmount(stop);
    return {remaining, start};
}
