package io.docbot.spring.archaius;

import com.netflix.archaius.config.AbstractConfig;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.framework.recipes.cache.ChildData;
import com.netflix.curator.framework.recipes.cache.PathChildrenCache;
import com.netflix.curator.framework.recipes.cache.PathChildrenCacheEvent;
import com.netflix.curator.framework.recipes.cache.PathChildrenCacheListener;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ZooKeepConfig extends AbstractConfig {

    private final Logger LOG = LoggerFactory.getLogger(ZooKeepConfig.class);

    Map<String,Object> current = new ConcurrentHashMap();
    private String configRootPath;
    private final Charset charset = Charset.forName("UTF-8");

    private CuratorFramework client;

    private PathChildrenCache configDir;

    public ZooKeepConfig(String connectString,String path){
        configRootPath = path;
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
        builder.connectString(connectString);
        builder.connectionTimeoutMs(3000);
        builder.sessionTimeoutMs(60000);
        builder.retryPolicy(new ExponentialBackoffRetry(1000, 3));
        client = builder.build();
        client.start();
        configDir = new PathChildrenCache(client, path, true);
        configDir.getListenable().addListener(new PathChildrenCacheListener() {
            public void childEvent(CuratorFramework aClient, PathChildrenCacheEvent event)
                    throws Exception {
                PathChildrenCacheEvent.Type eventType = event.getType();
                ChildData data = event.getData();
                switch (eventType) {
                    case CHILD_ADDED:
                    case CHILD_UPDATED:
                        current.put(removeRootPath(data.getPath()), new String(data.getData(), charset));
                        notifyConfigUpdated(ZooKeepConfig.this);
                        break;
                    case CHILD_REMOVED:
                        current.remove(removeRootPath(data.getPath()));
                        notifyConfigRemoved(ZooKeepConfig.this);
                        break;
                }
            }
        });
        try {
            configDir.start();
        }catch (Exception e){
            LOG.error("ZooConfig Init Error:",e);
        }
    }
    private String removeRootPath(String nodePath) {
        return nodePath.replace(configRootPath + "/", "");
    }
    public Object getRawProperty(String key) {
        return current.get(key);
    }

    public boolean containsKey(String key) {
        return current.containsKey(key);
    }

    public boolean isEmpty() {
        return current.isEmpty();
    }

    public Iterator<String> getKeys() {
         return current.keySet().iterator();
    }

    public void close(){
        if(configDir != null){
            try {
                configDir.close();
            }catch (Exception e){
                LOG.error("Close ConfigDir Error",e);
            }

        }
        if(client != null){
            try {
                client.close();
            }catch (Exception e){
                LOG.error("Close ZooKeeper Client Error",e);
            }
        }

    }
}
