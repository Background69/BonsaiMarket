package com.bonsaimarket.backend.catalog;

import java.nio.file.*;
import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Chạy SQL thật bằng H2/MySQL mode với các điều chỉnh đã ghi; không chứng minh MySQL thật. */
class ShippingLocationMigrationTest {
    @Test void additiveUpgradePreservesLegacyRowsAndEnforcesLocationAndShipmentConstraints()throws Exception{
        var helper=new StorefrontMigrationSeedTest();
        try(var c=DriverManager.getConnection("jdbc:h2:mem:shippingupgrade;MODE=MySQL","sa","")){
            helper.schema(c,true);
            long users=helper.scalar(c,"select count(*) from users"),orders=helper.scalar(c,"select count(*) from orders");String address=helper.text(c,"select shipping_address_snapshot from orders where id=1");
            assertEquals(users,helper.scalar(c,"select count(*) from users"));assertEquals(orders,helper.scalar(c,"select count(*) from orders"));assertEquals(address,helper.text(c,"select shipping_address_snapshot from orders where id=1"));
            try(var s=c.createStatement()){
                assertThrows(SQLException.class,()->s.execute("insert into store_public_locations(store_id,mode,published,region,ward,public_address,updated_at) values(1,'AREA',true,'Test','Test','',CURRENT_TIMESTAMP)"));
                assertThrows(SQLException.class,()->s.execute("insert into store_public_locations(store_id,mode,published,region,ward,public_address,latitude,longitude,consent_at,updated_at) values(1,'EXACT',true,'Test','Test','Public',91,106,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"));
                assertThrows(SQLException.class,()->s.execute("insert into shipping_parcels(order_id,weight_grams,length_cm,width_cm,height_cm,carrier_accepted_goods,updated_at) values(1,0,10,10,10,true,CURRENT_TIMESTAMP)"));
                s.execute("insert into shipping_quotes(id,order_id,buyer_id,shop_id,parcel_revision,service_type,service_name,fee,order_fingerprint,destination_snapshot,pickup_snapshot,parcel_snapshot,expires_at,created_at) select 'quote',o.id,o.buyer_user_id,123,1,2,'Test',25000,'fingerprint','{}','{}','{}',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP from orders o where o.id=1");
                s.execute("insert into shipments(id,order_id,quote_id,shop_id,client_reference,state,created_at,updated_at) values('shipment',1,'quote',123,'BM-stable','UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
                assertThrows(SQLException.class,()->s.execute("insert into shipments(id,order_id,quote_id,shop_id,client_reference,state,created_at,updated_at) values('duplicate',1,'quote',123,'BM-other','UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"));
                assertThrows(SQLException.class,()->s.execute("insert into shipments(id,order_id,quote_id,shop_id,client_reference,state,created_at,updated_at) values('wrong-order',999999,'quote',123,'BM-other','UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)"));
                s.execute("insert into shipment_events(shipment_id,dedup_key,carrier_status,state,event_at,received_at,source) values('shipment','event','picked','PICKED_UP',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'WEBHOOK')");
                assertThrows(SQLException.class,()->s.execute("insert into shipment_events(shipment_id,dedup_key,carrier_status,state,event_at,received_at,source) values('shipment','event','picked','PICKED_UP',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'WEBHOOK')"));
            }
        }
    }
}
