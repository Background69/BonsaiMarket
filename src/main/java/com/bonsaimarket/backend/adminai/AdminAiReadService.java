package com.bonsaimarket.backend.adminai;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Clock;
import java.util.*;
import static com.bonsaimarket.backend.adminai.AdminAiDtos.*;

/** Scalar SELECT projections only. No entity loading, mutation service, native SQL or model query execution. */
@Service
public class AdminAiReadService {
    private static final String PUBLIC_STORE = "s.status=com.bonsaimarket.backend.store.StoreStatus.ACTIVE and o.isActive=true and o.role in (com.bonsaimarket.backend.user.UserRole.SELLER,com.bonsaimarket.backend.user.UserRole.ADMIN)";
    private static final String PRODUCTS = " from Product p join p.store s join s.owner o join p.category c where p.status=com.bonsaimarket.backend.product.ProductStatus.ACTIVE and c.isActive=true and " + PUBLIC_STORE;
    private static final String STORES = " from Store s join s.owner o where " + PUBLIC_STORE;
    private static final String SCOPE = "Sản phẩm ACTIVE, danh mục hoạt động, gian hàng ACTIVE, chủ gian hàng hoạt động với vai trò SELLER/ADMIN tại thời điểm đo.";
    private final EntityManager em;
    private final AdminAiAccess access;
    private final Clock clock;
    public AdminAiReadService(EntityManager em, AdminAiAccess access, Clock clock) { this.em=em; this.access=access; this.clock=clock; }

    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ, timeout=8)
    public Snapshot read(AuthPrincipal principal, AdminAiQuery query) {
        access.requireAdmin(principal);
        if(query.to()!=null && query.to().isAfter(java.time.LocalDate.now(clock.withZone(AdminAiQuery.ZONE))))
            throw AdminAiQuery.invalid("Không thể đo khoảng ngày trong tương lai.");
        var metrics = new ArrayList<Metric>(); var rows = new ArrayList<Row>();
        var sources = new ArrayList<Source>(); var warnings = new ArrayList<String>(); var findings = new ArrayList<Finding>();
        String title;
        switch (query.operation()) {
            case OVERVIEW -> {
                title="Tổng quan sản phẩm và gian hàng";
                sources.add(new Source("catalog", "Tra cứu sản phẩm công khai", "/admin/products"));
                sources.add(new Source("stores", "Danh sách gian hàng công khai", "/stores"));
                metrics.add(metric("publicProducts", "Sản phẩm công khai", count("select count(p)"+PRODUCTS), SCOPE, "catalog"));
                metrics.add(metric("publicStores", "Gian hàng công khai", count("select count(s)"+STORES), "Gian hàng ACTIVE, chủ hoạt động và có vai trò SELLER/ADMIN; có thể chưa có sản phẩm.", "stores"));
                metrics.add(metric("inStock", "Sản phẩm còn hàng", count("select count(p)"+PRODUCTS+" and p.stock>0"), "stock > 0; "+SCOPE, "catalog"));
                metrics.add(metric("soldOut", "Sản phẩm hết hàng", count("select count(p)"+PRODUCTS+" and p.stock=0"), "stock = 0; "+SCOPE, "catalog"));
                metrics.add(metric("unknownStock", "Tồn kho chưa xác định", count("select count(p)"+PRODUCTS+" and (p.stock is null or p.stock<0)"), "stock NULL hoặc âm, không quy thành 0; "+SCOPE, "catalog"));
                finding(metrics,findings,"soldOut","soldOutCheck","Có sản phẩm công khai với stock = 0; đây là tình trạng tồn kho, không phải kết luận vi phạm.","Mở tra cứu sản phẩm để kiểm tra tồn kho với người bán.");
                finding(metrics,findings,"unknownStock","unknownStockCheck","Có sản phẩm chưa có tồn kho hợp lệ; không thể coi là còn hàng hoặc hết hàng.","Kiểm tra dữ liệu tồn kho ở module hiện có.");
            }
            case PRODUCT_QUALITY -> {
                title="Chất lượng nội dung sản phẩm";
                sources.add(new Source("catalog", "Tra cứu sản phẩm công khai", "/admin/products"));
                metrics.add(metric("missingDescription","Thiếu mô tả",productCount("p.description is null or trim(p.description)=''"),"Mô tả NULL hoặc rỗng sau trim; "+SCOPE,"catalog"));
                metrics.add(metric("shortDescription","Mô tả ngắn",productCount("p.description is not null and length(trim(p.description)) between 1 and 79"),"Từ 1 đến 79 ký tự sau trim; ngưỡng gợi ý nội dung, không phải chính sách vi phạm; "+SCOPE,"catalog"));
                metrics.add(metric("missingImage","Thiếu ảnh chính",productCount("p.imageUrl is null or trim(p.imageUrl)=''"),"imageUrl NULL/rỗng; không kiểm tra ảnh từ xa; "+SCOPE,"catalog"));
                metrics.add(metric("missingSpecifications","Chưa có thông số",productCount("p.technicalSpecsJson is null or trim(p.technicalSpecsJson)='' or trim(p.technicalSpecsJson)='{}'"),"JSON thông số NULL/rỗng/{}; chưa kiểm định độ đầy đủ theo từng danh mục; "+SCOPE,"catalog"));
                long totalActive=count("select count(p) from Product p where p.status=com.bonsaimarket.backend.product.ProductStatus.ACTIVE");
                metrics.add(metric("outsidePublic","Sản phẩm ACTIVE ngoài phạm vi công khai",totalActive-count("select count(p)"+PRODUCTS),"Tổng ACTIVE trừ phạm vi công khai: gồm thiếu/ẩn danh mục, gian hàng không công khai hoặc chủ không đủ điều kiện. Chỉ trả tổng, không lộ bản ghi.","catalog"));
                metrics.add(metric("missingCategory","Sản phẩm ACTIVE thiếu danh mục",count("select count(p) from Product p where p.status=com.bonsaimarket.backend.product.ProductStatus.ACTIVE and p.category is null"),"Chỉ tổng hợp ACTIVE thiếu category; không trả tên, ID hoặc nội dung bản ghi không công khai.","catalog"));
                String bad="(p.description is null or length(trim(p.description))<80 or p.imageUrl is null or trim(p.imageUrl)='' or p.technicalSpecsJson is null or trim(p.technicalSpecsJson)='' or trim(p.technicalSpecsJson)='{}')";
                for(Object[] r:em.createQuery("select p.id,substring(p.name,1,160),length(trim(p.description)),case when p.imageUrl is null or trim(p.imageUrl)='' then 1 else 0 end,case when p.technicalSpecsJson is null or trim(p.technicalSpecsJson)='' or trim(p.technicalSpecsJson)='{}' then 1 else 0 end"+PRODUCTS+" and "+bad+" order by p.id",Object[].class).setMaxResults(10).getResultList()) {
                    var flags=new ArrayList<String>(); long length=r[2]==null?0:((Number)r[2]).longValue();
                    if(length==0) flags.add("Thiếu mô tả"); else if(length<80) flags.add("Mô tả dưới 80 ký tự");
                    if(((Number)r[3]).intValue()==1) flags.add("Thiếu ảnh chính");
                    if(((Number)r[4]).intValue()==1) flags.add("Chưa có thông số");
                    long id=((Number)r[0]).longValue(); rows.add(new Row(id,label(r[1]),null,flags,"/products/"+id));
                }
                warnings.add("Cảnh báo chất lượng nội dung không phải vi phạm chính sách. Các nhóm có thể trùng nhau; không cộng thành số sản phẩm duy nhất.");
                finding(metrics,findings,"missingDescription","descriptionCheck","Có sản phẩm công khai chưa có mô tả.","Mở sản phẩm để đọc nội dung và trao đổi với người bán qua quy trình hiện có.");
                finding(metrics,findings,"shortDescription","shortDescriptionCheck","Có mô tả dưới ngưỡng tham khảo 80 ký tự.","Kiểm tra xem nội dung đã đủ giúp người mua hiểu sản phẩm chưa.");
                finding(metrics,findings,"missingImage","imageCheck","Có sản phẩm công khai chưa khai báo ảnh chính.","Kiểm tra nội dung sản phẩm; không tự kết luận vi phạm.");
                finding(metrics,findings,"missingSpecifications","specificationCheck","Có sản phẩm chưa khai báo thông số trong trường JSON.","Đối chiếu loại sản phẩm trước khi đề nghị bổ sung thông tin phù hợp.");
            }
            case STORES, STORE_COMPLETENESS -> {
                title="Hồ sơ và sản phẩm của gian hàng";
                sources.add(new Source("stores","Gian hàng công khai","/stores"));
                metrics.add(metric("publicStores","Gian hàng công khai",count("select count(s)"+STORES),"ACTIVE, chủ hoạt động và có vai trò SELLER/ADMIN.","stores"));
                metrics.add(metric("missingAbout","Thiếu Giới thiệu gian hàng",count("select count(s)"+STORES+" and (s.aboutShop is null or trim(s.aboutShop)='')"),"aboutShop NULL/rỗng; chỉ gian hàng công khai.","stores"));
                metrics.add(metric("missingBanner","Thiếu banner",count("select count(s)"+STORES+" and (s.bannerUrl is null or trim(s.bannerUrl)='')"),"bannerUrl NULL/rỗng; không kiểm tra URL từ xa; chỉ gian hàng công khai.","stores"));
                String join=" from Store s join s.owner o left join Product p on p.store=s and p.status=com.bonsaimarket.backend.product.ProductStatus.ACTIVE and p.category.isActive=true where "+PUBLIC_STORE;
                boolean incomplete=query.operation()==AdminAiQuery.Operation.STORE_COMPLETENESS;
                if(incomplete) join+=" and (s.aboutShop is null or trim(s.aboutShop)='' or s.bannerUrl is null or trim(s.bannerUrl)='')";
                for(Object[] r:em.createQuery("select s.id,s.name,count(p.id),case when s.aboutShop is null or trim(s.aboutShop)='' then 1 else 0 end,case when s.bannerUrl is null or trim(s.bannerUrl)='' then 1 else 0 end"+join+" group by s.id,s.name,s.aboutShop,s.bannerUrl order by count(p.id) desc,s.id asc",Object[].class).setMaxResults(10).getResultList()) {
                    var flags=new ArrayList<String>();if(((Number)r[3]).intValue()==1) flags.add("Thiếu giới thiệu");if(((Number)r[4]).intValue()==1) flags.add("Thiếu banner");
                    long id=((Number)r[0]).longValue(); rows.add(new Row(id,label(r[1]),((Number)r[2]).longValue(),flags,"/stores/"+id));
                }
                warnings.add((incomplete?"Chỉ liệt kê gian hàng thiếu giới thiệu hoặc banner. ":"")+"Danh sách tối đa 10 gian hàng, sắp theo số sản phẩm công khai giảm dần rồi ID tăng dần. Thiếu hồ sơ không chứng minh gian lận; số sản phẩm không biểu thị độ phổ biến.");
                finding(metrics,findings,"missingAbout","aboutCheck","Có gian hàng công khai chưa có nội dung Giới thiệu gian hàng.","Kiểm tra hồ sơ công khai trước khi đề nghị người bán bổ sung.");
                finding(metrics,findings,"missingBanner","bannerCheck","Có gian hàng công khai chưa khai báo banner.","Xem hồ sơ gian hàng; banner là nội dung, không phải bằng chứng xác minh người bán.");
            }
            case CATEGORIES -> {
                title="Phân bố danh mục đang hoạt động";
                sources.add(new Source("categories","Quản lý danh mục","/admin/categories"));
                metrics.add(metric("activeCategories","Danh mục hoạt động",count("select count(c) from Category c where c.isActive=true"),"isActive = true; bao gồm danh mục chưa có sản phẩm công khai.","categories"));
                for(Object[] r:em.createQuery("select c.id,c.name,count(p.id),count(distinct s.id)"+PRODUCTS+" group by c.id,c.name order by count(p.id) desc,c.id asc",Object[].class).setMaxResults(10).getResultList()) {
                    long id=((Number)r[0]).longValue();String name=label(r[1]);
                    rows.add(new Row(id,name,((Number)r[2]).longValue(),List.of("Gian hàng công khai: "+((Number)r[3]).longValue()),"/products?categoryId="+id));
                    metrics.add(metric("category"+id,"Sản phẩm thuộc "+name,((Number)r[2]).longValue(),SCOPE,"categories"));
                }
                warnings.add("Tối đa 10 danh mục có sản phẩm công khai, số sản phẩm giảm dần rồi ID tăng dần. Đếm gian hàng theo danh mục có thể trùng giữa các danh mục.");
            }
            case REPORTS -> {
                title="Hàng đợi báo cáo cần đối chiếu";
                sources.add(new Source("reports","Xem xét báo cáo","/admin/reports"));
                for(String status:List.of("OPEN","IN_REVIEW","RESOLVED","DISMISSED")) {
                    long n=em.createQuery("select count(r) from MarketplaceReport r where r.status=:status",Long.class).setParameter("status",status).getSingleResult();
                    metrics.add(metric("report"+status,switch(status){case "OPEN"->"Báo cáo chờ tiếp nhận";case "IN_REVIEW"->"Báo cáo đang xem xét";case "RESOLVED"->"Báo cáo đã xử lý";default->"Báo cáo không tiếp tục xử lý";},n,"Trạng thái lưu trong hệ thống = "+status+"; không xác nhận cáo buộc.","reports"));
                }
                for(String reason:List.of("MISLEADING","PROHIBITED","FRAUD","OTHER")) {
                    long n=em.createQuery("select count(r) from MarketplaceReport r where r.status in ('OPEN','IN_REVIEW') and r.reason=:reason",Long.class).setParameter("reason",reason).getSingleResult();
                    metrics.add(metric("reason"+reason,switch(reason){case "MISLEADING"->"Cáo buộc thông tin sai lệch";case "PROHIBITED"->"Cáo buộc nội dung bị cấm";case "FRAUD"->"Cáo buộc gian lận";default->"Lý do khác";},n,"Lý do người gửi chọn; chỉ OPEN/IN_REVIEW; chưa xác minh nội dung.","reports"));
                }
                warnings.add("Trạng thái và số lượng là dữ liệu hệ thống; lý do là cáo buộc từ người gửi. Không gửi mô tả, danh tính người báo cáo hoặc kết luận xử lý tới Gemini.");
                finding(metrics,findings,"reportOPEN","reportCheck","Có báo cáo chưa được tiếp nhận; cáo buộc chưa được xác minh.","Mở hàng đợi báo cáo, đối chiếu chứng cứ và thực hiện quy trình đang có nếu phù hợp.");
                finding(metrics,findings,"reportIN_REVIEW","reportReviewCheck","Có báo cáo đang được xem xét.","Đối chiếu nội dung và lịch sử xử lý trong module báo cáo.");
            }
            case NEW_PRODUCTS, NEW_STORES -> {
                boolean product=query.operation()==AdminAiQuery.Operation.NEW_PRODUCTS;
                title=product?"Sản phẩm mới theo ngày tạo":"Gian hàng mới theo ngày tạo";
                String alias=product?"p":"s",base=product?PRODUCTS:STORES;
                String dated=base+" and "+alias+".createdAt>=:start and "+alias+".createdAt<:end";
                sources.add(new Source("created",product?"Sản phẩm công khai":"Gian hàng công khai",product?"/admin/products":"/stores"));
                long n=em.createQuery("select count("+alias+")"+dated,Long.class).setParameter("start",query.startUtc()).setParameter("end",query.endUtc()).getSingleResult();
                metrics.add(metric("createdInRange",product?"Sản phẩm tạo trong khoảng ngày":"Gian hàng tạo trong khoảng ngày",n,"createdAt trong khoảng ngày đã chọn, lưu UTC và đổi ranh giới từ Asia/Ho_Chi_Minh; hiện vẫn công khai. Không phải ngày xuất bản hoặc updatedAt.","created"));
                metrics.add(metric("unknownCreatedAt","Bản ghi công khai thiếu ngày tạo",count("select count("+alias+")"+base+" and "+alias+".createdAt is null"),"Không thể xếp các bản ghi createdAt NULL vào khoảng ngày.","created"));
                for(Object[] r:em.createQuery("select "+alias+".id,"+alias+".name"+dated+" order by "+alias+".createdAt desc,"+alias+".id desc",Object[].class).setParameter("start",query.startUtc()).setParameter("end",query.endUtc()).setMaxResults(10).getResultList()) {
                    long id=((Number)r[0]).longValue();rows.add(new Row(id,label(r[1]),null,List.of(),(product?"/products/":"/stores/")+id));
                }
                warnings.add("Đếm theo ngày tạo của các bản ghi hiện công khai, không tái dựng lịch sử hiển thị. Danh sách mới nhất tối đa 10 bản ghi.");
                finding(metrics,findings,"unknownCreatedAt","createdCheck","Có bản ghi công khai thiếu ngày tạo, nên không được tính vào khoảng ngày.","Đối chiếu timestamp trong module nguồn; không suy đoán ngày còn thiếu.");
            }
            default -> throw new IllegalStateException("Closed operation");
        }
        warnings.add("Dữ liệu là ảnh chụp tại thời điểm đo; nếu database đang dùng development seed thì các số đo bao gồm dữ liệu demo đó. Không phải doanh thu hoặc giao dịch đã xác minh.");
        return new Snapshot(query.operation().name(),title,clock.instant(),AdminAiQuery.ZONE.getId(),query.range(),List.copyOf(metrics),List.copyOf(rows),List.copyOf(sources),List.copyOf(warnings),List.copyOf(findings));
    }
    private long count(String query) { return em.createQuery(query,Long.class).getSingleResult(); }
    private long productCount(String condition) { return count("select count(p)"+PRODUCTS+" and ("+condition+")"); }
    private static Metric metric(String id,String name,long value,String definition,String source) { return new Metric(id,name,value,definition,source.equals("reports")?"REPORT_STATUS_AGGREGATES":Set.of("outsidePublic","missingCategory").contains(id)?"ACTIVE_NON_PUBLIC_AGGREGATE":"CURRENT_PUBLIC_SNAPSHOT",source); }
    private static String label(Object s) { String name=s==null?"Chưa có tên":s.toString();return name.substring(0,Math.min(160,name.length())); }
    private static void finding(List<Metric> metrics,List<Finding> out,String metric,String id,String text,String next) {
        if(metrics.stream().anyMatch(m->m.id().equals(metric)&&m.value()>0)) out.add(new Finding(id,text,List.of(metric),next));
    }
}
