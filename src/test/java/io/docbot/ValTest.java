package io.docbot;

import com.netflix.archaius.Property;
import io.docbot.spring.archaius.DynamicValue;
import org.springframework.stereotype.Service;

@Service
public class ValTest {
    @DynamicValue(value = "foo.prop",defaultValue = "100")
    private Property<Integer> pro;

    @DynamicValue(value = "foo.prop1",defaultValue = "200")
    private Property<String> pro1;

    public void show(){
        System.out.println(pro.get());
        System.out.println(pro1.get());

    }
}
