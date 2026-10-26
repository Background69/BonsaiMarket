package com.bonsaimarket.backend.checkout;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Loopback SMTP sink only. No AUTH/TLS or provider deliverability claim. */
final class OrderSmtpSink implements AutoCloseable {
    final ServerSocket server;final List<byte[]> messages=new CopyOnWriteArrayList<>();final ExecutorService workers=Executors.newVirtualThreadPerTaskExecutor();volatile boolean dropAfterData;
    OrderSmtpSink(){try{server=new ServerSocket(0,10,InetAddress.getLoopbackAddress());workers.submit(()->{while(!server.isClosed()){try{var socket=server.accept();workers.submit(()->handle(socket));}catch(IOException e){if(!server.isClosed())throw new UncheckedIOException(e);}}});}catch(IOException e){throw new UncheckedIOException(e);}}
    int port(){return server.getLocalPort();}
    void handle(Socket socket){try(socket){socket.setSoTimeout(10000);var input=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));var output=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8));reply(output,"220 isolated-test-smtp");boolean data=false;var message=new StringBuilder();String line;
        while((line=input.readLine())!=null){if(data){if(line.equals(".")){messages.add(message.toString().getBytes(StandardCharsets.UTF_8));data=false;if(dropAfterData){dropAfterData=false;return;}reply(output,"250 accepted by test sink");}else message.append(line.startsWith("..")?line.substring(1):line).append("\r\n");continue;}
            if(line.startsWith("EHLO")||line.startsWith("HELO"))reply(output,"250 isolated-test-smtp");else if(line.startsWith("MAIL FROM")||line.startsWith("RCPT TO")||line.equals("RSET"))reply(output,"250 OK");else if(line.equals("DATA")){data=true;reply(output,"354 send data");}else if(line.equals("QUIT")){reply(output,"221 bye");return;}else reply(output,"250 OK");}
    }catch(IOException ignored){}}
    void reply(BufferedWriter out,String text)throws IOException{out.write(text+"\r\n");out.flush();}
    public void close()throws IOException{server.close();workers.shutdownNow();}
}
