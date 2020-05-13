package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Message implements Serializable {

    String msg = "";

    String destinationPort = "";
    String currentStatus = "";
    String cvK = null;
    String cvV = null;
    List<String> ports = new ArrayList<String>();
    List<String> cursorList = new ArrayList<String>();
    HashMap<String,String> hm = new HashMap<String,String>();


    public HashMap<String, String> getHm() {
        return hm;
    }

    public void setHm(HashMap<String, String> hm) {
        this.hm = hm;
    }

    @Override
    public String toString() {
        return "Message{" +
                "msg='" + msg + '\'' +
                ", destinationPort='" + destinationPort + '\'' +
                ", currentStatus='" + currentStatus + '\'' +
                ", cvK='" + cvK + '\'' +
                ", cvV='" + cvV + '\'' +
                ", ports=" + ports +
                ", cursorList=" + cursorList +
                ", hm=" + hm +
                '}';
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(String destinationPort) {
        this.destinationPort = destinationPort;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getCvK() {
        return cvK;
    }

    public void setCvK(String cvK) {
        this.cvK = cvK;
    }

    public String getCvV() {
        return cvV;
    }

    public void setCvV(String cvV) {
        this.cvV = cvV;
    }

    public List<String> getPorts() {
        return ports;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    public List<String> getCursorList() {
        return cursorList;
    }

    public void setCursorList(List<String> cursorList) {
        this.cursorList = cursorList;
    }
}