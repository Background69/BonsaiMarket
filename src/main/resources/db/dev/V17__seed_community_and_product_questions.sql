-- DEV ONLY. Insert-only synthetic discussion content; no login-capable demo passwords.
-- Capture only unused identities so a colliding real account is never adopted.
CREATE TEMPORARY TABLE discussion_seed_authors (email VARCHAR(255) PRIMARY KEY, display_name VARCHAR(150));
INSERT INTO discussion_seed_authors SELECT 'discussion-v1-lan@example.invalid','Lan · Demo' WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='discussion-v1-lan@example.invalid');
INSERT INTO discussion_seed_authors SELECT 'discussion-v1-minh@example.invalid','Minh · Demo' WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='discussion-v1-minh@example.invalid');
INSERT INTO discussion_seed_authors SELECT 'discussion-v1-an@example.invalid','An · Demo' WHERE NOT EXISTS (SELECT 1 FROM users WHERE email='discussion-v1-an@example.invalid');
INSERT INTO users(name,email,password_hash,role,is_active,email_verified_at)
SELECT display_name,email,'!discussion-demo-login-disabled!','CUSTOMER',TRUE,CURRENT_TIMESTAMP(6) FROM discussion_seed_authors;

INSERT INTO community_posts(author_id,title,body,topic,seed_key)
SELECT u.id,'Quan sát giá thể sen đá trước khi tưới','Mình đặt sen đá ở ban công có mái che. Thay vì tưới theo ngày cố định, mình quan sát độ khô và độ thoáng của giá thể.\n\nCác bạn thường ghi lại những dấu hiệu nào để điều chỉnh cách chăm cây? Đây là cuộc thảo luận minh họa trong môi trường phát triển.','SUCCULENTS','discussion-v1-succulents'
FROM users u JOIN discussion_seed_authors a ON a.email=u.email WHERE u.email='discussion-v1-lan@example.invalid' AND NOT EXISTS(SELECT 1 FROM community_posts WHERE seed_key='discussion-v1-succulents');
INSERT INTO community_posts(author_id,title,body,topic,seed_key)
SELECT u.id,'Chọn vị trí ngắm một dáng bonsai nhỏ','Mình đang sắp xếp lại kệ bonsai và thấy khoảng trống quanh cây ảnh hưởng nhiều đến cách nhìn đường thân.\n\nBạn thích đặt cây ngang tầm mắt hay thấp hơn một chút? Bài viết minh họa để trao đổi về bố cục, không khẳng định một thế cây cụ thể.','BONSAI','discussion-v1-bonsai'
FROM users u JOIN discussion_seed_authors a ON a.email=u.email WHERE u.email='discussion-v1-minh@example.invalid' AND NOT EXISTS(SELECT 1 FROM community_posts WHERE seed_key='discussion-v1-bonsai');
INSERT INTO community_posts(author_id,title,body,topic,seed_key)
SELECT u.id,'Ghi chép thay đổi của góc vườn qua mùa','Mình chụp cùng một góc vườn vào đầu mỗi tháng để so sánh ánh sáng và màu lá. Những ghi chú nhỏ giúp mình nhớ được thay đổi sau khi chuyển chậu.\n\nCác bạn có cách ghi chép đơn giản nào muốn chia sẻ? Đây là nội dung phát triển, không phải bài của thành viên thật.','CARE','discussion-v1-care'
FROM users u JOIN discussion_seed_authors a ON a.email=u.email WHERE u.email='discussion-v1-an@example.invalid' AND NOT EXISTS(SELECT 1 FROM community_posts WHERE seed_key='discussion-v1-care');
INSERT INTO community_posts(author_id,title,body,topic,seed_key)
SELECT u.id,'Một chiếc kệ nhỏ cho những chậu cây mới','Mình muốn chừa khoảng cách giữa các chậu để dễ quan sát và vệ sinh kệ. Đang cân nhắc cách sắp xếp sao cho cây nào cũng có ánh sáng phù hợp.\n\nBài chia sẻ minh họa dành cho môi trường phát triển.','OTHER','discussion-v1-shelf'
FROM users u JOIN discussion_seed_authors a ON a.email=u.email WHERE u.email='discussion-v1-lan@example.invalid' AND NOT EXISTS(SELECT 1 FROM community_posts WHERE seed_key='discussion-v1-shelf');

INSERT INTO community_comments(post_id,author_id,body,seed_key)
SELECT p.id,u.id,'Mình thường ghi thêm vị trí đặt cây và thời tiết hôm đó, nhờ vậy dễ đối chiếu hơn khi lá có thay đổi.','discussion-v1-comment-care'
FROM community_posts p JOIN users u ON u.email='discussion-v1-minh@example.invalid' JOIN discussion_seed_authors a ON a.email=u.email WHERE p.seed_key='discussion-v1-care' AND NOT EXISTS(SELECT 1 FROM community_comments WHERE seed_key='discussion-v1-comment-care');
INSERT INTO community_comments(post_id,author_id,body,seed_key)
SELECT p.id,u.id,'Mình còn nhìn các lỗ thoát nước dưới đáy chậu. Cùng một loại giá thể nhưng mỗi vị trí trồng lại khô khác nhau.','discussion-v1-comment-succulents'
FROM community_posts p JOIN users u ON u.email='discussion-v1-an@example.invalid' JOIN discussion_seed_authors a ON a.email=u.email WHERE p.seed_key='discussion-v1-succulents' AND NOT EXISTS(SELECT 1 FROM community_comments WHERE seed_key='discussion-v1-comment-succulents');

-- Answers are attributed to the actual owner of the matching demo product only.
INSERT INTO product_questions(product_id,author_id,body,answer_author_id,answer_body,answered_at,seed_key)
SELECT p.id,u.id,'Mẫu sen đá này có kèm chậu đang được mô tả không?',s.owner_user_id,'Mẫu minh họa này gồm cây và chậu đất nung như nội dung sản phẩm.',CURRENT_TIMESTAMP(6),'discussion-v1-question-succulent-answered'
FROM products p JOIN stores s ON s.id=p.store_id JOIN users owner ON owner.id=s.owner_user_id JOIN users u ON u.email='discussion-v1-lan@example.invalid' JOIN discussion_seed_authors a ON a.email=u.email
WHERE p.slug='demo-storefront-vuon-hien-2' AND p.status='ACTIVE' AND s.status='ACTIVE' AND owner.email='storefront-v1-vuon-hien@example.invalid' AND owner.password_hash='!development-account-login-disabled!' AND NOT EXISTS(SELECT 1 FROM product_questions WHERE seed_key='discussion-v1-question-succulent-answered');
INSERT INTO product_questions(product_id,author_id,body,seed_key)
SELECT p.id,u.id,'Mình có thể xem thêm thông tin đường kính chậu của mẫu này không?','discussion-v1-question-succulent-open'
FROM products p JOIN stores s ON s.id=p.store_id JOIN users owner ON owner.id=s.owner_user_id JOIN users u ON u.email='discussion-v1-minh@example.invalid' JOIN discussion_seed_authors a ON a.email=u.email
WHERE p.slug='demo-storefront-vuon-hien-2' AND p.status='ACTIVE' AND s.status='ACTIVE' AND owner.email='storefront-v1-vuon-hien@example.invalid' AND owner.password_hash='!development-account-login-disabled!' AND NOT EXISTS(SELECT 1 FROM product_questions WHERE seed_key='discussion-v1-question-succulent-open');
INSERT INTO product_questions(product_id,author_id,body,answer_author_id,answer_body,answered_at,seed_key)
SELECT p.id,u.id,'Chiều cao bonsai được tính từ mặt chậu hay gồm cả chậu?',s.owner_user_id,'Thông số trong mẫu minh họa được tính từ mặt giá thể đến ngọn cây.',CURRENT_TIMESTAMP(6),'discussion-v1-question-bonsai-answered'
FROM products p JOIN stores s ON s.id=p.store_id JOIN users owner ON owner.id=s.owner_user_id JOIN users u ON u.email='discussion-v1-an@example.invalid' JOIN discussion_seed_authors a ON a.email=u.email
WHERE p.slug='demo-storefront-moc-vien-1' AND p.status='ACTIVE' AND s.status='ACTIVE' AND owner.email='storefront-v1-moc-vien@example.invalid' AND owner.password_hash='!development-account-login-disabled!' AND NOT EXISTS(SELECT 1 FROM product_questions WHERE seed_key='discussion-v1-question-bonsai-answered');
INSERT INTO product_questions(product_id,author_id,body,seed_key)
SELECT p.id,u.id,'Dáng cây trong mô tả phù hợp để nhìn từ phía nào?','discussion-v1-question-bonsai-open'
FROM products p JOIN stores s ON s.id=p.store_id JOIN users owner ON owner.id=s.owner_user_id JOIN users u ON u.email='discussion-v1-lan@example.invalid' JOIN discussion_seed_authors a ON a.email=u.email
WHERE p.slug='demo-storefront-moc-vien-2' AND p.status='ACTIVE' AND s.status='ACTIVE' AND owner.email='storefront-v1-moc-vien@example.invalid' AND owner.password_hash='!development-account-login-disabled!' AND NOT EXISTS(SELECT 1 FROM product_questions WHERE seed_key='discussion-v1-question-bonsai-open');
DROP TEMPORARY TABLE discussion_seed_authors;
