package edu.buffalo.cse.cse486586.simpledht;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class Message implements Serializable {

    String msg = "";
    String sourcePort = "1234";
    String destinationPort = "";
    String currentStatus = "";
    TreeMap<String, String> tm = new TreeMap<String, String>();
    String cvK = null;
    String cvV = null;
    List<String> cursorList = new ArrayList<String>();


    public List<String> getCursorList() {
        return cursorList;
    }

    public void setCursorList(List<String> cursorList) {
        this.cursorList = cursorList;
    }


    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(String sourcePort) {
        this.sourcePort = sourcePort;
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

    public TreeMap<String, String> getTm() {
        return tm;
    }

    public void setTm(TreeMap<String, String> tm) {
        this.tm = tm;
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

    @Override
    public String toString() {
        return "Message{" +
                "msg='" + msg + '\'' +
                ", sourcePort='" + sourcePort + '\'' +
                ", destinationPort='" + destinationPort + '\'' +
                ", currentStatus='" + currentStatus + '\'' +
                ", tm=" + tm +
                ", cvK='" + cvK + '\'' +
                ", cvV='" + cvV + '\'' +
                ", cursorList=" + cursorList +
                '}';
    }
}