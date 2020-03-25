package com.lan.cang.backupp2p.model;

import java.io.Serializable;

import androidx.annotation.NonNull;

public class FileTransfer implements Serializable {

    //文件路径
    private String filePath;

    //文件大小
    private long fileLength;

    //MD5码
    private String md5;

    public FileTransfer(String name, long fileLength) {
        this.filePath = name;
        this.fileLength = fileLength;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    @NonNull
    @Override
    public String toString() {
        return "FileTransfer{" +
                "filePath='" + filePath + '\'' +
                ", fileLength=" + fileLength +
                ", md5='" + md5 + '\'' +
                '}';
    }

}