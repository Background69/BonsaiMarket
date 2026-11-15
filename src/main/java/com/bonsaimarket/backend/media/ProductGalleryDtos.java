package com.bonsaimarket.backend.media;

import jakarta.validation.constraints.*;
import java.util.List;

public final class ProductGalleryDtos {
    private ProductGalleryDtos(){}
    public record Image(Long id,String imageUrl,String thumbnailUrl,int sortOrder,boolean primary,String contentType,
        Long fileSizeBytes,Integer width,Integer height,String source){}
    public record View(Long productId,long revision,boolean initialized,int maxImages,long maxBytes,String imageUrl,List<Image> images){}
    public record Upload(View gallery,Long imageId,boolean duplicate,boolean replayed){}
    public record Revision(@NotNull @Min(0) @tools.jackson.databind.annotation.JsonDeserialize(using=GalleryRevisionDeserializer.class) Long revision){}
    public record Reorder(@NotNull @Min(0) @tools.jackson.databind.annotation.JsonDeserialize(using=GalleryRevisionDeserializer.class) Long revision,
        @NotNull @Size(max=8) @tools.jackson.databind.annotation.JsonDeserialize(contentUsing=GalleryRevisionDeserializer.class) List<@NotNull @Positive Long> imageIds){}
}
