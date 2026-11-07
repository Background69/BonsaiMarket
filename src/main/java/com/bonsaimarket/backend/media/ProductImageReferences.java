package com.bonsaimarket.backend.media;

import jakarta.persistence.EntityManager;
import java.net.URI;
import java.util.*;

/** Known image metadata owners, including absolute URLs that reference the same serving path. */
final class ProductImageReferences {
    private ProductImageReferences(){}
    static boolean storage(EntityManager em,String key){
        return em.createQuery("select count(g) from ProductGalleryImage g where g.storageKey=:key or g.thumbnailKey=:key",Long.class).setParameter("key",key).getSingleResult()>0
            ||em.createQuery("select count(i) from ProductImage i where i.fileKey=:key",Long.class).setParameter("key",key).getSingleResult()>0;
    }
    static boolean urls(EntityManager em,List<String> urls,Long ownProduct,Long ownImage){
        for(String url:urls){
            String path;try{path=URI.create(url).getPath();}catch(IllegalArgumentException e){continue;}
            if(path==null||!path.startsWith("/api/products/"))continue;
            String escaped=path.replace("!","!!").replace("%","!%").replace("_","!_");
            for(var entry:List.of(new String[]{"Product p","p.imageUrl","p.id",Objects.toString(ownProduct,"-1")},
                new String[]{"ProductGalleryImage g","g.imageUrl","g.id",Objects.toString(ownImage,"-1")},
                new String[]{"Category c","c.imageUrl","c.id","-1"},new String[]{"Store s","s.logoUrl","s.id","-1"},new String[]{"Store s","s.bannerUrl","s.id","-1"})){
                String field=entry[1],alias=entry[0].substring(entry[0].lastIndexOf(' ')+1);var query=em.createQuery("select count("+alias+") from "+entry[0]+" where "+entry[2]+"<>:excluded and ("+field+" like :path escape '!' or "+field+" like :query escape '!' or "+field+" like :fragment escape '!')",Long.class);
                if(query.setParameter("excluded",Long.parseLong(entry[3])).setParameter("path","%"+escaped).setParameter("query","%"+escaped+"?%").setParameter("fragment","%"+escaped+"#%").getSingleResult()>0)return true;
            }
        }return false;
    }
}
