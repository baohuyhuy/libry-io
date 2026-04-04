package io.libry.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new SnakeCasePageableResolver());
    }

    static class SnakeCasePageableResolver extends PageableHandlerMethodArgumentResolver {

        @Override
        public Pageable resolveArgument(MethodParameter methodParameter,
                                        ModelAndViewContainer mavContainer,
                                        NativeWebRequest webRequest,
                                        WebDataBinderFactory binderFactory) {
            HttpServletRequest raw = (HttpServletRequest) webRequest.getNativeRequest();
            HttpServletRequest wrapped = new HttpServletRequestWrapper(raw) {
                @Override
                public String[] getParameterValues(String name) {
                    if (!"sort".equals(name)) {
                        return super.getParameterValues(name);
                    }
                    String[] values = super.getParameterValues(name);
                    if (values == null) return null;
                    return Arrays.stream(values)
                                 .map(SnakeCasePageableResolver::translateSortParam)
                                 .toArray(String[]::new);
                }
            };
            return super.resolveArgument(methodParameter, mavContainer,
                    new ServletWebRequest(wrapped), binderFactory);
        }

        private static String translateSortParam(String value) {
            // value is like "full_name,asc" or just "full_name"
            String[] parts = value.split(",", 2);
            parts[0] = snakeToCamel(parts[0].trim());
            return parts.length == 2 ? parts[0] + "," + parts[1] : parts[0];
        }

        private static String snakeToCamel(String snake) {
            String[] parts = snake.split("_");
            StringBuilder sb = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    sb.append(Character.toUpperCase(parts[i].charAt(0)));
                    sb.append(parts[i].substring(1));
                }
            }
            return sb.toString();
        }
    }
}
