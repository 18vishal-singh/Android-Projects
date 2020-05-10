package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

public class Message implements Serializable {

    int sourceX;
    int sourceY;
    int proposedX = Integer.MIN_VALUE;
    int proposedY = Integer.MIN_VALUE;
    String message;
    int sourcePort;
    boolean isFinal = false;

    int failedPort =0;

    @Override
    public String toString() {
        return "Message{" +
                "sourceX=" + sourceX +
                ", sourceY=" + sourceY +
                ", proposedX=" + proposedX +
                ", proposedY=" + proposedY +
                ", message='" + message + '\'' +
                ", sourcePort=" + sourcePort +
                ", isFinal=" + isFinal +
                ", failedPort=" + failedPort +
                '}';
    }

    public int getFailedPort() {
        return failedPort;
    }

    public void setFailedPort(int failedPort) {
        this.failedPort = failedPort;
    }

    public int getSourceX() {
        return sourceX;
    }

    public void setSourceX(int sourceX) {
        this.sourceX = sourceX;
    }

    public int getSourceY() {
        return sourceY;
    }

    public void setSourceY(int sourceY) {
        this.sourceY = sourceY;
    }

    public int getProposedX() {
        return proposedX;
    }

    public void setProposedX(int proposedX) {
        this.proposedX = proposedX;
    }

    public int getProposedY() {
        return proposedY;
    }

    public void setProposedY(int proposedY) {
        this.proposedY = proposedY;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }


}