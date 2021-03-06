package com.anji.hoau.common.security;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SecurityPermissionScan implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Value("${spring.application.name:}")
    private String serverName;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        ApplicationContext context = contextRefreshedEvent.getApplicationContext();
        if (context == null) {
            return;
        }

        Map<String, String> apiPermissionMap = new HashMap<String, String>();
        Map<String, Object> mvcBeanMap = context.getBeansWithAnnotation(RestController.class);
        Set<Map.Entry<String, Object>> entries = mvcBeanMap.entrySet();
        Iterator<Map.Entry<String, Object>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> beanMap = iterator.next();
            Class<?> clazz = beanMap.getValue().getClass();
            Map<String, String> oneClassMap = getApiPermissionMapFromClass(clazz);
            apiPermissionMap.putAll(oneClassMap);
        }
        for (Map.Entry<String, String> entry : apiPermissionMap.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.isNotEmpty(serverName)) {
                key = "/".concat(serverName).concat(entry.getKey());
            }
            stringRedisTemplate.opsForHash().put(Constant.MVC_PATH_PERMISSION_HASH_TABLE, key, entry.getValue());
        }
    }


    /**
     * ??????MVC???????????????RequestMapping???????????????????????????????????????
     *
     * @param clazz
     * @return Map<path, permission>
     */
    private Map<String, String> getApiPermissionMapFromClass(Class<?> clazz) {
        Map<String, String> result = new HashMap<String, String>();
        if (clazz.getName().contains("CGLIB")) {
            //??????@LOG????????????????????????????????????????????????class
            clazz = clazz.getSuperclass();
        }
        //?????????????????????
        String classMvcPath = getPathFromClass(clazz);
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            //??????mvc???????????????????????????
            String methodPath = "";
            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                methodPath = getPathFromMethod(requestMapping);
            } else {
                PostMapping postMapping = method.getAnnotation(PostMapping.class);
                if (postMapping != null) {
                    methodPath = getPathFromMethod(postMapping);
                } else {
                    GetMapping getMapping = method.getAnnotation(GetMapping.class);
                    methodPath = getPathFromMethod(getMapping);
                }
            }
            if (StringUtils.isBlank(methodPath)) {
                continue;
            }

            //????????????????????????
            String apiPath = classMvcPath + methodPath;

            //???????????????
            HasPermission hasPermission = method.getAnnotation(HasPermission.class);
            if (hasPermission == null) {
                continue;
            }
            String permissions = hasPermission.value();
            if (StringUtils.isNotBlank(permissions)) {
                result.put(apiPath, permissions);
            }
        }
        return result;
    }

    /**
     * ??????MVC?????????RequestMapping?????????????????????
     *
     * @param clazz
     * @return
     */
    private String getPathFromClass(Class<?> clazz) {
        RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
        return getPathFromMethod(requestMapping);
    }

    /**
     * ??????RequestMapping??????????????????????????????
     *
     * @param requestMapping
     * @return
     */
    private String getPathFromMethod(RequestMapping requestMapping) {
        String path = "";

        if (requestMapping == null) {
            return path;
        }

        String[] pathArr = requestMapping.path();
        if (pathArr == null || pathArr.length == 0) {
            pathArr = requestMapping.value();
        }
        if (pathArr != null && pathArr.length > 0) {
            path = pathArr[0];
        }

        return path;
    }

    /**
     * ??????PostMapping??????????????????????????????
     *
     * @param postMapping
     * @return
     */
    private String getPathFromMethod(PostMapping postMapping) {
        String path = "";

        if (postMapping == null) {
            return path;
        }

        String[] pathArr = postMapping.path();
        if (pathArr == null || pathArr.length == 0) {
            pathArr = postMapping.value();
        }
        if (pathArr != null && pathArr.length > 0) {
            path = pathArr[0];
        }

        return path;
    }

    /**
     * ??????GetMapping??????????????????????????????
     *
     * @param getMapping
     * @return
     */
    private String getPathFromMethod(GetMapping getMapping) {
        String path = "";

        if (getMapping == null) {
            return path;
        }

        String[] pathArr = getMapping.path();
        if (pathArr == null || pathArr.length == 0) {
            pathArr = getMapping.value();
        }
        if (pathArr != null && pathArr.length > 0) {
            path = pathArr[0];
        }

        return path;
    }
}
