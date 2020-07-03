package com.chutneytesting.engine.domain.execution.engine.parameterResolver;

import static org.assertj.core.api.Assertions.assertThat;

import com.chutneytesting.task.domain.parameter.Parameter;
import org.apache.groovy.util.Maps;
import org.junit.jupiter.api.Test;

class InputParameterResolverTest {

    @Test
    void should_resolve_simple_object() {
        java.lang.reflect.Parameter simpleObjectStringConstructorParameter = SimpleObject.class.getConstructors()[0].getParameters()[0];
        Parameter parameter = Parameter.fromJavaParameter(simpleObjectStringConstructorParameter);

        InputParameterResolver sut = new InputParameterResolver(Maps.of(
            "string-name", "myStringValue",
            "integer-name", 666
        ));
        Object result = sut.resolve(parameter);

        assertThat(result).isInstanceOf(String.class);
        assertThat(result.toString()).isEqualTo("myStringValue");

        java.lang.reflect.Parameter simpleObjectIntegerConstructorParameter = SimpleObject.class.getConstructors()[0].getParameters()[1];
        parameter = Parameter.fromJavaParameter(simpleObjectIntegerConstructorParameter);

        result = sut.resolve(parameter);

        assertThat(result).isInstanceOf(Integer.class);
        assertThat((Integer) result).isEqualTo(666);
    }

    @Test
    void should_resolve_simple_object_with_map_input() {
        java.lang.reflect.Parameter simpleObjectStringConstructorParameter = SimpleObject.class.getConstructors()[0].getParameters()[0];
        Parameter parameter = Parameter.fromJavaParameter(simpleObjectStringConstructorParameter);

        InputParameterResolver sut = new InputParameterResolver(Maps.of(
            "string-name", Maps.of("json_root", Maps.of("simple key", "value", "complex key", Maps.of("key", "val")))
        ));
        Object result = sut.resolve(parameter);

        assertThat(result).isInstanceOf(String.class);
        assertThat(result.toString()).isEqualTo("{\"json_root\":{\"simple key\":\"value\",\"complex key\":{\"key\":\"val\"}}}");
    }

    @Test
    void should_resolve_complex_object_without_direct_inputs() {
        java.lang.reflect.Parameter complexObjectWhitoutInputConstructorParameter = ComplexObjectConstructor.class.getConstructors()[0].getParameters()[0];
        Parameter parameter = Parameter.fromJavaParameter(complexObjectWhitoutInputConstructorParameter);

        InputParameterResolver sut = new InputParameterResolver(Maps.of(
            "string-name", "myStringValue",
            "integer-name", 666
        ));
        Object result = sut.resolve(parameter);

        assertThat(result).isInstanceOf(SimpleObject.class);
        assertThat((SimpleObject) result).usingRecursiveComparison().isEqualTo(new SimpleObject("myStringValue", 666));
    }

    @Test
    void should_resolve_complex_object_witth_direct_inputs() {
        SimpleObject constructorParameter = new SimpleObject("myStringValue", 666);
        java.lang.reflect.Parameter complexObjectWhitoutInputConstructorParameter = ComplexObjectConstructor.class.getConstructors()[0].getParameters()[0];
        Parameter parameter = Parameter.fromJavaParameter(complexObjectWhitoutInputConstructorParameter);

        InputParameterResolver sut = new InputParameterResolver(Maps.of(
            "simple-object-name", constructorParameter
        ));
        Object result = sut.resolve(parameter);

        assertThat(result).isInstanceOf(SimpleObject.class);
        assertThat((SimpleObject) result).usingRecursiveComparison().isEqualTo(new SimpleObject("myStringValue", 666));
    }
}
