// Thay ảnh trong public/images/home/; cấu hình các vị trí ảnh ở đây.
export const homeVisuals = {
    heroImage: '/images/home/hero.webp',
    aboutImage: '/images/home/about.webp',
    categoryFallback: '/images/home/category.webp',
    productFallback: '/images/home/product.webp',
    promoMainImage: '/images/home/promo-main.webp',
    promoSideImage1: '/images/home/promo-side-1.webp',
    promoSideImage2: '/images/home/promo-side-2.webp',
    promoGreenImage: '/images/home/promo-green.webp',
    promoBonsaiImage: '/images/home/promo-bonsai.webp',
    collectionImage: '/images/home/collection.webp',
    bannerImage: '/images/home/banner.webp',
    knowledgeImage1: '/images/home/knowledge-1.webp',
    knowledgeImage2: '/images/home/knowledge-2.webp',
    knowledgeImage3: '/images/home/knowledge-3.webp',
    ...Object.fromEntries(Array.from({length: 7}, (_, i) => [`galleryImage${i + 1}`, `/images/home/gallery-${i + 1}.webp`])),
};

// Có thể gán ảnh theo ID danh mục/sản phẩm thật từ API mà không sửa component.
export const categoryVisuals = {};
export const productVisuals = {};

// Tùy chỉnh nhận diện theo ID gian hàng công khai có thật.
export const storeVisuals = {};

// Ảnh Trang chủ; storefront không tự tải hoặc tạo ảnh mẫu.
export const marketplaceVisuals = {
    compactBanner: null,
    // Ảnh ngang, bonsai bên phải; thiếu ảnh thì dùng minh họa hiện có.
    heroImage: '/images/home/hero.webp',
    heroFallback: '/images/hero-bonsai.svg',
    sellerImage: null,
};
