package com.bonsaimarket.backend.media;

import com.bonsaimarket.backend.marketplace.MarketplaceException;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import org.springframework.web.multipart.MultipartFile;

/** Decode by signature with bounded dimensions; callers re-encode to strip metadata and trailing bytes. */
public final class SafeImage {
    private SafeImage() {}
    public static BufferedImage decode(MultipartFile file,long maxBytes) throws IOException {
        if(file.isEmpty()||file.getSize()>maxBytes)throw new MarketplaceException(413,"IMAGE_TOO_LARGE","Ảnh phải có dữ liệu và không vượt quá 5 MB.");
        byte[] bytes;try(var stream=file.getInputStream()){bytes=stream.readNBytes((int)Math.min(maxBytes+1,Integer.MAX_VALUE));}
        if(bytes.length>maxBytes)throw new MarketplaceException(413,"IMAGE_TOO_LARGE","Ảnh vượt giới hạn dung lượng.");
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw invalid();var reader=readers.next();
            try{reader.setInput(input,true,true);String format=reader.getFormatName();if(!format.equalsIgnoreCase("JPEG")&&!format.equalsIgnoreCase("PNG"))throw invalid();
                int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||w>6000||h>6000||(long)w*h>20_000_000)throw MarketplaceException.invalid("Ảnh tối đa 6000 px mỗi chiều và 20 triệu điểm ảnh.");
                var decoded=reader.read(0);if(decoded==null)throw invalid();return decoded;
            }finally{reader.dispose();}
        }catch(IOException e){throw invalid();}
    }
    private static MarketplaceException invalid(){return MarketplaceException.invalid("Chỉ chấp nhận ảnh JPEG hoặc PNG hợp lệ.");}
}
