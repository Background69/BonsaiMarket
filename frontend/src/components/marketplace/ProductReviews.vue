<template>
  <section id="product-reviews" class="discussion product-reviews" aria-labelledby="reviews-title">
    <header><h2 id="reviews-title">Đánh giá sản phẩm</h2><p class="muted">Chia sẻ trải nghiệm từ đơn hàng đã hoàn thành.</p></header>
    <ResourceState v-if="loading || loadError" resource="đánh giá" :loading="loading" :error="Boolean(loadError)" @retry="load"/>
    <template v-else>
      <div v-if="data.ratingCount" class="review-overview">
        <div class="review-summary">
          <p><strong>{{ Number(data.averageRating).toFixed(1) }}<span> / 5</span></strong></p>
          <p>{{ data.ratingCount }} đánh giá · Điểm chung</p>
          <p class="discussion-meta">Điểm chung tính tất cả đánh giá, gồm {{ data.unverifiedRatingCount }} đánh giá chưa xác minh lượt mua.</p>
          <p v-if="data.verifiedRatingCount" class="review-verified-summary">{{ Number(data.verifiedAverageRating).toFixed(1) }} / 5 từ {{ data.verifiedRatingCount }} lượt mua đã xác minh.</p>
          <p v-else class="discussion-meta">Chưa có đánh giá từ lượt mua đã xác minh.</p>
        </div>
        <ol class="review-distribution" aria-label="Phân bố số đánh giá theo mức sao">
          <li v-for="bucket in data.ratingDistribution" :key="bucket.stars">
            <span>{{ bucket.stars }} sao</span>
            <meter min="0" :max="data.ratingCount" :value="bucket.count" :aria-label="bucket.stars+' sao: '+bucket.count+' đánh giá'"/>
            <span>{{ bucket.count }} <span class="discussion-meta">· {{ percent(bucket.count) }}%</span></span>
          </li>
        </ol>
      </div>
      <p v-else class="discussion-empty">Chưa có đánh giá cho sản phẩm này.</p>
      <article v-for="review in data.reviews.content" :key="review.id" class="discussion-entry">
        <header><strong>{{ review.authorName }}</strong><time :datetime="review.createdAt">{{ date(review.createdAt) }}</time></header>
        <p class="review-rating" :aria-label="review.rating + ' trên 5 sao'"><FontAwesomeIcon v-for="star in 5" :key="star" :icon="faStar" :class="{'is-filled':star<=review.rating}" aria-hidden="true"/></p>
        <p class="discussion-meta">{{ review.verifiedPurchase ? 'Đã mua · Đơn hoàn thành' : 'Chưa xác minh lượt mua' }}</p>
        <p v-if="review.comment" class="discussion-text">{{ review.comment }}</p>
      </article>
      <nav v-if="data.reviews.totalPages>1" class="discussion-pagination" aria-label="Phân trang đánh giá">
        <button class="button button-outline" :disabled="page===0 || busy" @click="page--;load()">Trước</button>
        <span>Trang {{ page+1 }} / {{ data.reviews.totalPages }}</span>
        <button class="button button-outline" :disabled="page+1>=data.reviews.totalPages || busy" @click="page++;load()">Sau</button>
      </nav>
    </template>
    <p v-if="message" role="status">{{ message }}</p><p v-if="submitError" role="alert">{{ submitError }}</p>
    <template v-if="isAuthenticated">
      <p v-if="eligibilityLoading" role="status">Đang kiểm tra lượt mua có thể đánh giá…</p>
      <div v-else-if="eligibilityError" role="alert"><p>{{ eligibilityError }}</p><button class="button button-outline" @click="loadEligibility">Kiểm tra lại lượt mua</button></div>
      <form v-else-if="eligibility.purchases.length" :aria-busy="busy" @submit.prevent="submit">
        <h3>Đánh giá lượt mua của bạn</h3>
        <label for="review-purchase">Lượt mua đã hoàn thành</label>
        <select id="review-purchase" v-model="purchase" :disabled="busy" required><option value="" disabled>Chọn lượt mua</option><option v-for="item in eligibility.purchases" :key="item.orderItemId" :value="item.orderItemId">{{ item.orderCode }} · {{ date(item.purchasedAt) }} · Mục {{ item.orderItemId }}</option></select>
        <fieldset class="review-stars" :disabled="busy" aria-describedby="review-rating-help"><legend>Số sao đánh giá</legend><div>
          <label v-for="star in 5" :key="star" :class="{'is-selected':rating>=star}"><input v-model="rating" type="radio" name="product-rating" :value="star" required/><FontAwesomeIcon :icon="faStar" aria-hidden="true"/><span>{{ star }} sao</span></label>
        </div></fieldset>
        <p id="review-rating-help" class="discussion-notice">{{ rating ? 'Đã chọn '+rating+' trên 5 sao.' : 'Chọn từ 1 đến 5 sao. Dùng phím mũi tên để đổi lựa chọn.' }}</p>
        <label for="review-comment">Nội dung đánh giá</label><textarea id="review-comment" v-model="comment" rows="4" maxlength="2000" :disabled="busy" required aria-describedby="review-help"/>
        <p id="review-help" class="discussion-notice">{{ comment.length }}/2.000 ký tự. Mỗi dòng sản phẩm đã mua trong đơn hoàn tất được gửi một đánh giá.</p>
        <button class="button button-primary" :disabled="busy || !purchase || !rating || !comment.trim()">{{ busy ? 'Đang gửi…' : 'Gửi đánh giá' }}</button>
      </form>
      <div v-else class="discussion-empty"><p>{{ eligibility.message }}</p><template v-if="comment"><label for="review-unsent-draft">Nội dung đang soạn được giữ lại</label><textarea id="review-unsent-draft" v-model="comment" maxlength="2000" rows="4"/></template></div>
    </template>
    <p v-else><RouterLink :to="{path:'/login',query:{redirect:route.fullPath}}">Đăng nhập để kiểm tra lượt mua có thể đánh giá</RouterLink></p>
  </section>
</template>
<script setup>
import {ref,watch,onBeforeUnmount} from 'vue';
import {useRoute} from 'vue-router';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {faStar} from '@fortawesome/free-solid-svg-icons';
import {useAuth} from '../../composables/useAuth.js';
import {marketplaceRequest as request} from '../../services/marketplace.js';
import ResourceState from './ResourceState.vue';
import '../../css/discussions.css';
const props=defineProps({productId:{type:[Number,String],required:true}}),route=useRoute(),{user,isAuthenticated}=useAuth();
const data=ref({reviews:{content:[],totalPages:0},ratingCount:0,averageRating:null,verifiedRatingCount:0,unverifiedRatingCount:0,ratingDistribution:[]}),page=ref(0),loading=ref(true),loadError=ref('');
const percent=count=>data.value.ratingCount?Math.round(count*100/data.value.ratingCount):0;
const eligibility=ref({purchases:[],message:''}),eligibilityLoading=ref(false),eligibilityError=ref(''),purchase=ref(''),rating=ref(0),comment=ref(''),busy=ref(false),message=ref(''),submitError=ref('');
let context=0,listRequest=0,eligibilityRequest=0;
const date=value=>value?new Intl.DateTimeFormat('vi-VN',{dateStyle:'medium',timeZone:'Asia/Ho_Chi_Minh'}).format(new Date(/[Z+]\d*$/.test(value)?value:value+'Z')):'';
async function load(){const ticket=++listRequest,scope=context;loading.value=true;loadError.value='';try{const result=await request('/products/'+props.productId+'/reviews?page='+page.value);if(scope===context&&ticket===listRequest)data.value=result;}catch(e){if(scope===context&&ticket===listRequest)loadError.value=e.message;}finally{if(scope===context&&ticket===listRequest)loading.value=false;}}
async function loadEligibility(){if(!isAuthenticated.value)return;const ticket=++eligibilityRequest,scope=context;eligibilityLoading.value=true;eligibilityError.value='';try{const result=await request('/account/products/'+props.productId+'/review-eligibility');if(scope===context&&ticket===eligibilityRequest){eligibility.value=result;if(!result.purchases.some(p=>p.orderItemId===purchase.value))purchase.value=result.purchases.length===1?result.purchases[0].orderItemId:'';}}catch(e){if(scope===context&&ticket===eligibilityRequest)eligibilityError.value=e.message;}finally{if(scope===context&&ticket===eligibilityRequest)eligibilityLoading.value=false;}}
async function submit(){if(busy.value)return;const scope=context,productId=props.productId,payload={orderItemId:Number(purchase.value),rating:rating.value,comment:comment.value};busy.value=true;submitError.value='';message.value='';try{await request('/account/products/'+productId+'/reviews','POST',payload,false);if(scope!==context)return;comment.value='';rating.value=0;message.value='Đã gửi đánh giá cho lượt mua của bạn.';page.value=0;await Promise.all([load(),loadEligibility()]);}catch(e){if(scope!==context)return;submitError.value=e.message;if(e.status===409)await loadEligibility();}finally{if(scope===context)busy.value=false;}}
watch([()=>props.productId,()=>user.value?.id],()=>{context++;page.value=0;purchase.value='';rating.value=0;comment.value='';message.value='';submitError.value='';busy.value=false;eligibility.value={purchases:[],message:''};load();loadEligibility();},{immediate:true});
onBeforeUnmount(()=>context++);
</script>
<style scoped>
.review-overview{display:grid;grid-template-columns:minmax(180px,1fr) minmax(0,1fr);gap:32px;align-items:center;padding-block:24px}.review-summary p{margin:4px 0;line-height:1.6}.review-summary strong{font-size:var(--bm-type-task);color:var(--bm-forest)}.review-summary strong span{font-size:var(--bm-type-body);font-weight:400}.review-verified-summary{font-size:var(--bm-type-label);color:var(--bm-forest)}.review-distribution{display:grid;gap:12px;list-style:none;margin:0;padding:0}.review-distribution li{display:grid;grid-template-columns:44px minmax(0,1fr) minmax(64px,auto);gap:12px;align-items:center;font-size:var(--bm-type-label)}.review-distribution li>span:last-child{text-align:right;white-space:nowrap}.review-distribution meter{width:100%;height:12px;border:0;background:var(--bm-border);border-radius:var(--bm-radius-control)}.review-distribution meter::-webkit-meter-bar{background:var(--bm-border);border:0}.review-distribution meter::-webkit-meter-optimum-value{background:var(--bm-forest)}.review-distribution meter::-moz-meter-bar{background:var(--bm-forest)}.review-rating{display:flex;gap:6px;color:var(--bm-muted);font-size:18px;margin:16px 0 8px}.review-rating .is-filled{color:var(--bm-forest)}
@media(max-width:600px){.review-overview{grid-template-columns:minmax(0,1fr);gap:20px}.review-distribution li{gap:8px}}
.review-stars{border:0;margin:0;padding:0;min-width:0}.review-stars legend{font-weight:600;margin-bottom:12px}.review-stars>div{display:flex;flex-wrap:wrap;gap:8px}.review-stars label{display:grid;justify-items:center;gap:6px;position:relative;padding:10px;min-width:48px;border:1px solid var(--bm-control-border);border-radius:var(--bm-radius-control);color:var(--bm-muted);cursor:pointer;font-size:14px}.review-stars label.is-selected{color:var(--bm-forest);background:var(--bm-soft)}.review-stars input{position:absolute;opacity:0;width:100%;height:100%;inset:0;min-height:0;padding:0;cursor:pointer}.review-stars label:focus-within{outline:3px solid var(--bm-forest);outline-offset:3px}.review-stars svg{font-size:20px}.product-reviews h3{font-size:var(--bm-type-subheading);margin:0}.product-reviews .discussion-notice{margin:0}
</style>
