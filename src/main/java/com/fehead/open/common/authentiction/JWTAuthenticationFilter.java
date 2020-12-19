package com.fehead.open.common.authentiction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fehead.lang.error.AuthenticationException;
import com.fehead.lang.error.EmBusinessError;
import com.fehead.lang.properties.FeheadProperties;
import com.fehead.lang.response.AuthenticationReturnType;
import com.fehead.lang.util.JWTUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: JWT认证过滤器
 * done.
 * @Author lmwis
 * @Date 2019-11-15 20:36
 * @Version 1.0
 */
@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTAuthenticationFilter.class);

    private final ObjectMapper objectMapper ;

    private final PathMatcher pathMatcher;

    /**
     * 解析签名
     */
    public final String SINGE_KEY;

    /**
     * token请求头
     */
    public static final String authorizationHeader = "Authorization";

    /**
     * uri require认证列表
     * 此处之后需要移入配置文件
     */
    private static final Map<String, List<HttpMethod>> authenticatedUriList = new HashMap<String, List<HttpMethod>>() {{
        put("/**", Arrays.asList(HttpMethod.PUT,HttpMethod.POST));
//        put("/user", Arrays.asList(HttpMethod.GET, HttpMethod.DELETE, HttpMethod.PUT));
    }};
    public JWTAuthenticationFilter(FeheadProperties feheadProperties, ObjectMapper objectMapper,PathMatcher pathMatcher) {
        this.objectMapper = objectMapper;
        this.pathMatcher = pathMatcher;
        SINGE_KEY = feheadProperties.getSecurityProperties().getJwtInnerSecretKey();
    }

    /**
     * 过滤行为
     * @param request request
     * @param response response
     * @param filterChain filterChain
     * @throws ServletException ServletException
     * @throws IOException IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        logger.info(requestURI);
        if (isUriRequiredAuthenticated(request, request.getRequestURI())) { // 如果需要认证
            String token = request.getHeader(authorizationHeader); // 获取认证信息
            if (StringUtils.isEmpty(token) || !token.startsWith("Bearer ")) { // 无请求头或者不以"Bearer "开头认为无效
                // 认证不通过处理
                onAuthenticationFailure(request,response
                        ,new AuthenticationException(EmBusinessError.SERVICE_AUTHENTICATION_ILLEGAL));
                return;
            }
            // 解析token
            String authentication = parseToken(token);
            if(authentication==null){ // 无效token
                onAuthenticationFailure(request,response
                        ,new AuthenticationException(EmBusinessError.SERVICE_AUTHENTICATION_ILLEGAL));
                return;
            }
            // 校验成功放入容器
//            feheadSecurityContext.setAuthentication(authentication);
        }
        // 不需要校验
        filterChain.doFilter(request, response);

    }

    /**
     * 判断uri是否需要认证
     *
     * @param request request
     * @param uri uri
     * @return boolean
     */
    private boolean isUriRequiredAuthenticated(HttpServletRequest request, String uri) {
        for (String pattern : authenticatedUriList.keySet()) {
            if (pathMatcher.match(pattern, uri)) { // 先判断uri是否存在，再核对http method
                for (HttpMethod httpMethod : authenticatedUriList.get(pattern)) {
                    if (HttpMethod.resolve(request.getMethod()) == httpMethod) { // 匹配则需要认证
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     * 从token中解析服务调用信息
     *
     * @param token token
     * @return 服务名
     */
    private String parseToken(String token) {
        if (token != null) {
            try {
                // parse the token.
                String serviceName = JWTUtil.parasToken4Subject(token,SINGE_KEY);
                if (serviceName != null) {
                    return serviceName;
                }
            } catch (Exception e){ // jwt无效
                return null;
            }
            return null;
        }
        return null;
    }

    /**
     * 认证失败的处理
     * @param request request
     * @param response response
     * @param exception exception
     */
    private void onAuthenticationFailure(HttpServletRequest request
            ,HttpServletResponse response
            ,AuthenticationException exception) throws IOException {

        logger.info("校验失败");

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper
                .writeValueAsString( AuthenticationReturnType
                        .create(exception.getErrorMsg()
                                ,HttpStatus.UNAUTHORIZED.value())));
    }
}
