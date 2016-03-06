package io.docbot;

import com.netflix.archaius.DefaultConfigLoader;
import com.netflix.archaius.DefaultDecoder;
import com.netflix.archaius.DefaultPropertyFactory;
import com.netflix.archaius.Property;
import com.netflix.archaius.cascade.ConcatCascadeStrategy;
import com.netflix.archaius.config.CompositeConfig;
import com.netflix.archaius.config.MapConfig;
import com.netflix.archaius.exceptions.ConfigException;
import com.netflix.archaius.typesafe.TypesafeConfigReader;
import io.docbot.spring.archaius.DynamicValue;
import io.docbot.spring.archaius.ZooKeepConfig;
import org.springframework.util.ReflectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ArchaiusTest {
    public static void main(String[] args) throws ConfigException, IOException, NoSuchFieldException, IllegalAccessException {
        CompositeConfig config = new CompositeConfig();
        ZooKeepConfig scmConfig = new ZooKeepConfig("127.0.0.1:2181", "/zook/config");
        config.addConfig("scm", scmConfig);
        MapConfig mapConfig = MapConfig.builder()
                .put("env", "prod")
                .put("region", "us-east")
                .build();
        config.addConfig("prop", mapConfig);
        DefaultConfigLoader loader = DefaultConfigLoader.builder()
                .withConfigReader(new TypesafeConfigReader())
                .withStrLookup(config)
                .build();
        CompositeConfig typeSafeConfig = loader.newLoader()
                .withCascadeStrategy(ConcatCascadeStrategy.from("${env}", "${region}"))
                .load("foo");
        config.replaceConfig("foo", typeSafeConfig);
        DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);


        ValTest valTest = new ValTest();
        Field f = valTest.getClass().getDeclaredField("pro");
        Field[] fields = valTest.getClass().getDeclaredFields();
        for(Field f1 : fields){
            if(f1.isAnnotationPresent(DynamicValue.class)){
                Type t = f1.getGenericType();
                System.out.println(t);
            }
        }
        DynamicValue dv = f.getAnnotation(DynamicValue.class);
        String proName = dv.value();
        String defaultName = dv.defaultValue();
        Type t = f.getGenericType();
        if(f.getType().isAssignableFrom(Property.class) && t instanceof ParameterizedType){
            ParameterizedType p = (ParameterizedType) t;
            Type[] acTypes = p.getActualTypeArguments();
            Type pt = acTypes[0];
            if(pt instanceof Class){
                Object a = DefaultDecoder.INSTANCE.decode((Class)pt,defaultName);
                Property dpt = factory.getProperty(proName).asType((Class)pt, a);
                ReflectionUtils.makeAccessible(f);
                f.set(valTest, dpt);
            }
        }
        BufferedReader b = new BufferedReader(new InputStreamReader(System.in));
        String path = b.readLine();
        while (path != null || !path.isEmpty()) {
            valTest.show();
            path = b.readLine();
        }
    }
}
