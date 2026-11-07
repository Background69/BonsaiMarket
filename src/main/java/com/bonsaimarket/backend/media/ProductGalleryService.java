package com.bonsaimarket.backend.media;

import com.bonsaimarket.backend.marketplace.*;
import com.bonsaimarket.backend.product.Product;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import static com.bonsaimarket.backend.media.ProductGalleryDtos.*;

@Service @Transactional(isolation=Isolation.READ_COMMITTED,timeout=60)
public class ProductGalleryService {
    public static final int MAX_IMAGES=8;
    private final EntityManager em;private final MarketplaceAccess access;private final ProductImageAccess guard;
    private final ProductGalleryRepository images;private final ProductImageStorage storage;
    public ProductGalleryService(EntityManager em,MarketplaceAccess access,ProductImageAccess guard,ProductGalleryRepository images,ProductImageStorage storage){this.em=em;this.access=access;this.guard=guard;this.images=images;this.storage=storage;}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public View seller(Long actor,Long id){return view(guard.owned(actor,id,false));}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public View publicGallery(Long id,Long actor){return view(guard.readable(id,actor));}
    public View initialize(Long actor,Long id){var product=guard.owned(actor,id,true);initialize(product);return view(product);}
    private List<ProductGalleryImage> rows(Long id){return new ArrayList<>(images.findByProductIdOrderBySortOrderAscIdAsc(id));}
    private ProductGalleryImage legacy(Product product){
        var old=em.find(ProductImage.class,product.getId());String url=product.getImageUrl();if(url!=null&&url.isBlank())url=null;
        boolean validKey=old!=null&&old.getFileKey().matches("[0-9a-f-]{36}\\.png");
        String route="/api/products/"+product.getId()+"/image";
        boolean internal=validKey&&(url==null||url.equals(route)||url.equals(route+"?v="+old.getFileKey().substring(0,36)));
        if(url==null&&!internal)return null;
        var row=new ProductGalleryImage();row.setProductId(product.getId());row.setImageUrl(url==null?route+"?v="+old.getFileKey().substring(0,36):url);
        row.setPrimary(true);row.setSortOrder(0);row.setCreatedAt(access.now());row.setUpdatedAt(access.now());
        if(internal){row.setStorageKey(old.getFileKey());row.setContentType(old.getContentType());if(old.getBytes()>0)row.setFileSizeBytes(old.getBytes());}
        return row;
    }
    private void initialize(Product product){
        if(product.isGalleryInitialized())return;
        if(rows(product.getId()).isEmpty()){var old=legacy(product);if(old!=null)em.persist(old);}
        product.setGalleryInitialized(true);changed(product);em.flush();syncCover(product,rows(product.getId()));
    }
    private Image image(ProductGalleryImage row){
        String thumb=row.getThumbnailKey()==null?row.getImageUrl():"/api/products/"+row.getProductId()+"/gallery/"+row.getId()+"/thumbnail";
        return new Image(row.getId(),row.getImageUrl(),thumb,row.getSortOrder(),row.isPrimary(),row.getContentType(),row.getFileSizeBytes(),row.getWidth(),row.getHeight(),row.getStorageKey()==null?"LEGACY_URL":row.getChecksum()==null?"LEGACY_UPLOAD":"UPLOAD");
    }
    private View view(Product product){
        var rows=rows(product.getId());
        if(rows.isEmpty()&&!product.isGalleryInitialized()){var old=legacy(product);if(old!=null)rows.add(old);}
        return new View(product.getId(),product.getGalleryRevision(),product.isGalleryInitialized(),MAX_IMAGES,storage.maxBytes(),
            rows.stream().filter(ProductGalleryImage::isPrimary).map(ProductGalleryImage::getImageUrl).findFirst().orElse(null),rows.stream().map(this::image).toList());
    }
    public Upload upload(Long actor,Long id,MultipartFile file,String requestKey){return upload(actor,id,file,requestKey,false);}
    public Upload upload(Long actor,Long id,MultipartFile file,String requestKey,boolean makePrimary){
        guard.owned(actor,id,false);String key;
        try{key=UUID.fromString(requestKey).toString();}catch(IllegalArgumentException|NullPointerException e){throw MarketplaceException.invalid("Cần mã yêu cầu tải ảnh hợp lệ.");}
        var prepared=storage.prepare(file);var product=guard.owned(actor,id,true);initialize(product);
        var receipt=em.createQuery("select u from ProductGalleryUpload u where u.productId=:product and u.requestKey=:key",ProductGalleryUpload.class).setParameter("product",id).setParameter("key",key).getResultStream().findFirst().orElse(null);
        if(receipt!=null){
            if(!receipt.getChecksum().equals(prepared.checksum()))throw conflict("IMAGE_REQUEST_CONFLICT","Mã tải ảnh đã dùng cho nội dung khác.");
            if(!"COMPLETED".equals(receipt.getStatus())||receipt.getImageId()==null||em.find(ProductGalleryImage.class,receipt.getImageId())==null)throw new MarketplaceException(410,"IMAGE_UPLOAD_REMOVED","Ảnh của yêu cầu này đã bị xóa. Không tải lại ngầm bằng mã cũ.");
            return new Upload(view(product),receipt.getImageId(),false,true);
        }
        var rows=rows(id);var duplicate=rows.stream().filter(row->prepared.checksum().equals(row.getChecksum())).findFirst().orElse(null);
        if(duplicate!=null){
            remember(id,key,prepared.checksum(),duplicate.getId());
            if(makePrimary&&!duplicate.isPrimary()){primary(rows,duplicate);changed(product);syncCover(product,rows);}
            em.flush();return new Upload(view(product),duplicate.getId(),true,false);
        }
        if(rows.size()>=MAX_IMAGES)throw conflict("GALLERY_FULL","Sản phẩm đã đủ 8 ảnh. Tải lại gallery hoặc xóa ảnh trước khi thêm.");
        var stored=storage.store(prepared);var row=new ProductGalleryImage();row.setProductId(id);row.setStorageKey(stored.key());row.setThumbnailKey(stored.thumbnailKey());
        row.setImageUrl("/api/products/"+id+"/gallery");row.setSortOrder(rows.size());row.setContentType("image/png");row.setFileSizeBytes((long)prepared.bytes().length);
        row.setWidth(prepared.width());row.setHeight(prepared.height());row.setChecksum(prepared.checksum());row.setCreatedAt(access.now());row.setUpdatedAt(access.now());em.persist(row);em.flush();
        row.setImageUrl("/api/products/"+id+"/gallery/"+row.getId()+"/content");rows.add(row);
        if(rows.size()==1||makePrimary)primary(rows,row);
        remember(id,key,prepared.checksum(),row.getId());changed(product);syncCover(product,rows);em.flush();return new Upload(view(product),row.getId(),false,false);
    }
    private void remember(Long product,String key,String checksum,Long image){var receipt=new ProductGalleryUpload();receipt.setProductId(product);receipt.setRequestKey(key);receipt.setChecksum(checksum);receipt.setImageId(image);receipt.setStatus("COMPLETED");receipt.setCreatedAt(access.now());em.persist(receipt);}
    public View reorder(Long actor,Long id,Reorder body){
        var product=guard.owned(actor,id,true);MarketplaceAccess.revision(product.getGalleryRevision(),body.revision());initialize(product);var rows=rows(id);
        var ids=rows.stream().map(ProductGalleryImage::getId).toList();
        if(body.imageIds().size()!=ids.size()||new HashSet<>(body.imageIds()).size()!=ids.size()||!new HashSet<>(ids).equals(new HashSet<>(body.imageIds())))throw conflict("GALLERY_CHANGED","Thứ tự phải chứa đúng toàn bộ ảnh hiện tại, không lặp ID. Tải lại gallery.");
        if(ids.equals(body.imageIds()))return view(product);
        var index=new HashMap<Long,ProductGalleryImage>();rows.forEach(row->index.put(row.getId(),row));
        for(int order=0;order<body.imageIds().size();order++){var row=index.get(body.imageIds().get(order));row.setSortOrder(order);row.setUpdatedAt(access.now());}
        changed(product);em.flush();return view(product);
    }
    public View setPrimary(Long actor,Long id,Long imageId,long revision){
        var product=guard.owned(actor,id,true);MarketplaceAccess.revision(product.getGalleryRevision(),revision);var rows=rows(id);var row=find(rows,imageId);
        if(!row.isPrimary()){primary(rows,row);changed(product);syncCover(product,rows);em.flush();}return view(product);
    }
    private void primary(List<ProductGalleryImage> rows,ProductGalleryImage selected){
        for(var row:rows)if(row.isPrimary()){row.setPrimary(false);row.setUpdatedAt(access.now());}
        // Release the generated unique primary slot before assigning it to another row.
        em.flush();selected.setPrimary(true);selected.setUpdatedAt(access.now());
    }
    public View remove(Long actor,Long id,Long imageId,long revision){
        var product=guard.owned(actor,id,true);MarketplaceAccess.revision(product.getGalleryRevision(),revision);var rows=rows(id);var row=find(rows,imageId);
        var urls=List.of(row.getImageUrl(),"/api/products/"+id+"/gallery/"+imageId+"/thumbnail");
        if(row.getStorageKey()!=null&&ProductImageReferences.urls(em,urls,id,imageId))throw conflict("IMAGE_REFERENCED","Ảnh còn được tham chiếu bởi sản phẩm, danh mục hoặc gian hàng khác; chưa thể xóa.");
        var keys=new ArrayList<String>();if(row.getStorageKey()!=null)keys.add(row.getStorageKey());if(row.getThumbnailKey()!=null)keys.add(row.getThumbnailKey());storage.deferCleanup(keys,urls);
        em.createQuery("update ProductGalleryUpload u set u.status='REMOVED',u.imageId=null where u.productId=:product and u.imageId=:image").setParameter("product",id).setParameter("image",imageId).executeUpdate();
        boolean wasPrimary=row.isPrimary();row.setPrimary(false);em.remove(row);em.flush();rows.remove(row);
        for(int order=0;order<rows.size();order++)rows.get(order).setSortOrder(order);
        if(wasPrimary&&!rows.isEmpty())primary(rows,rows.getFirst());
        product.setGalleryInitialized(true);changed(product);syncCover(product,rows);em.flush();return view(product);
    }
    public View removeLegacyCover(Long actor,Long id){
        var product=guard.owned(actor,id,true);initialize(product);var rows=rows(id);
        if(rows.size()>1)throw conflict("GALLERY_DELETE_REQUIRED","Sản phẩm có nhiều ảnh. Chọn đúng ảnh cần xóa trong gallery.");
        return rows.isEmpty()?view(product):remove(actor,id,rows.getFirst().getId(),product.getGalleryRevision());
    }
    private ProductGalleryImage find(List<ProductGalleryImage> rows,Long id){return rows.stream().filter(row->row.getId().equals(id)).findFirst().orElseThrow(MarketplaceException::missing);}
    private void syncCover(Product product,List<ProductGalleryImage> rows){product.setImageUrl(rows.stream().filter(ProductGalleryImage::isPrimary).map(ProductGalleryImage::getImageUrl).findFirst().orElse(null));}
    private void changed(Product product){product.setGalleryRevision(product.getGalleryRevision()+1);product.setUpdatedAt(access.now());}
    @Transactional(readOnly=true) public ProductImageService.ImageBytes read(Long productId,Long imageId,boolean thumbnail,Long actor){
        guard.readable(productId,actor);var row=em.find(ProductGalleryImage.class,imageId);
        if(row==null||!row.getProductId().equals(productId)||row.getStorageKey()==null)throw MarketplaceException.missing();
        String key=thumbnail&&row.getThumbnailKey()!=null?row.getThumbnailKey():row.getStorageKey();
        return new ProductImageService.ImageBytes(storage.read(key),thumbnail&&row.getThumbnailKey()!=null?"image/png":row.getContentType());
    }
    private MarketplaceException conflict(String code,String message){return new MarketplaceException(409,code,message);}
}
