package work.cxlm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import work.cxlm.cache.AbstractStringCacheStore;
import work.cxlm.cache.InMemoryCacheStore;
import work.cxlm.cache.LevelCacheStore;
import work.cxlm.cache.RedisCacheStore;

/**
 * 程序相关配置
 * created 2020/11/1 17:06
 *
 * @author johnniang
 * @author cxlm
 */
@Configuration
@EnableConfigurationProperties(QfzsProperties.class)
@Slf4j
public class QfzsConfiguration {

    private QfzsProperties qfzsProperties;

    @Autowired
    public void setMyFontProperties(QfzsProperties qfzsProperties) {
        this.qfzsProperties = qfzsProperties;
    }

    @Bean
    public ObjectMapper objectMapperBean(Jackson2ObjectMapperBuilder builder) {
        // 配置此项后，在遇到无法转换的对象时，不会抛出异常
        builder.failOnEmptyBeans(false);
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public AbstractStringCacheStore stringCacheStore() {
        AbstractStringCacheStore stringCacheStore;
        switch (qfzsProperties.getCache()) {
            case "level":
                stringCacheStore = new LevelCacheStore(qfzsProperties);
                break;
            case "redis":
                stringCacheStore = new RedisCacheStore(qfzsProperties);
                break;
            case "memory":
            default:
                stringCacheStore = new InMemoryCacheStore();
                break;
        }
        log.info("正在使用 [{}] 缓存", stringCacheStore.getClass());
        return stringCacheStore;
    }

}
