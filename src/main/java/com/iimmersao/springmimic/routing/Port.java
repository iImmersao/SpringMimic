package com.iimmersao.springmimic.routing;

import com.iimmersao.springmimic.annotations.Component;

@Component
public class Port {
    private final int portNo;

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
