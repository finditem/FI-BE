package com.fmi.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DelegatingDataSource;

@TestConfiguration
public class SqlCaptureConfiguration {
    private static final ThreadLocal<List<String>> STATEMENTS = ThreadLocal.withInitial(ArrayList::new);

    public static void reset() {
        STATEMENTS.get().clear();
    }

    public static List<String> statements() {
        return List.copyOf(STATEMENTS.get());
    }

    @Bean
    static BeanPostProcessor recordingDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String name) {
                if (!(bean instanceof DataSource source)) {
                    return bean;
                }
                return new DelegatingDataSource(source) {
                    @Override
                    public Connection getConnection() throws SQLException {
                        return recordingConnection(super.getConnection());
                    }

                    @Override
                    public Connection getConnection(String username, String password) throws SQLException {
                        return recordingConnection(super.getConnection(username, password));
                    }
                };
            }
        };
    }

    private static Connection recordingConnection(Connection connection) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                    Object result = invoke(connection, method, args);
                    if (method.getName().equals("prepareStatement") && result instanceof PreparedStatement statement) {
                        String sql = (String) args[0];
                        return Proxy.newProxyInstance(
                                PreparedStatement.class.getClassLoader(),
                                new Class<?>[] {PreparedStatement.class},
                                (statementProxy, statementMethod, statementArgs) -> {
                                    if (statementMethod.getName().startsWith("execute")) {
                                        STATEMENTS.get().add(sql);
                                    }
                                    return invoke(statement, statementMethod, statementArgs);
                                });
                    }
                    return result;
                });
    }

    private static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}
