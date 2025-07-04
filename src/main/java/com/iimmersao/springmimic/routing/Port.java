package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.Component;

@Component
public class Port {
    int portNo;

    /*
    public Port() {
        portNo = 8080;
    }
     */

    public Port(int portNo) {
        this.portNo = portNo;
    }

    public int getPortNo() {
        return portNo;
    }

    @Override
    public String toString() {
        return "" + portNo;
    }
}
