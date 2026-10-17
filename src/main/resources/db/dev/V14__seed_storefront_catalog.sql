CREATE TEMPORARY TABLE storefront_seed_owners (email VARCHAR(255) PRIMARY KEY, shop_key VARCHAR(40), shop_name VARCHAR(200), public_location VARCHAR(500), summary VARCHAR(4000), about_text TEXT);
INSERT INTO storefront_seed_owners SELECT 'storefront-v1-vuon-hien@example.invalid','vuon-hien','Vườn Hiên · Demo','Đà Nẵng','Sen đá cho góc bàn và ban công nhỏ.','Vườn Hiên bắt đầu từ một kệ cây bên cửa sổ. Chúng tôi dành không gian cho những chậu sen đá nhỏ, dễ sắp đặt trong nhà có ánh sáng phù hợp.

Mỗi cây được giới thiệu theo dáng lá, kích thước và chậu đi kèm. Bạn có thể xem thông tin từng sản phẩm để chọn bố cục phù hợp với góc trồng của mình.

Đây là gian hàng minh họa trong môi trường phát triển, không phải người bán thực.' WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='storefront-v1-vuon-hien@example.invalid');
INSERT INTO storefront_seed_owners SELECT 'storefront-v1-nang-som@example.invalid','nang-som','Nắng Sớm · Demo','Huế','Những cụm sen đá với dáng lá và chậu mộc.','Nắng Sớm tập hợp các chậu cây mọng nước trong một góc vườn thoáng. Các sản phẩm tập trung vào hình dáng cụm lá và sự hài hòa với chậu.

Thông tin chăm sóc trên từng sản phẩm là nội dung minh họa theo điều kiện trồng được mô tả; lượng nước và ánh sáng cần điều chỉnh theo thực tế.

Đây là gian hàng minh họa trong môi trường phát triển, không phải người bán thực.' WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='storefront-v1-nang-som@example.invalid');
INSERT INTO storefront_seed_owners SELECT 'storefront-v1-goc-la@example.invalid','goc-la','Góc Lá · Demo','Đà Lạt','Sen đá và bonsai cho không gian sống.','Góc Lá kết hợp các chậu cây nhỏ với những dáng bonsai dành cho người thích quan sát sự thay đổi của cây qua mùa.

Bộ sưu tập được chia theo danh mục. Hãy đối chiếu kích thước, tình trạng và ảnh minh họa trước khi lựa chọn.

Đây là gian hàng minh họa trong môi trường phát triển, không phải người bán thực.' WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='storefront-v1-goc-la@example.invalid');
INSERT INTO storefront_seed_owners SELECT 'storefront-v1-moc-vien@example.invalid','moc-vien','Mộc Viên · Demo','Hà Nội','Bonsai, đường thân và khoảng trống.','Mộc Viên dành chỗ cho những thế cây giản dị: một đường thân, một tán lá, một chiếc chậu vừa vặn.

Gian hàng giới thiệu bonsai theo dáng tổng thể thay vì khẳng định tuổi cây hoặc giống loài khi chưa có chứng cứ. Thông số của mỗi mẫu được ghi riêng.

Đây là gian hàng minh họa trong môi trường phát triển, không phải người bán thực.' WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='storefront-v1-moc-vien@example.invalid');
INSERT INTO storefront_seed_owners SELECT 'storefront-v1-nha-chau@example.invalid','nha-chau','Nhà Chậu · Demo','TP Hồ Chí Minh','Chậu và phụ kiện cho góc vườn.','Nhà Chậu giới thiệu các vật dụng để sắp xếp một góc cây nhỏ. Trọng tâm là chất liệu, kích thước và cách kết hợp chậu với cây sẵn có.

Sản phẩm trong mục chậu không bao gồm cây trừ khi mô tả ghi rõ. Màu sắc có thể khác khi xem trên những màn hình khác nhau.

Đây là gian hàng minh họa trong môi trường phát triển, không phải người bán thực.' WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='storefront-v1-nha-chau@example.invalid');
INSERT INTO users(name,email,password_hash,role,is_active,email_verified_at) SELECT CONCAT('Demo storefront: ',shop_key),email,'!development-account-login-disabled!','SELLER',TRUE,CURRENT_TIMESTAMP FROM storefront_seed_owners;
INSERT INTO stores(owner_user_id,name,description,address,status,banner_url,about_shop) SELECT u.id,d.shop_name,d.summary,d.public_location,'ACTIVE','https://images.pexels.com/photos/11678291/pexels-photo-11678291.jpeg?auto=compress&dpr=1&h=750&w=1260',d.about_text FROM storefront_seed_owners d JOIN users u ON u.email=d.email;
INSERT INTO categories(name,slug,is_active,sort_order,name_key) SELECT 'Chậu và phụ kiện','chau-va-phu-kien',TRUE,60,NULL WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug='chau-va-phu-kien');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá Echeveria chậu đất nung','demo-storefront-vuon-hien-1','## Giới thiệu
Sen đá Echeveria chậu đất nung có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Miệng chậu được giữ thoáng để dễ quan sát bề mặt giá thể.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá echeveria chậu đất nung. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.

## Gợi ý bố trí
Sen đá Echeveria chậu đất nung có thể được đặt trên một mặt kệ chắc chắn, chừa khoảng trống quanh chậu để quan sát lá và bề mặt giá thể. Khi đổi vị trí, hãy cân nhắc chênh lệch ánh sáng và độ thoáng giữa góc bàn, cửa sổ và hiên nhà; một bố cục đẹp vẫn cần phù hợp với điều kiện trồng thực tế.

## Theo dõi qua thời gian
Ghi nhận các thay đổi về sắc lá, độ khô của giá thể và hình dáng cụm cây theo môi trường của bạn. Ảnh minh họa là một thời điểm tham khảo, không phải cam kết rằng cây luôn giữ nguyên màu hoặc kích thước. Thông tin bổ sung nên được đối chiếu với người bán trước khi quyết định chọn mẫu.','85000','0','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":12,"potDiameterCm":9,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-1');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá hoa thị cho góc bàn','demo-storefront-vuon-hien-2','## Giới thiệu
Sen đá hoa thị cho góc bàn có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Dáng tổng thể nhỏ gọn, thuận tiện thay đổi vị trí trưng bày.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá hoa thị cho góc bàn. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','90000','1','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":13,"potDiameterCm":10,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-2');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá lá xếp trong chậu nhỏ','demo-storefront-vuon-hien-3','## Giới thiệu
Sen đá lá xếp trong chậu nhỏ có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Bề mặt chậu mộc tạo cảm giác gần gũi khi đặt cạnh kệ gỗ.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá lá xếp trong chậu nhỏ. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','95000','5','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":14,"potDiameterCm":11,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-3');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá cụm tròn bên cửa sổ','demo-storefront-vuon-hien-4','## Giới thiệu
Sen đá cụm tròn bên cửa sổ có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Khoảng trống quanh sản phẩm giúp người xem đánh giá tỷ lệ rõ hơn.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá cụm tròn bên cửa sổ. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','100000','6','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":15,"potDiameterCm":12,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-4');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá xanh trong chậu mộc','demo-storefront-vuon-hien-5','## Giới thiệu
Sen đá xanh trong chậu mộc có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Mẫu này thích hợp để so sánh bố cục cùng các chậu khác trong gian hàng.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá xanh trong chậu mộc. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','105000','7','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":16,"potDiameterCm":13,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-5');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá hoa thị dáng thấp','demo-storefront-vuon-hien-6','## Giới thiệu
Sen đá hoa thị dáng thấp có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Thông tin kích thước được ghi theo mẫu minh họa riêng của sản phẩm.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá hoa thị dáng thấp. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','110000','8','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":17,"potDiameterCm":9,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-6');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá Echeveria chậu cao','demo-storefront-vuon-hien-7','## Giới thiệu
Sen đá Echeveria chậu cao có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Màu sắc nên được đối chiếu trong điều kiện ánh sáng thực tế.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá echeveria chậu cao. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','115000','9',NULL,'ACTIVE','{}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":null}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-7');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá hoa thị góc nắng','demo-storefront-vuon-hien-8','## Giới thiệu
Sen đá hoa thị góc nắng có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Miệng chậu được giữ thoáng để dễ quan sát bề mặt giá thể.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá hoa thị góc nắng. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.

## Gợi ý bố trí
Sen đá hoa thị góc nắng có thể được đặt trên một mặt kệ chắc chắn, chừa khoảng trống quanh chậu để quan sát lá và bề mặt giá thể. Khi đổi vị trí, hãy cân nhắc chênh lệch ánh sáng và độ thoáng giữa góc bàn, cửa sổ và hiên nhà; một bố cục đẹp vẫn cần phù hợp với điều kiện trồng thực tế.

## Theo dõi qua thời gian
Ghi nhận các thay đổi về sắc lá, độ khô của giá thể và hình dáng cụm cây theo môi trường của bạn. Ảnh minh họa là một thời điểm tham khảo, không phải cam kết rằng cây luôn giữ nguyên màu hoặc kích thước. Thông tin bổ sung nên được đối chiếu với người bán trước khi quyết định chọn mẫu.','120000','0','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":19,"potDiameterCm":11,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-8');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá lá dày chậu tròn','demo-storefront-vuon-hien-9','## Giới thiệu
Sen đá lá dày chậu tròn có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Dáng tổng thể nhỏ gọn, thuận tiện thay đổi vị trí trưng bày.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá lá dày chậu tròn. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','125000','1','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":20,"potDiameterCm":12,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-9');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá xanh cho kệ sách','demo-storefront-vuon-hien-10','## Giới thiệu
Sen đá xanh cho kệ sách có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Bề mặt chậu mộc tạo cảm giác gần gũi khi đặt cạnh kệ gỗ.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá xanh cho kệ sách. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','130000','4','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":12,"potDiameterCm":13,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-10');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá cụm nhỏ ban công','demo-storefront-vuon-hien-11','## Giới thiệu
Sen đá cụm nhỏ ban công có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Khoảng trống quanh sản phẩm giúp người xem đánh giá tỷ lệ rõ hơn.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá cụm nhỏ ban công. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','135000','5','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":13,"potDiameterCm":9,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-11');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá hoa thị chậu nâu','demo-storefront-vuon-hien-12','## Giới thiệu
Sen đá hoa thị chậu nâu có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Mẫu này thích hợp để so sánh bố cục cùng các chậu khác trong gian hàng.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá hoa thị chậu nâu. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','140000','6','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":14,"potDiameterCm":10,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-12');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá Echeveria góc làm việc','demo-storefront-vuon-hien-13','## Giới thiệu
Sen đá Echeveria góc làm việc có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Thông tin kích thước được ghi theo mẫu minh họa riêng của sản phẩm.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá echeveria góc làm việc. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','145000','7','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":15,"potDiameterCm":11,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-13');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá hoa thị dành cho bàn nhỏ','demo-storefront-vuon-hien-14','## Giới thiệu
Sen đá hoa thị dành cho bàn nhỏ có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Màu sắc nên được đối chiếu trong điều kiện ánh sáng thực tế.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá hoa thị dành cho bàn nhỏ. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','150000','8',NULL,'ACTIVE','{}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":null}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-14');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá lá xếp chậu miệng rộng','demo-storefront-vuon-hien-15','## Giới thiệu
Sen đá lá xếp chậu miệng rộng có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Miệng chậu được giữ thoáng để dễ quan sát bề mặt giá thể.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá lá xếp chậu miệng rộng. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','155000','0','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":17,"potDiameterCm":13,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-15');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá xanh cho hiên nhà','demo-storefront-vuon-hien-16','## Giới thiệu
Sen đá xanh cho hiên nhà có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Dáng tổng thể nhỏ gọn, thuận tiện thay đổi vị trí trưng bày.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá xanh cho hiên nhà. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','160000','1','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":18,"potDiameterCm":9,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-16');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá hoa thị trong chậu đơn','demo-storefront-vuon-hien-17','## Giới thiệu
Sen đá hoa thị trong chậu đơn có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Bề mặt chậu mộc tạo cảm giác gần gũi khi đặt cạnh kệ gỗ.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá hoa thị trong chậu đơn. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','165000','3','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":19,"potDiameterCm":10,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-17');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá cụm lá cân đối','demo-storefront-vuon-hien-18','## Giới thiệu
Sen đá cụm lá cân đối có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Khoảng trống quanh sản phẩm giúp người xem đánh giá tỷ lệ rõ hơn.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá cụm lá cân đối. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','170000','4','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":20,"potDiameterCm":11,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-18');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá Echeveria góc đọc sách','demo-storefront-vuon-hien-19','## Giới thiệu
Sen đá Echeveria góc đọc sách có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Mẫu này thích hợp để so sánh bố cục cùng các chậu khác trong gian hàng.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá echeveria góc đọc sách. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','175000','5','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":12,"potDiameterCm":12,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-19');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá hoa thị chậu đất mộc','demo-storefront-vuon-hien-20','## Giới thiệu
Sen đá hoa thị chậu đất mộc có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Thông tin kích thước được ghi theo mẫu minh họa riêng của sản phẩm.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá hoa thị chậu đất mộc. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','180000','6','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":13,"potDiameterCm":13,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-20');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá lá dày cho kệ gỗ','demo-storefront-vuon-hien-21','## Giới thiệu
Sen đá lá dày cho kệ gỗ có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Màu sắc nên được đối chiếu trong điều kiện ánh sáng thực tế.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá lá dày cho kệ gỗ. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','185000','7',NULL,'ACTIVE','{}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":null}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='vuon-hien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-vuon-hien-21');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá hoa thị chậu nắng','demo-storefront-nang-som-1','## Giới thiệu
Sen đá hoa thị chậu nắng có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Miệng chậu được giữ thoáng để dễ quan sát bề mặt giá thể.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá hoa thị chậu nắng. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','85000','0','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":12,"potDiameterCm":9,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='nang-som' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-nang-som-1');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá Echeveria chậu thô','demo-storefront-nang-som-2','## Giới thiệu
Sen đá Echeveria chậu thô có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Dáng tổng thể nhỏ gọn, thuận tiện thay đổi vị trí trưng bày.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá echeveria chậu thô. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','90000','1','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":13,"potDiameterCm":10,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='nang-som' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-nang-som-2');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá lá xếp kệ hiên','demo-storefront-nang-som-3','## Giới thiệu
Sen đá lá xếp kệ hiên có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Bề mặt chậu mộc tạo cảm giác gần gũi khi đặt cạnh kệ gỗ.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá lá xếp kệ hiên. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','95000','5','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":14,"potDiameterCm":11,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='nang-som' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-nang-som-3');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá xanh chậu nhỏ','demo-storefront-nang-som-4','## Giới thiệu
Sen đá xanh chậu nhỏ có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Khoảng trống quanh sản phẩm giúp người xem đánh giá tỷ lệ rõ hơn.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá xanh chậu nhỏ. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','100000','6','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":15,"potDiameterCm":12,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='nang-som' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-nang-som-4');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá hoa thị bên thềm','demo-storefront-nang-som-5','## Giới thiệu
Sen đá hoa thị bên thềm có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Mẫu này thích hợp để so sánh bố cục cùng các chậu khác trong gian hàng.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá hoa thị bên thềm. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','105000','7','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":16,"potDiameterCm":13,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='nang-som' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-nang-som-5');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá cụm lá chậu tròn','demo-storefront-nang-som-6','## Giới thiệu
Sen đá cụm lá chậu tròn có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Thông tin kích thước được ghi theo mẫu minh họa riêng của sản phẩm.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá cụm lá chậu tròn. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','110000','8','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":17,"potDiameterCm":9,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='nang-som' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-nang-som-6');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá Echeveria chậu mộc','demo-storefront-nang-som-7','## Giới thiệu
Sen đá Echeveria chậu mộc có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Màu sắc nên được đối chiếu trong điều kiện ánh sáng thực tế.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá echeveria chậu mộc. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','115000','9',NULL,'ACTIVE','{}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":null}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='nang-som' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-nang-som-7');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá hoa thị góc vườn','demo-storefront-goc-la-1','## Giới thiệu
Sen đá hoa thị góc vườn có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Miệng chậu được giữ thoáng để dễ quan sát bề mặt giá thể.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá hoa thị góc vườn. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','85000','0','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":12,"potDiameterCm":9,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='goc-la' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-goc-la-1');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá Echeveria chậu đất','demo-storefront-goc-la-2','## Giới thiệu
Sen đá Echeveria chậu đất có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Dáng tổng thể nhỏ gọn, thuận tiện thay đổi vị trí trưng bày.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá echeveria chậu đất. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','90000','1','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":13,"potDiameterCm":10,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='goc-la' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-goc-la-2');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Sen đá lá xếp cho ban công','demo-storefront-goc-la-3','## Giới thiệu
Sen đá lá xếp cho ban công có bố cục cụm lá gọn, phù hợp để sắp đặt một điểm xanh trên kệ. Mẫu được giới thiệu cùng chậu đất nung, chú trọng tương quan giữa tán lá và miệng chậu.

## Đặc điểm
Bề mặt chậu mộc tạo cảm giác gần gũi khi đặt cạnh kệ gỗ.

## Chăm sóc
Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của sen đá lá xếp cho ban công. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','95000','5','https://images.pexels.com/photos/33306623/pexels-photo-33306623.jpeg?auto=compress&cs=tinysrgb&w=1000','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":14,"potDiameterCm":11,"potMaterial":"Đất nung","light":"INDIRECT","watering":"Đặt ở nơi có ánh sáng phù hợp với điều kiện trồng. Kiểm tra độ khô của giá thể và khả năng thoát nước trước khi tưới, tránh áp dụng một lịch tưới cố định cho mọi góc nhà.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='sen-da' AND c.is_active=TRUE WHERE d.shop_key='goc-la' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-goc-la-3');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Bonsai dáng nghiêng chậu tối màu','demo-storefront-goc-la-4','## Giới thiệu
Bonsai dáng nghiêng chậu tối màu được chọn theo dáng thân và khoảng thoáng giữa các nhánh. Bộ mẫu giới thiệu cách bố trí một cây nhỏ trong không gian có ánh sáng; không xác nhận tuổi cây hoặc giống loài từ ảnh minh họa.

## Đặc điểm
Khoảng trống quanh sản phẩm giúp người xem đánh giá tỷ lệ rõ hơn.

## Chăm sóc
Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của bonsai dáng nghiêng chậu tối màu. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','645000','6','https://images.pexels.com/photos/19013774/pexels-photo-19013774/free-photo-of-bonsai-tree-in-pot.jpeg?auto=compress&dpr=1&h=750&w=1260','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":15,"light":"INDIRECT","watering":"Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='bonsai' AND c.is_active=TRUE WHERE d.shop_key='goc-la' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-goc-la-4');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Bonsai tán nhỏ góc vườn','demo-storefront-goc-la-5','## Giới thiệu
Bonsai tán nhỏ góc vườn được chọn theo dáng thân và khoảng thoáng giữa các nhánh. Bộ mẫu giới thiệu cách bố trí một cây nhỏ trong không gian có ánh sáng; không xác nhận tuổi cây hoặc giống loài từ ảnh minh họa.

## Đặc điểm
Mẫu này thích hợp để so sánh bố cục cùng các chậu khác trong gian hàng.

## Chăm sóc
Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của bonsai tán nhỏ góc vườn. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','710000','7','https://images.pexels.com/photos/19013774/pexels-photo-19013774/free-photo-of-bonsai-tree-in-pot.jpeg?auto=compress&dpr=1&h=750&w=1260','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":16,"light":"INDIRECT","watering":"Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='bonsai' AND c.is_active=TRUE WHERE d.shop_key='goc-la' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-goc-la-5');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Bonsai thân uốn chậu chữ nhật','demo-storefront-goc-la-6','## Giới thiệu
Bonsai thân uốn chậu chữ nhật được chọn theo dáng thân và khoảng thoáng giữa các nhánh. Bộ mẫu giới thiệu cách bố trí một cây nhỏ trong không gian có ánh sáng; không xác nhận tuổi cây hoặc giống loài từ ảnh minh họa.

## Đặc điểm
Thông tin kích thước được ghi theo mẫu minh họa riêng của sản phẩm.

## Chăm sóc
Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của bonsai thân uốn chậu chữ nhật. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','775000','8','https://images.pexels.com/photos/19013774/pexels-photo-19013774/free-photo-of-bonsai-tree-in-pot.jpeg?auto=compress&dpr=1&h=750&w=1260','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":17,"light":"INDIRECT","watering":"Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='bonsai' AND c.is_active=TRUE WHERE d.shop_key='goc-la' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-goc-la-6');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Bonsai tán thoáng trên kệ','demo-storefront-goc-la-7','## Giới thiệu
Bonsai tán thoáng trên kệ được chọn theo dáng thân và khoảng thoáng giữa các nhánh. Bộ mẫu giới thiệu cách bố trí một cây nhỏ trong không gian có ánh sáng; không xác nhận tuổi cây hoặc giống loài từ ảnh minh họa.

## Đặc điểm
Màu sắc nên được đối chiếu trong điều kiện ánh sáng thực tế.

## Chăm sóc
Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của bonsai tán thoáng trên kệ. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','840000','9',NULL,'ACTIVE','{}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":null}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='bonsai' AND c.is_active=TRUE WHERE d.shop_key='goc-la' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-goc-la-7');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Bonsai dáng nghiêng trong chậu','demo-storefront-moc-vien-1','## Giới thiệu
Bonsai dáng nghiêng trong chậu được chọn theo dáng thân và khoảng thoáng giữa các nhánh. Bộ mẫu giới thiệu cách bố trí một cây nhỏ trong không gian có ánh sáng; không xác nhận tuổi cây hoặc giống loài từ ảnh minh họa.

## Đặc điểm
Miệng chậu được giữ thoáng để dễ quan sát bề mặt giá thể.

## Chăm sóc
Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của bonsai dáng nghiêng trong chậu. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','450000','0','https://images.pexels.com/photos/19013774/pexels-photo-19013774/free-photo-of-bonsai-tree-in-pot.jpeg?auto=compress&dpr=1&h=750&w=1260','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":12,"light":"INDIRECT","watering":"Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='bonsai' AND c.is_active=TRUE WHERE d.shop_key='moc-vien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-moc-vien-1');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Bonsai tán nhỏ chậu chữ nhật','demo-storefront-moc-vien-2','## Giới thiệu
Bonsai tán nhỏ chậu chữ nhật được chọn theo dáng thân và khoảng thoáng giữa các nhánh. Bộ mẫu giới thiệu cách bố trí một cây nhỏ trong không gian có ánh sáng; không xác nhận tuổi cây hoặc giống loài từ ảnh minh họa.

## Đặc điểm
Dáng tổng thể nhỏ gọn, thuận tiện thay đổi vị trí trưng bày.

## Chăm sóc
Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của bonsai tán nhỏ chậu chữ nhật. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','515000','1','https://images.pexels.com/photos/19013774/pexels-photo-19013774/free-photo-of-bonsai-tree-in-pot.jpeg?auto=compress&dpr=1&h=750&w=1260','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":13,"light":"INDIRECT","watering":"Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='bonsai' AND c.is_active=TRUE WHERE d.shop_key='moc-vien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-moc-vien-2');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Bonsai thân cong góc hiên','demo-storefront-moc-vien-3','## Giới thiệu
Bonsai thân cong góc hiên được chọn theo dáng thân và khoảng thoáng giữa các nhánh. Bộ mẫu giới thiệu cách bố trí một cây nhỏ trong không gian có ánh sáng; không xác nhận tuổi cây hoặc giống loài từ ảnh minh họa.

## Đặc điểm
Bề mặt chậu mộc tạo cảm giác gần gũi khi đặt cạnh kệ gỗ.

## Chăm sóc
Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của bonsai thân cong góc hiên. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','580000','5','https://images.pexels.com/photos/19013774/pexels-photo-19013774/free-photo-of-bonsai-tree-in-pot.jpeg?auto=compress&dpr=1&h=750&w=1260','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":14,"light":"INDIRECT","watering":"Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='bonsai' AND c.is_active=TRUE WHERE d.shop_key='moc-vien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-moc-vien-3');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Bonsai tán rộng chậu thấp','demo-storefront-moc-vien-4','## Giới thiệu
Bonsai tán rộng chậu thấp được chọn theo dáng thân và khoảng thoáng giữa các nhánh. Bộ mẫu giới thiệu cách bố trí một cây nhỏ trong không gian có ánh sáng; không xác nhận tuổi cây hoặc giống loài từ ảnh minh họa.

## Đặc điểm
Khoảng trống quanh sản phẩm giúp người xem đánh giá tỷ lệ rõ hơn.

## Chăm sóc
Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của bonsai tán rộng chậu thấp. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','645000','6','https://images.pexels.com/photos/19013774/pexels-photo-19013774/free-photo-of-bonsai-tree-in-pot.jpeg?auto=compress&dpr=1&h=750&w=1260','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":15,"light":"INDIRECT","watering":"Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='bonsai' AND c.is_active=TRUE WHERE d.shop_key='moc-vien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-moc-vien-4');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Bonsai dáng tự nhiên trên bàn','demo-storefront-moc-vien-5','## Giới thiệu
Bonsai dáng tự nhiên trên bàn được chọn theo dáng thân và khoảng thoáng giữa các nhánh. Bộ mẫu giới thiệu cách bố trí một cây nhỏ trong không gian có ánh sáng; không xác nhận tuổi cây hoặc giống loài từ ảnh minh họa.

## Đặc điểm
Mẫu này thích hợp để so sánh bố cục cùng các chậu khác trong gian hàng.

## Chăm sóc
Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của bonsai dáng tự nhiên trên bàn. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','710000','7','https://images.pexels.com/photos/19013774/pexels-photo-19013774/free-photo-of-bonsai-tree-in-pot.jpeg?auto=compress&dpr=1&h=750&w=1260','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":16,"light":"INDIRECT","watering":"Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='bonsai' AND c.is_active=TRUE WHERE d.shop_key='moc-vien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-moc-vien-5');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Bonsai cành thoáng chậu tối màu','demo-storefront-moc-vien-6','## Giới thiệu
Bonsai cành thoáng chậu tối màu được chọn theo dáng thân và khoảng thoáng giữa các nhánh. Bộ mẫu giới thiệu cách bố trí một cây nhỏ trong không gian có ánh sáng; không xác nhận tuổi cây hoặc giống loài từ ảnh minh họa.

## Đặc điểm
Thông tin kích thước được ghi theo mẫu minh họa riêng của sản phẩm.

## Chăm sóc
Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của bonsai cành thoáng chậu tối màu. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','775000','8','https://images.pexels.com/photos/19013774/pexels-photo-19013774/free-photo-of-bonsai-tree-in-pot.jpeg?auto=compress&dpr=1&h=750&w=1260','ACTIVE','{"condition":"Mẫu cây trồng trong chậu","heightCm":17,"light":"INDIRECT","watering":"Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.","substrate":"Giá thể thoát nước theo mẫu minh họa"}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":"Pexels"}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='bonsai' AND c.is_active=TRUE WHERE d.shop_key='moc-vien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-moc-vien-6');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Bonsai góc vườn yên tĩnh','demo-storefront-moc-vien-7','## Giới thiệu
Bonsai góc vườn yên tĩnh được chọn theo dáng thân và khoảng thoáng giữa các nhánh. Bộ mẫu giới thiệu cách bố trí một cây nhỏ trong không gian có ánh sáng; không xác nhận tuổi cây hoặc giống loài từ ảnh minh họa.

## Đặc điểm
Màu sắc nên được đối chiếu trong điều kiện ánh sáng thực tế.

## Chăm sóc
Theo dõi độ ẩm của bầu đất và điều kiện ánh sáng tại vị trí đặt cây. Việc tỉa cành hoặc đổi chậu nên cân nhắc theo tình trạng cây, không thực hiện chỉ dựa vào ảnh.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của bonsai góc vườn yên tĩnh. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','840000','9',NULL,'ACTIVE','{}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":null}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='bonsai' AND c.is_active=TRUE WHERE d.shop_key='moc-vien' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-moc-vien-7');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Chậu đất nung tròn nhỏ','demo-storefront-nha-chau-1','## Giới thiệu
Chậu đất nung tròn nhỏ dành cho việc sắp đặt cây theo kích thước thực tế. Sản phẩm chỉ gồm vật dụng được nêu trong tên; cây và phụ kiện trang trí không đi kèm.

## Đặc điểm
Miệng chậu được giữ thoáng để dễ quan sát bề mặt giá thể.

## Sử dụng
Lau sạch chậu trước khi sử dụng. Kiểm tra lỗ thoát nước, cạnh miệng và đường kính bầu cây để chọn kích thước phù hợp; không dùng vật cứng chà lên bề mặt men.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của chậu đất nung tròn nhỏ. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','60000','0',NULL,'ACTIVE','{"condition":"Chậu mới trong bộ dữ liệu minh họa","potDiameterCm":9,"potMaterial":"Đất nung","careNotes":"Lau sạch chậu trước khi sử dụng. Kiểm tra lỗ thoát nước, cạnh miệng và đường kính bầu cây để chọn kích thước phù hợp; không dùng vật cứng chà lên bề mặt men."}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":null}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='chau-va-phu-kien' AND c.is_active=TRUE WHERE d.shop_key='nha-chau' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-nha-chau-1');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Chậu gốm men trắng miệng rộng','demo-storefront-nha-chau-2','## Giới thiệu
Chậu gốm men trắng miệng rộng dành cho việc sắp đặt cây theo kích thước thực tế. Sản phẩm chỉ gồm vật dụng được nêu trong tên; cây và phụ kiện trang trí không đi kèm.

## Đặc điểm
Dáng tổng thể nhỏ gọn, thuận tiện thay đổi vị trí trưng bày.

## Sử dụng
Lau sạch chậu trước khi sử dụng. Kiểm tra lỗ thoát nước, cạnh miệng và đường kính bầu cây để chọn kích thước phù hợp; không dùng vật cứng chà lên bề mặt men.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của chậu gốm men trắng miệng rộng. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','70000','1',NULL,'ACTIVE','{"condition":"Chậu mới trong bộ dữ liệu minh họa","potDiameterCm":10,"potMaterial":"Gốm","careNotes":"Lau sạch chậu trước khi sử dụng. Kiểm tra lỗ thoát nước, cạnh miệng và đường kính bầu cây để chọn kích thước phù hợp; không dùng vật cứng chà lên bề mặt men."}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":null}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='chau-va-phu-kien' AND c.is_active=TRUE WHERE d.shop_key='nha-chau' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-nha-chau-2');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Chậu đất mộc dáng thấp','demo-storefront-nha-chau-3','## Giới thiệu
Chậu đất mộc dáng thấp dành cho việc sắp đặt cây theo kích thước thực tế. Sản phẩm chỉ gồm vật dụng được nêu trong tên; cây và phụ kiện trang trí không đi kèm.

## Đặc điểm
Bề mặt chậu mộc tạo cảm giác gần gũi khi đặt cạnh kệ gỗ.

## Sử dụng
Lau sạch chậu trước khi sử dụng. Kiểm tra lỗ thoát nước, cạnh miệng và đường kính bầu cây để chọn kích thước phù hợp; không dùng vật cứng chà lên bề mặt men.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của chậu đất mộc dáng thấp. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','80000','5',NULL,'ACTIVE','{"condition":"Chậu mới trong bộ dữ liệu minh họa","potDiameterCm":11,"potMaterial":"Đất nung","careNotes":"Lau sạch chậu trước khi sử dụng. Kiểm tra lỗ thoát nước, cạnh miệng và đường kính bầu cây để chọn kích thước phù hợp; không dùng vật cứng chà lên bề mặt men."}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":null}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='chau-va-phu-kien' AND c.is_active=TRUE WHERE d.shop_key='nha-chau' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-nha-chau-3');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Chậu gốm xanh cho bàn cây','demo-storefront-nha-chau-4','## Giới thiệu
Chậu gốm xanh cho bàn cây dành cho việc sắp đặt cây theo kích thước thực tế. Sản phẩm chỉ gồm vật dụng được nêu trong tên; cây và phụ kiện trang trí không đi kèm.

## Đặc điểm
Khoảng trống quanh sản phẩm giúp người xem đánh giá tỷ lệ rõ hơn.

## Sử dụng
Lau sạch chậu trước khi sử dụng. Kiểm tra lỗ thoát nước, cạnh miệng và đường kính bầu cây để chọn kích thước phù hợp; không dùng vật cứng chà lên bề mặt men.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của chậu gốm xanh cho bàn cây. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','90000','6',NULL,'ACTIVE','{"condition":"Chậu mới trong bộ dữ liệu minh họa","potDiameterCm":12,"potMaterial":"Gốm","careNotes":"Lau sạch chậu trước khi sử dụng. Kiểm tra lỗ thoát nước, cạnh miệng và đường kính bầu cây để chọn kích thước phù hợp; không dùng vật cứng chà lên bề mặt men."}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":null}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='chau-va-phu-kien' AND c.is_active=TRUE WHERE d.shop_key='nha-chau' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-nha-chau-4');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Khay lót chậu đất nung','demo-storefront-nha-chau-5','## Giới thiệu
Khay lót chậu đất nung dành cho việc sắp đặt cây theo kích thước thực tế. Sản phẩm chỉ gồm vật dụng được nêu trong tên; cây và phụ kiện trang trí không đi kèm.

## Đặc điểm
Mẫu này thích hợp để so sánh bố cục cùng các chậu khác trong gian hàng.

## Sử dụng
Lau sạch chậu trước khi sử dụng. Kiểm tra lỗ thoát nước, cạnh miệng và đường kính bầu cây để chọn kích thước phù hợp; không dùng vật cứng chà lên bề mặt men.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của khay lót chậu đất nung. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','100000','7',NULL,'ACTIVE','{"condition":"Chậu mới trong bộ dữ liệu minh họa","potDiameterCm":13,"potMaterial":"Đất nung","careNotes":"Lau sạch chậu trước khi sử dụng. Kiểm tra lỗ thoát nước, cạnh miệng và đường kính bầu cây để chọn kích thước phù hợp; không dùng vật cứng chà lên bề mặt men."}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":null}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='chau-va-phu-kien' AND c.is_active=TRUE WHERE d.shop_key='nha-chau' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-nha-chau-5');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Chậu gốm nâu hình bầu dục','demo-storefront-nha-chau-6','## Giới thiệu
Chậu gốm nâu hình bầu dục dành cho việc sắp đặt cây theo kích thước thực tế. Sản phẩm chỉ gồm vật dụng được nêu trong tên; cây và phụ kiện trang trí không đi kèm.

## Đặc điểm
Thông tin kích thước được ghi theo mẫu minh họa riêng của sản phẩm.

## Sử dụng
Lau sạch chậu trước khi sử dụng. Kiểm tra lỗ thoát nước, cạnh miệng và đường kính bầu cây để chọn kích thước phù hợp; không dùng vật cứng chà lên bề mặt men.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của chậu gốm nâu hình bầu dục. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','110000','8',NULL,'ACTIVE','{"condition":"Chậu mới trong bộ dữ liệu minh họa","potDiameterCm":9,"potMaterial":"Gốm","careNotes":"Lau sạch chậu trước khi sử dụng. Kiểm tra lỗ thoát nước, cạnh miệng và đường kính bầu cây để chọn kích thước phù hợp; không dùng vật cứng chà lên bề mặt men."}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":null}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='chau-va-phu-kien' AND c.is_active=TRUE WHERE d.shop_key='nha-chau' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-nha-chau-6');
INSERT INTO products(store_id,category_id,name,slug,description,price,stock,image_url,status,technical_specs_json,metadata_json) SELECT s.id,c.id,'Chậu đất nung cho sen đá','demo-storefront-nha-chau-7','## Giới thiệu
Chậu đất nung cho sen đá dành cho việc sắp đặt cây theo kích thước thực tế. Sản phẩm chỉ gồm vật dụng được nêu trong tên; cây và phụ kiện trang trí không đi kèm.

## Đặc điểm
Màu sắc nên được đối chiếu trong điều kiện ánh sáng thực tế.

## Sử dụng
Lau sạch chậu trước khi sử dụng. Kiểm tra lỗ thoát nước, cạnh miệng và đường kính bầu cây để chọn kích thước phù hợp; không dùng vật cứng chà lên bề mặt men.

## Lưu ý trước khi chọn
Đối chiếu mô tả, kích thước và tình trạng của chậu đất nung cho sen đá. Đây là dữ liệu minh họa cho môi trường phát triển; ảnh thư viện không xác nhận một cây cụ thể của người bán thực.','120000','9',NULL,'ACTIVE','{}','{"developmentDemo":true,"seed":"storefront-v1","imageSource":null}' FROM storefront_seed_owners d JOIN users u ON u.email=d.email JOIN stores s ON s.owner_user_id=u.id JOIN categories c ON c.slug='chau-va-phu-kien' AND c.is_active=TRUE WHERE d.shop_key='nha-chau' AND NOT EXISTS (SELECT 1 FROM products WHERE slug='demo-storefront-nha-chau-7');
DROP TEMPORARY TABLE storefront_seed_owners;
