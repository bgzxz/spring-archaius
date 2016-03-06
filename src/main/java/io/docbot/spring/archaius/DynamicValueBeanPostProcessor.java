package io.docbot.spring.archaius;

import com.netflix.archaius.DefaultDecoder;
import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.Property;
import com.netflix.archaius.PropertyFactory;
import com.netflix.archaius.config.CompositeConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.config.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.io.Resource;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Properties;

public class DynamicValueBeanPostProcessor implements BeanPostProcessor, DisposableBean, InitializingBean {

    private final Logger LOG = LoggerFactory.getLogger(DynamicValueBeanPostProcessor.class);

    public static final String XML_FILE_EXTENSION = ".xml";

    private boolean enableSystemProperty;
    private String zkServer;
    private String appCode;
    private String secretKey;
    private String configDir;

    private Resource[] locations;

    private String fileEncoding;

    private PropertyFactory propertyFactory;

    private ZooKeepConfig zkConfig;

    private PropertiesPersister propertiesPersister = new DefaultPropertiesPersister();

    public Object postProcessBeforeInitialization(final Object bean, String beanName) throws BeansException {
        try {
           final Class beanClass = AopUtils.getTargetClass(bean);
            ReflectionUtils.doWithFields(beanClass, new ReflectionUtils.FieldCallback() {
                public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                    LOG.debug("{}{}",beanClass.getCanonicalName(),field);
                        DynamicValue dv = field.getAnnotation(DynamicValue.class);
                        String proName = dv.value();
                        String defaultValue = dv.defaultValue();
                        Type t = field.getGenericType();
                        if (t instanceof ParameterizedType) {
                            ParameterizedType p = (ParameterizedType) t;
                            Type[] acTypes = p.getActualTypeArguments();
                            Type pt = acTypes[0];
                            if (pt instanceof Class) {
                                Object a = DefaultDecoder.INSTANCE.decode((Class) pt, defaultValue);
                                Property dpt = propertyFactory.getProperty(proName).asType((Class) pt, a);
                                ReflectionUtils.makeAccessible(field);
                                field.set(bean, dpt);
                            }
                        } else {
                            LOG.error(beanClass.getCanonicalName() + "'s Field " + field.getName() + " must Property<T>");
                        }
                }
            }, new ReflectionUtils.FieldFilter() {
                public boolean matches(Field field) {
                    return field.isAnnotationPresent(DynamicValue.class) && field.getType().isAssignableFrom(Property.class);
                }
            });
        }catch (Exception e){
            LOG.error("DynamicValue Inject Error:",e);
        }
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    public String getZkServer() {
        return zkServer;
    }

    public void setZkServer(String zkServer) {
        this.zkServer = zkServer;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void destroy() throws Exception {
        if (zkConfig != null) {
            zkConfig.close();
        }
    }

    public String getConfigDir() {
        return configDir;
    }

    public void setConfigDir(String configDir) {
        this.configDir = configDir;
    }

    public void afterPropertiesSet() throws Exception {
        CompositeConfig config = new CompositeConfig();
        if (zkConfig == null) {
            zkConfig = new ZooKeepConfig(zkServer,configDir);
        }
        config.addConfig("zkConfig", zkConfig);
        if (enableSystemProperty) {
            config.addConfig("systemProperty", SystemConfig.INSTANCE);
        }
        Properties props = new Properties();
        loadProperties(props);
        config.addConfig("file", MapConfig.from(props));
        propertyFactory = DefaultPropertyFactory.from(config);
    }

    protected void loadProperties(Properties props) throws IOException {
        if (this.locations != null) {
            for (Resource location : this.locations) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Loading properties file from " + location);
                }
                InputStream is = null;
                try {
                    is = location.getInputStream();
                    String filename = location.getFilename();
                    if (filename != null && filename.endsWith(XML_FILE_EXTENSION)) {
                        this.propertiesPersister.loadFromXml(props, is);
                    } else {
                        if (this.fileEncoding != null) {
                            this.propertiesPersister.load(props, new InputStreamReader(is, this.fileEncoding));
                        } else {
                            this.propertiesPersister.load(props, is);
                        }
                    }
                } catch (IOException ex) {
                    throw ex;
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        }
    }

    public boolean isEnableSystemProperty() {
        return enableSystemProperty;
    }

    public void setEnableSystemProperty(boolean enableSystemProperty) {
        this.enableSystemProperty = enableSystemProperty;
    }

    public Resource[] getLocations() {
        return locations;
    }

    public void setLocations(Resource[] locations) {
        this.locations = locations;
    }

    public String getFileEncoding() {
        return fileEncoding;
    }

    public void setFileEncoding(String fileEncoding) {
        this.fileEncoding = fileEncoding;
    }
}
