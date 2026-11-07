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
        return decode(file,maxBytes,false);
    }
    public static BufferedImage decodeProduct(MultipartFile file,long maxBytes) throws IOException {
        return decode(file,maxBytes,true);
    }
    private static BufferedImage decode(MultipartFile file,long maxBytes,boolean allowWebp) throws IOException {
        if(file.isEmpty()||file.getSize()>maxBytes)throw new MarketplaceException(413,"IMAGE_TOO_LARGE","Ảnh phải có dữ liệu và không vượt quá 5 MB.");
        byte[] bytes;try(var stream=file.getInputStream()){bytes=stream.readNBytes((int)Math.min(maxBytes+1,Integer.MAX_VALUE));}
        if(bytes.length>maxBytes)throw new MarketplaceException(413,"IMAGE_TOO_LARGE","Ảnh vượt giới hạn dung lượng.");
        String signature=bytes.length>=8&&bytes[0]==(byte)0x89&&bytes[1]=='P'&&bytes[2]=='N'&&bytes[3]=='G'&&bytes[4]==13&&bytes[5]==10&&bytes[6]==26&&bytes[7]==10?"PNG":
            bytes.length>=3&&bytes[0]==(byte)0xff&&bytes[1]==(byte)0xd8&&bytes[2]==(byte)0xff?"JPEG":
            allowWebp&&bytes.length>=12&&new String(bytes,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")&&new String(bytes,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")?"WEBP":null;
        if(signature==null)throw unsupported(allowWebp);
        // Browser MIME/extension are untrusted. The signature, decoder and PNG re-encoding own the served MIME.
        try(var input=new javax.imageio.stream.MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw unsupported(allowWebp);var reader=readers.next();
            try{reader.setInput(input,true,true);String format=reader.getFormatName();if(!format.equalsIgnoreCase(signature))throw unsupported(allowWebp);
                int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||w>6000||h>6000||(long)w*h>20_000_000)throw MarketplaceException.invalid("Ảnh tối đa 6000 px mỗi chiều và 20 triệu điểm ảnh.");
                var decoded=reader.read(0);if(decoded==null)throw unsupported(allowWebp);return decoded;
            }finally{reader.dispose();}
        }catch(IOException e){throw unsupported(allowWebp);}
    }
    private static MarketplaceException unsupported(boolean webp){return new MarketplaceException(400,"IMAGE_FORMAT_UNSUPPORTED",webp?"Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP hợp lệ; không nhận SVG.":"Chỉ chấp nhận ảnh JPEG hoặc PNG hợp lệ.");}
}
