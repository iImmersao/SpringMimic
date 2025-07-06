package com.iimmersao.springmimic.security;

import com.iimmersao.springmimic.exceptions.UnauthorizedException;
import fi.iki.elonen.NanoHTTPD;

public interface AuthManager {
    void authenticate(NanoHTTPD.IHTTPSession session) throws UnauthorizedException;
}
