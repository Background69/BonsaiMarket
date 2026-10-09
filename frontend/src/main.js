import {createApp} from 'vue'
import './css/index.css'
import './css/legacy-utilities.css'
import App from './App.vue'
import router from './router/index.js'
import {route} from './legacy/route.js'
import {usePage} from './legacy/navigation.js'
import {restoreLegacyIcons} from './legacy/icons.js'

const app = createApp(App)
router.afterEach(to => {
    usePage().url = to.fullPath
})
app.config.globalProperties.route = route
app.config.globalProperties.$page = usePage()
app.use(router).mount('#app')
restoreLegacyIcons()

import './css/marketplace-system.css'
import './css/marketplace-reference.css'
import './css/header-footer-reference.css'
