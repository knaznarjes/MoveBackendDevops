package com.move.apigateway.config;

import org.springframework.cloud.gateway.mvc.ProxyExchange;
import org.springframework.cloud.gateway.mvc.config.ProxyExchangeArgumentResolver;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CustomProxyExchangeArgumentResolver extends ProxyExchangeArgumentResolver {

    private final RestTemplate loadBalancedRestTemplate;

    public CustomProxyExchangeArgumentResolver(RestTemplate loadBalancedRestTemplate) {
        super(loadBalancedRestTemplate); // Pass the RestTemplate to the parent constructor
        this.loadBalancedRestTemplate = loadBalancedRestTemplate;
    }
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        ProxyExchange<?> exchange = (ProxyExchange<?>) super.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

        return exchange;
    }
}