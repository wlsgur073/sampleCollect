package com.abilsys.oa.util;

import com.abilsys.oa.OaproApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = OaproApplication.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PropertyUtilTest {

    @Test
    public void getProperty() {

        String ret = "";
        ret = PropertyUtil.getProperty("spring.redis.host");

        System.out.println("ret = " + ret);
    }


    static class PropertyUtil {
        public static String getProperty(String propertyName) {
            return getProperty(propertyName, null);
        }

        public static String getProperty(String propertyName, String defaultValue) {
            String value = defaultValue;
            ApplicationContext applicationContext = ApplicationContextProvider.getApplicationContext();

            if (applicationContext != null) {
                String propertyValue = applicationContext.getEnvironment().getProperty(propertyName);

                if (propertyValue != null) {
                    value = propertyValue;
                } else {
                    System.out.println(propertyName + " property was not loaded.");
                }
            } else {
                System.out.println("ApplicationContext is null. Unable to retrieve property : " + propertyName);
            }

            return value;
        }
    }
}
