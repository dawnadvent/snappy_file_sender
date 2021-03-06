package com.suvankarmitra.sender;

import com.suvankarmitra.data.Constants;
import com.suvankarmitra.data.FTConnection;
import javafx.concurrent.Task;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FTSender {
    private FTConnection ftConnection;
    private String filePath;

    private ServerSocket serverSocket;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    Thread processFileThread;

    private volatile double processFileProgress;
    private volatile boolean processingFileDone = false;
    private boolean waitingForReceiver = true;

    public FTConnection getFtConnection() {
        return ftConnection;
    }

    public double getProcessFileProgress() {
        return processFileProgress;
    }

    public boolean isProcessingFileDone() {
        return processingFileDone;
    }

    public boolean isWaitingForReceiver() {
        return waitingForReceiver;
    }

    public FTSender(FTConnection ftConnection, String filePath) throws Exception {
        this.ftConnection = ftConnection;
        this.filePath = filePath;
    }

    public void processFile() throws Exception {
        System.out.println(filePath!=null);
        File file = new File(filePath);
        if(!file.exists()) {
            Exception e = new Exception("File "+file+" does not exist.");
            throw e;
        }
        if(!file.canRead()) {
            throw new Exception("File "+file+" is not readable.");
        }

        processFileThread = new Thread(()->{
            try {
                zipAFile(filePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        processFileThread.start();
    }

    public void openConnectionAndWait() throws Exception {
        System.out.println(ftConnection);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            executorService.submit(new Task() {
                @Override
                protected Object call() throws Exception {
                    processingFileDone = false;
                    serverSocket = new ServerSocket(ftConnection.getPort());
                    System.out.println("Socket opening - "+serverSocket);
                    socket = serverSocket.accept();
                    System.out.println("Socket opened - "+socket);
                    waitingForReceiver = false; // connected with receiver
                    ftConnection.setRemoteIP(socket.getRemoteSocketAddress().toString().substring(1));
                    dis = new DataInputStream(socket.getInputStream());
                    dos = new DataOutputStream(socket.getOutputStream());
                    return null;
                }
            }).get(Constants.SENDER_WAIT_TIME, TimeUnit.SECONDS);
        } catch (Exception e) {
            waitingForReceiver = false;
            throw e;
        }

    }

    public void close() throws IOException {
        processFileThread.interrupt();
        if(socket!=null) socket.close();
        if(serverSocket!=null) serverSocket.close();
        System.out.println("Connections closed");
    }

    public void zipAFile(String sourceFile) throws IOException {
        File source = new File(sourceFile);
        long fileSize = source.length();
        double processedSize = 0;

        FileOutputStream fos = new FileOutputStream(sourceFile+".zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(sourceFile);
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while(!Thread.currentThread().isInterrupted() && (length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
            processedSize += length;
            processFileProgress = processedSize / (fileSize*1.0);
            //System.out.println("Zip file progress: "+processFileProgress);
        }
        zipOut.close();
        fis.close();
        fos.close();
        processingFileDone = true;
        //System.out.println("Processing file done: "+processingFileDone);
        if(Thread.currentThread().isInterrupted()) {
            processFileProgress = 0;
        }
    }
}
