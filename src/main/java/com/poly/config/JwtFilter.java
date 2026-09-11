package com.poly.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.poly.utils.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
    	
    	String path = request.getRequestURI();

        //skip filter
        if (path.startsWith("/images/") ||
        	path.startsWith("/login/") ||
            path.startsWith("/product/") ||
            path.startsWith("/category/") ||
            path.startsWith("/ai/") ||
            path.startsWith("/google/") ||
            path.startsWith("/ws/")){
            chain.doFilter(request, response);
            return;
        }
    	
        //validate token
        final String authorization= request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);

            if (jwtUtil.isValid(token)) {

                String username = jwtUtil.extractUsername(token);
                List<String> roles = jwtUtil.extractRoles(token);

                if (!roles.isEmpty()) {
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        chain.doFilter(request, response);
    }
    /*
        in java, var is the type of datatype that will automatically figure out the datatype of the thing that get assigned to it
        Ex:
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
            can also be
            var authorities = List.of(new SimpleGrantedAuthority(role));
    */
}
